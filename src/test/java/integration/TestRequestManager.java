package integration;

import com.google.common.collect.BiMap;
import dataMgmt.DataManager;
import dataMgmt.SessionStorage;
import dataMgmt.models.FileMetadata;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import requestMgmt.RequestManager;
import websocket.ConnectException;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.UserRegisterRequest;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by fahslaj on 10/31/2016.
 */
public class TestRequestManager extends UserBasedIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger("requestManagerIntegrationTest");

    private static final String user1ID = "_testUser1";
    private static final String user1Pass = "_testPass1";
    private static final String user1FirstName = "_testFirstName1";
    private static final String user1LastName = "_testLastName1";
    private static final String user1Email = "_testEmail1@testDomain.com";

    private static final String user2ID = "_testUser2";
    private static final String user2Pass = "_testPass2";
    private static final String user2FirstName = "_testFirstName2";
    private static final String user2LastName = "_testLastName2";
    private static final String user2Email = "_testEmail2@testDomain.com";

    private static Project proj1 = new Project(-1, "_testProject1", new HashMap<>());
    private static Project proj2 = new Project(-1, "_testProject2", new HashMap<>());

    private static BiMap<String, Byte> apiConstants;

    private static final IRequestSendErrorHandler errHandler = () -> Assert.fail("Failed to send message");

    private static WSManager ws1 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static WSManager ws2 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static DataManager dataMgr;
    private static SessionStorage sesSto;
    private static RequestManager reqMgr;

    private static Request req;

    @Before
    public void setup() {
        // Set some invalid auth info; avoid null checks
        ws1.setAuthInfo("","");
        dataMgr = new DataManager();
        sesSto = dataMgr.getSessionStorage();
        reqMgr = new RequestManager(dataMgr, ws1,
                () -> Assert.fail("Error sending request"), (e, m) -> Assert.fail("Wrong status code")) {
            @Override
            public void finishSubscribeToProject(long id, File[] files) {

            }

            @Override
            public void finishCreateProject(Project project) {

            }

            @Override
            public void finishDeleteProject(Project project) {

            }

			@Override
			public void finishRenameFile(FileMetadata fMeta) {
				
			}

			@Override
			public void finishMoveFile(FileMetadata fMeta) {
				
			}
        };
    }

    @After
    public void cleanup() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);
        cleanupUser(logger, user1ID, user1Pass, waiter, errHandler);
        cleanupUser(logger, user2ID, user2Pass, waiter, errHandler);
        waiter.tryAcquire(2, 10, TimeUnit.SECONDS);
    }

    @Test
    public void testRequestManager() throws java.net.ConnectException, InterruptedException {
        // register, login, get permission constants
        registerUser(user1ID, user1FirstName, user1LastName, user1Email, user1Pass);
        testLogin(user1ID, user1Pass);
        testFetchPermissionConstants();
        // create a project, register user 2, add them to the project, and remove them from the project
        proj1 = testCreateProject(proj1.getName());
        registerUser(user2ID, user2FirstName, user2LastName, user2Email, user2Pass);
        testAddUserToProject(proj1.getProjectID(), proj1.getName(), user2ID, "admin", apiConstants);
        testRemoveUserFromProject(proj1.getProjectID(), proj1.getName(), user2ID);
        // create project 2, fetch and subscribe to both projects, unsubscribe from project 1
        proj2 = testCreateProject(proj2.getName());
        List<Long> projectIDs = Arrays.asList(proj1.getProjectID(), proj2.getProjectID());
        testFetchAndSubscribeAll(projectIDs);
        testUnsubscribeFromProject(proj1.getProjectID());
        // fetch projects, subscribe to project 1, add user2 to project2, delete project 1, logout user 1
        testFetchProjects();
        testSubscribeToProject(proj1.getProjectID());
        testAddUserToProject(proj2.getProjectID(), proj2.getName(), user2ID, "write", apiConstants);
        testDeleteProject(proj1.getProjectID());
        testLogout();
        // login as user 2, remove self from project 2, logout as user 2
        testLogin(user2ID, user2Pass);
        testRemoveSelfFromProject(proj2.getProjectID());
        testLogout();
        // login as user 1, delete project 2
        testLogin(user1ID, user1Pass);
        deleteProject(proj2.getProjectID());
    }

    private void testFetchPermissionConstants() throws InterruptedException {
        logger.info("Fetching permission constants");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PERMISSION_CONSTANTS)) {
                apiConstants = (BiMap<String, Byte>) event.getNewValue();
                waiter.release();
            }
        };
        dataMgr.getSessionStorage().addPropertyChangeListener(listener);
        reqMgr.fetchPermissionConstants();
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        dataMgr.getSessionStorage().removePropertyChangeListener(listener);
    }

    private void registerUser(String userID, String userFirstName,
                              String userLastName, String userEmail, String userPass) throws InterruptedException, java.net.ConnectException {
        logger.info(String.format("Registering user: %s", userID));
        Semaphore waiter = new Semaphore(0);

        req = new UserRegisterRequest(userID, userFirstName, userLastName, userEmail, userPass).getRequest(response -> {
            // If registration fails, probably is already there.
            waiter.release();
        }, errHandler);
        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testLogin(String userID, String userPass) throws InterruptedException {
        logger.info("Logging in");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.USERNAME)) {
                if (userID.equals(event.getNewValue())) {
                    waiter.release();
                }
            } else if (event.getPropertyName().equals(SessionStorage.AUTH_TOKEN)) {
                if (event.getNewValue() != null) {
                    waiter.release();
                }
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.login(userID, userPass);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }

    private Project tempCreateProj;
    private Project testCreateProject(String name) throws InterruptedException {
        logger.info("Creating project");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
                Project project = (Project) event.getNewValue();
                if (project.getName().equals(name)) {
                    tempCreateProj = project;
                    waiter.release();
                }
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.createProject(name);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
        return tempCreateProj;
    }

    private void testAddUserToProject(long projId, String projName, String userId, String level, BiMap<String, Byte> apiConstants) throws InterruptedException {
        logger.info("Adding user to project");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
                Project project = (Project) event.getNewValue();
                if (project.getName().equals(projName) &&
                        project.getPermissions().containsKey(userId) &&
                        project.getPermissions().get(userId).getPermissionLevel() == apiConstants.get(level)) {
                    waiter.release();
                }
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.addUserToProject(projId, userId, apiConstants.get(level));
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }

    private void testRemoveUserFromProject(long projId, String projName, String userId) throws InterruptedException {
        logger.info("Remove user from project");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
                Project project = (Project) event.getNewValue();
                if (project.getName().equals(projName) &&
                    !project.getPermissions().containsKey(userId)) {
                    waiter.release();
                }
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.removeUserFromProject(projId, userId);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }

    private void testFetchAndSubscribeAll(List<Long> projectIds) throws InterruptedException {
        logger.info("Fetch and subscribe all");
        Semaphore waiter = new Semaphore(0);
        List<Long> subscribedIds = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
                List<Project> projects = (List<Project>) event.getNewValue();
                List<Long> responseIds = new ArrayList<>();
                for (Project p : projects) {
                    responseIds.add(p.getProjectID());
                }
                if (responseIds.containsAll(projectIds)) {
                    waiter.release();
                    counter.getAndAdd(1);
                }
            } else if (event.getPropertyName().equals(SessionStorage.SUBSCRIBED_PROJECTS)) {
                subscribedIds.add((Long) event.getNewValue());
                waiter.release();
                counter.getAndAdd(1);
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.fetchAndSubscribeAll(projectIds);
        if (!waiter.tryAcquire(3, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
        Assert.assertTrue(subscribedIds.containsAll(projectIds));
    }

    private void testUnsubscribeFromProject(long projId) throws InterruptedException {
        logger.info("Unsubscribe from project");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.SUBSCRIBED_PROJECTS)) {
                if (event.getOldValue().equals(projId)) {
                    waiter.release();
                }
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.unsubscribeFromProject(projId);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }

    private void testFetchProjects() throws InterruptedException {
        logger.info("Fetching projects");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
                List<Project> projects = (List<Project>) event.getNewValue();
                List<Long> ids = projects.stream().map(Project::getProjectID).collect(Collectors.toList());
                List<Long> correctProjects = new ArrayList<>();
                correctProjects.add(proj1.getProjectID());
                correctProjects.add(proj2.getProjectID());
                if (ids.containsAll(correctProjects)) {
                    waiter.release();
                }
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.fetchProjects();
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }

    private void testSubscribeToProject(long projId) throws InterruptedException {
        logger.info("Subscribing to project");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.SUBSCRIBED_PROJECTS)) {
                long id = (long) event.getNewValue();
                if (id == projId) {
                    waiter.release();
                }
            }
        };
        sesSto.addPropertyChangeListener(listener);
        reqMgr.subscribeToProject(projId);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }

    private PropertyChangeListener getProjectRemovedChangeListener(long projId, Semaphore waiter) {
        return (event) -> {
            if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
                Project p = (Project) event.getOldValue();
                if (projId == p.getProjectID()) {
                    waiter.release();
                }
            } else if (event.getPropertyName().equals(SessionStorage.SUBSCRIBED_PROJECTS)) {
                long id = (long) event.getOldValue();
                if (projId == id) {
                    waiter.release();
                }
            }
        };
    }

    private void testDeleteProject(long projId) throws InterruptedException {
        logger.info("Deleting project");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = getProjectRemovedChangeListener(projId, waiter);
        sesSto.addPropertyChangeListener(listener);

        reqMgr.deleteProject(projId);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }

    private void deleteProject(long projId) {
        reqMgr.deleteProject(projId);
    }

    private void testLogout() throws InterruptedException {
        logger.info("Logging out");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = (event) -> {
            if (event.getPropertyName().equals(SessionStorage.USERNAME) ||
                    event.getPropertyName().equals(SessionStorage.AUTH_TOKEN)) {
                if (event.getNewValue() == null) {
                    waiter.release();
                }
            }
        };
        dataMgr.getSessionStorage().addPropertyChangeListener(listener);
        reqMgr.logout();
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        dataMgr.getSessionStorage().removePropertyChangeListener(listener);
    }

    private void testRemoveSelfFromProject(long projId) throws InterruptedException {
        logger.info("Removing self from project");
        Semaphore waiter = new Semaphore(0);
        PropertyChangeListener listener = getProjectRemovedChangeListener(projId, waiter);

        sesSto.addPropertyChangeListener(listener);
        reqMgr.removeSelfFromProject(projId);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        sesSto.removePropertyChangeListener(listener);
    }
}

