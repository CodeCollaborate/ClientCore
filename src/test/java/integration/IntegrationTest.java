package integration;

import com.google.common.collect.BiMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Request;
import websocket.models.notifications.*;
import websocket.models.requests.*;
import websocket.models.responses.*;

import java.net.ConnectException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger("integrationTest");
    private static final IRequestSendErrorHandler errHandler = new IRequestSendErrorHandler() {
        @Override
        public void handleRequestSendError() {
            System.out.println("Failed to send");
        }
    };
    private static final String userID = "_testUser";
    private static final String userPass = "_testPass";
    private static final String userFirstName = "_testFirstName";
    private static final String userLastName = "_testLastName";
    private static final String userEmail = "_testEmail@testDomain.com";
    private static final String fileData = "_test data1\ntest data 2";
    private static WSManager wsMgr = new WSManager(new ConnectionConfig("ws://localhost:8000/ws/", false, 5));
    private static String projectName = "_testProject";
    private static String filePath = "_test/file/path";
    private static String fileName = "_testFile";
    private static int fileVersion = 1;
    private static long projectID = -1;
    private static long fileID = -1;
    private static String senderID = "";
    private static String senderToken = "";
    private static BiMap<String, Byte> apiConstants;
    private Request req;

    @After
    public void cleanup() throws ConnectException, InterruptedException {
        // Create a new connection - The old one may have died if there were errors.
        wsMgr = new WSManager(new ConnectionConfig("ws://localhost:8000/ws/", false, 5));

        // TODO(wongb): Redo authentication once server supports it.
        wsMgr.setAuthInfo(senderID, senderToken);

        // TODO(wongb): Do user cleanup once server supports it.
        if (projectID != -1) {
            logger.info("Cleaning project up");
            // Deleting the project will delete all its files as well
            wsMgr.sendRequest(new ProjectDeleteRequest(projectID).getRequest(null, null));
        }
    }

    @Test
    public void integrationTest() throws Exception {
        // Test valid flow
        testUserRegister();
        testUserLogin();
        testProjectGetPermissionConstants();
        testUserLookup();
        testUserProjects();
        testProjectCreate();
        testUserProjects();
        testProjectLookup();
        testProjectGetFiles(); // should have 0 files
        testProjectSubscribe();
        testProjectRename();
        testFileCreate();
        testProjectGetFiles(); // should have 1 file
        testFilePull(); // Should have no changes
        testFileChange();
        testFilePull(); // Should have 1 change
        testFileMove();
        testFileChange();
        testFileRename();
        testFileChange();
        testFilePull(); // Should have 3 changes
        testProjectGetFiles(); // should have 1 file
        testFileDelete();
        testProjectGetFiles(); // should have 0 files
        testProjectUnsubscribe();
        testProjectSubscribe();
        testProjectLookup();
        testUserProjects();
        testProjectDelete();
        testUserProjects();
        // ProjectGetOnlineClientsRequest (NOT IMPLEMENTED)
        // ProjectGrantPermissionsRequest (NOT IMPLEMENTED)
        // ProjectRevokePermissionsRequest (NOT IMPLEMENTED)

        // TODO(shapiro): Test Project.GrantPermissions and Project.RevokePermissions


        // Run invalid method type, expect error
        // Re-run login, expect error(?)
        // Run invalid login, expect error

        Thread.sleep(1000);
    }

    private void testUserRegister() throws InterruptedException, ConnectException {
        logger.info(String.format("Registering user"));
        Semaphore waiter = new Semaphore(0);

        req = new UserRegisterRequest(userID, userFirstName, userLastName, userEmail, userPass).getRequest( response -> {
            // If registration fails, probably is already there.

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserLogin() throws InterruptedException, ConnectException {
        logger.info(String.format("Logging in"));
        Semaphore waiter = new Semaphore(0);

        req = new UserLoginRequest(userID, userPass).getRequest( response -> {
            // TODO(wongb) Add login logic for server
            if (response.getStatus() != 200) {
                try {
                    testUserRegister();
                    testUserLogin();
                } catch (InterruptedException | ConnectException e) {
                    Assert.fail("Failed to register and login");
                }
                waiter.release();
                return;
            }
            Assert.assertEquals("Failed to log in", 200, response.getStatus());

            wsMgr.setAuthInfo(userID, ((UserLoginResponse) response.getData()).getToken());
            senderID = userID;
            senderToken = ((UserLoginResponse) response.getData()).getToken();

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectGetPermissionConstants() throws InterruptedException {
        logger.info("Requesting api permission constants");
        Semaphore waiter = new Semaphore(0);

        req = new ProjectGetPermissionConstantsRequest().getRequest( response -> {
            Assert.assertEquals("Failed to get permission constants", 200, response.getStatus());
            apiConstants = ((ProjectGetPermissionConstantsResponse) response.getData()).getConstants();
            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        Assert.assertNotEquals("Constants map empty", 0, apiConstants.size());

        // NOTE: I chose a permission which (in my opinion) will probably always be around
        //       if the "read" permission is ever removed from the server, this will fail - Joel
        Assert.assertTrue("Constants map does not contain correct provlages", apiConstants.containsKey("read"));
    }

    private void testProjectCreate() throws ConnectException, InterruptedException {
        logger.info(String.format("Creating project with name %s", projectName));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectCreateRequest(projectName).getRequest( response -> {
            Assert.assertEquals("Failed to create project", 200, response.getStatus());

            projectID = ((ProjectCreateResponse) response.getData()).getProjectID();

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectSubscribe() throws InterruptedException, ConnectException {
        logger.info(String.format("Subscribing to project with id %d", projectID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectSubscribeRequest(projectID).getRequest( response -> {
            Assert.assertEquals("Failed to subscribe to project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectRename() throws ConnectException, InterruptedException {
        projectName = projectName + "_Renamed";

        logger.info(String.format("Renaming project with id %d to %s", projectID, projectName));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectRenameRequest(projectID, projectName).getRequest( response -> {
            Assert.assertEquals("Failed to rename project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("Project", "Rename", notification -> { // Create notification handler
            Assert.assertEquals("ProjectRenameNotification gave wrong project ID", projectID, notification.getResourceID());
            Assert.assertEquals("ProjectRenameNotification gave wrong value", projectName, ((ProjectRenameNotification) notification.getData()).newName);

            wsMgr.deregisterNotificationHandler("Project", "Rename");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileCreate() throws ConnectException, InterruptedException {
        logger.info(String.format("Creating file %s in project with id %d", filePath + "/" + fileName, projectID));
        Semaphore waiter = new Semaphore(0);

        req = new FileCreateRequest(fileName, filePath, projectID, fileData.getBytes()).getRequest( response -> {
            Assert.assertEquals("Failed to create file", 200, response.getStatus());

            fileID = ((FileCreateResponse) response.getData()).getFileID();

            waiter.release(2);
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Create", notification -> { // Create notification handler
            try {
                if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
                    Assert.fail("Acquire timed out");
                }
            } catch (InterruptedException e) {
                // do nothing
            }

            Assert.assertEquals("FileCreateNotification gave wrong project ID", projectID, notification.getResourceID());
            Assert.assertEquals("FileCreateNotification gave wrong file name", fileName, ((FileCreateNotification) notification.getData()).file.getFilename());
            Assert.assertEquals("FileCreateNotification gave wrong file path", filePath, ((FileCreateNotification) notification.getData()).file.getRelativePath());
            Assert.assertEquals("FileCreateNotification gave wrong file ID", fileID, ((FileCreateNotification) notification.getData()).file.getFileID());
            Assert.assertEquals("FileCreateNotification gave wrong file version", fileVersion, ((FileCreateNotification) notification.getData()).file.getFileVersion());

            wsMgr.deregisterNotificationHandler("File", "Create");
            waiter.release(2);
        });

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(3, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileMove() throws InterruptedException, ConnectException {
        filePath = filePath + "/new";

        logger.info(String.format("Moving file %d to %s", fileID, filePath));
        Semaphore waiter = new Semaphore(0);

        req = new FileMoveRequest(fileID, filePath).getRequest( response -> {
            Assert.assertEquals("Failed to move file", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Move", notification -> { // Create notification handler
            Assert.assertEquals("FileMoveNotification gave wrong file ID", fileID, notification.getResourceID());
            Assert.assertEquals("FileMoveNotification gave wrong changes", filePath, ((FileMoveNotification) notification.getData()).newPath);

            wsMgr.deregisterNotificationHandler("File", "Move");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileRename() throws InterruptedException, ConnectException {
        fileName = fileName + "_new";

        logger.info(String.format("Renaming file %d to %s", fileID, fileName));
        Semaphore waiter = new Semaphore(0);

        req = new FileRenameRequest(fileID, fileName).getRequest( response -> {
            Assert.assertEquals("Failed to rename file", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Rename", notification -> { // Create notification handler
            Assert.assertEquals("FileRenameNotification gave wrong file ID", fileID, notification.getResourceID());
            Assert.assertEquals("FileRenameNotification gave wrong new name", fileName, ((FileRenameNotification) notification.getData()).newName);

            wsMgr.deregisterNotificationHandler("File", "Rename");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileChange() throws InterruptedException, ConnectException {
        logger.info(String.format("Changing file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[]{"+5:6:newData" + fileVersion};
        req = new FileChangeRequest(fileID, changes, fileVersion).getRequest( response -> {
            Assert.assertEquals("Failed to change file", 200, response.getStatus());
            Assert.assertEquals("FileChangeResponse gave wrong file version", fileVersion + 1, ((FileChangeResponse) response.getData()).getFileVersion());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Change", notification -> { // Create notification handler
            Assert.assertEquals("FileChangeNotification gave wrong file ID", fileID, notification.getResourceID());
            Assert.assertArrayEquals("FileChangeNotification gave wrong changes", changes, ((FileChangeNotification) notification.getData()).changes);
            Assert.assertEquals("FileChangeNotification gave wrong file version", fileVersion + 1, ((FileChangeNotification) notification.getData()).fileVersion);
            Assert.assertEquals("FileChangeNotification gave wrong base file version", fileVersion, ((FileChangeNotification) notification.getData()).baseFileVersion);

            wsMgr.deregisterNotificationHandler("File", "Change");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        fileVersion++;
    }

    private void testFilePull() throws InterruptedException, ConnectException {
        logger.info(String.format("Pulling file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[fileVersion - 1];
        for (int i = 0; i < changes.length; i++) {
            changes[i] = "+5:6:newData" + (i + 1);
        }
        req = new FilePullRequest(fileID).getRequest( response -> {
            Assert.assertEquals("Failed to pull file", 200, response.getStatus());
            Assert.assertArrayEquals("FilePullResponse gave wrong base file text", fileData.getBytes(), ((FilePullResponse) response.getData()).getFileBytes());
            Assert.assertArrayEquals("FilePullResponse gave wrong changes", changes, ((FilePullResponse) response.getData()).getChanges());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileDelete() throws InterruptedException, ConnectException {
        logger.info(String.format("Deleting file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        req = new FileDeleteRequest(fileID).getRequest( response -> {
            Assert.assertEquals("Failed to delete file", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Delete", notification -> { // Create notification handler
            Assert.assertEquals("FileDeleteNotification gave wrong file ID", fileID, notification.getResourceID());

            fileID = -1;

            wsMgr.deregisterNotificationHandler("File", "Delete");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectUnsubscribe() throws InterruptedException, ConnectException {
        logger.info(String.format("Unsubscribing from project with id %d", projectID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectUnsubscribeRequest(projectID).getRequest( response -> {
            Assert.assertEquals("Failed to unsubscribe from project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);

        // Wait till subscription is done before doing anything else.
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectDelete() throws ConnectException, InterruptedException {
        logger.info(String.format("Deleting project with ID %d", projectID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectDeleteRequest(projectID).getRequest( response -> {
            Assert.assertEquals("Failed to delete project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("Project", "Delete", notification -> { // Create notification handler
            Assert.assertEquals("ProjectDeleteNotification gave wrong project ID", projectID, notification.getResourceID());

            projectID = -1;

            wsMgr.deregisterNotificationHandler("Project", "Delete");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectGetFiles() throws ConnectException, InterruptedException {
        logger.info(String.format("Getting files for project with ID %d", projectID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectGetFilesRequest(projectID).getRequest( response -> {
            Assert.assertEquals("Failed to get files for project", 200, response.getStatus());

            if (fileID != -1) {
                Assert.assertEquals("Incorrect number of files returned", 1, ((ProjectGetFilesResponse) response.getData()).files.length);
                Assert.assertEquals("Incorrect file ID returned for file at index 0", fileID, ((ProjectGetFilesResponse) response.getData()).files[0].getFileID());
                Assert.assertEquals("Incorrect file name returned for file at index 0", fileName, ((ProjectGetFilesResponse) response.getData()).files[0].getFilename());
                // Disabled until mysql os-filepath fix implemented in server
//                Assert.assertEquals("Incorrect file path returned for file at index 0", filePath, ((ProjectGetFilesResponse) response.getData()).files[0].getPermissions());
                Assert.assertEquals("Incorrect file version returned for file at index 0", fileVersion, ((ProjectGetFilesResponse) response.getData()).files[0].getFileVersion());
            } else {
                Assert.assertEquals("Incorrect number of files returned", 0, ((ProjectGetFilesResponse) response.getData()).files.length);
            }

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectLookup() throws ConnectException, InterruptedException {
        logger.info(String.format("Looking up project with ID %d", projectID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectLookupRequest(new Long[]{projectID}).getRequest( response -> {
            Assert.assertEquals("Failed to lookup project", 200, response.getStatus());

            Assert.assertEquals("Incorrect number of projects returned", 1, ((ProjectLookupResponse) response.getData()).getProjects().length);
            Assert.assertEquals("Incorrect ProjectID returned", projectID, ((ProjectLookupResponse) response.getData()).getProjects()[0].getProjectID());
            Assert.assertEquals("Incorrect project name returned", projectName, ((ProjectLookupResponse) response.getData()).getProjects()[0].getName());
            Assert.assertEquals("Incorrect project permissions count returned", 1, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().size());
            Assert.assertEquals("Incorrect project permissions name for owner returned", userID, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(userID).getUsername());
            Assert.assertEquals("Incorrect project permissions level for owner returned", 10, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(userID).getPermissionLevel());
            Assert.assertEquals("Incorrect project permissions granted by field for owner returned", userID, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(userID).getGrantedBy());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserLookup() throws ConnectException, InterruptedException {
        logger.info(String.format("Looking up user with ID %s", userID));
        Semaphore waiter = new Semaphore(0);

        req = new UserLookupRequest(new String[]{userID}).getRequest( response -> {
            Assert.assertEquals("Failed to lookup user", 200, response.getStatus());

            Assert.assertEquals("Incorrect number of users returned", 1, ((UserLookupResponse) response.getData()).getUsers().length);
            Assert.assertEquals("Incorrect first name returned", userFirstName, ((UserLookupResponse) response.getData()).getUsers()[0].getFirstName());
            Assert.assertEquals("Incorrect last name returned", userLastName, ((UserLookupResponse) response.getData()).getUsers()[0].getLastName());
            Assert.assertEquals("Incorrect email returned", userEmail, ((UserLookupResponse) response.getData()).getUsers()[0].getEmail());
            Assert.assertEquals("Incorrect username returned", userID, ((UserLookupResponse) response.getData()).getUsers()[0].getUsername());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserProjects() throws ConnectException, InterruptedException {
        logger.info(String.format("Looking up projects for user with ID %s", userID));
        Semaphore waiter = new Semaphore(0);

        req = new UserProjectsRequest().getRequest( response -> {
            Assert.assertEquals("Failed to lookup projects for user", 200, response.getStatus());

            if (projectID != -1) {
                Assert.assertEquals("Incorrect number of projects returned", 1, ((UserProjectsResponse) response.getData()).getProjects().length);
                Assert.assertEquals("Incorrect ProjectID returned", projectID, ((UserProjectsResponse) response.getData()).getProjects()[0].getProjectID());
                Assert.assertEquals("Incorrect project name returned", projectName, ((UserProjectsResponse) response.getData()).getProjects()[0].getName());
                Assert.assertEquals("Incorrect project permissions level returned", 10, ((UserProjectsResponse) response.getData()).getProjects()[0].getPermissionLevel());
            } else {
                Assert.assertEquals("Incorrect number of projects returned", 0, ((UserProjectsResponse) response.getData()).getProjects().length);
            }

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }
}
