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
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.*;
import websocket.models.requests.*;
import websocket.models.responses.*;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class IntegrationTestNew {
    private static final Logger logger = LoggerFactory.getLogger("integrationTest");

    private static final String SERVER_URL = "ws://localhost:8000/ws/";
    private static WSManager ws1 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static WSManager ws2 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));

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

    private static String sender1ID = "";
    private static String sender1Token = "";
    private static String sender2ID = "";
    private static String sender2Token = "";

    private static Project proj1 = new Project(-1, "_testProject1", new HashMap<>(), -1);
    private static File file1 = new File(-1, "_testFile1", "_test1/file/path", 1, null, null);
    private static final String fileData1 = "FileData1: _test data1\ntest data 2";

    private static Project proj2 = new Project(-1, "_testProject2", new HashMap<>(), -1);
    private static File file2 = new File(-1, "_testFile2", "_test2/file/path", 1, null, null);
    private static final String fileData2 = "FileData2: _test data1\ntest data 2";













    private static final IRequestSendErrorHandler errHandler = new IRequestSendErrorHandler() {
        @Override
        public void handleRequestSendError() {
            System.out.println("Failed to send");
        }
    };

//    private static WSManager wsMgr = new WSManager(new ConnectionConfig(SERVER_URL, false, 5));

    private static BiMap<String, Byte> apiConstants;

    private Request req;

    @After
    public void cleanup() throws ConnectException, InterruptedException {
        // Create a new connection - The old one may have died if there were errors.
        ws1 = new WSManager(new ConnectionConfig(SERVER_URL, false, 5));
        if (!"".equals(sender1ID)) {
            ws1.setAuthInfo(sender1ID, sender1Token);
            logger.info("Cleaning up user_1");

            ws1.sendRequest(new UserDeleteRequest().getRequest(null, null));
        }

        ws2 = new WSManager(new ConnectionConfig(SERVER_URL, false, 5));
        if (!"".equals(sender2ID)) {
            ws2.setAuthInfo(sender2ID, sender2Token);
            logger.info("Cleaning up user_2");

            ws2.sendRequest(new UserDeleteRequest().getRequest(null, null));
        }
    }

    @Test
    public void integrationTest() throws Exception {
        // Test valid flow
        testUsersRegister();
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

        testOtherUserRegisterAndLogin();
        testInvalidAccess();
        testProjectGrantReadPermission();
        testInvalidProjectWrite();
        testProjectUpdatePermission();
        testValidProjectWrite();
        testValidRevokePermission();
        testInvalidAccess();

        testFileDelete();
        testProjectGetFiles(); // should have 0 files
        testProjectUnsubscribe();
        testProjectSubscribe();
        testProjectLookup();
        testUserProjects();
        testProjectDelete();
        testUserProjects();
        // ProjectGetOnlineClientsRequest (NOT IMPLEMENTED)

        testUserDelete();

        // Run invalid method type, expect error
        // Re-run login, expect error(?)
        // Run invalid login, expect error

        Thread.sleep(1000);
    }

    private void testUsersRegister() throws InterruptedException, ConnectException {
        logger.info(String.format("Registering user: %s", user1ID));
        Semaphore waiter = new Semaphore(0);

        req = new UserRegisterRequest(user1ID, user1FirstName, user1LastName, user1Email, user1Pass).getRequest( response -> {
            // If registration fails, probably is already there.
            Assert.assertNotEquals(String.format("user %s already registered, rerun test", user1ID), 404, response.getStatus());
            Assert.assertEquals(String.format("Failed to register user: %s", user1ID), 200, response.getStatus());

            waiter.release();
        }, errHandler);
        ws1.sendRequest(req);

        req = new UserRegisterRequest(user2ID, user2FirstName, user2LastName, user2Email, user2Pass).getRequest( response -> {
            // If registration fails, probably is already there.
            Assert.assertNotEquals(String.format("user %s already registered, rerun test", user2ID), 404, response.getStatus());
            Assert.assertEquals(String.format("Failed to register user: %s", user2ID), 200, response.getStatus());

            waiter.release();
        }, errHandler);
        ws2.sendRequest(req);

        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserLogin() throws InterruptedException, ConnectException {
        logger.info(String.format("Logging in"));
        Semaphore waiter = new Semaphore(0);

        req = new UserLoginRequest(user1ID, user1Pass).getRequest(response -> {
            Assert.assertEquals("Failed to log in user 1", 200, response.getStatus());

            sender1Token = ((UserLoginResponse) response.getData()).getToken();
            ws1.setAuthInfo(user1ID, sender1Token);
            sender1ID = user1ID;

            waiter.release();
        }, errHandler);
        ws1.sendRequest(req);

        req = new UserLoginRequest(user2ID, user2Pass).getRequest(response -> {
            Assert.assertEquals("Failed to log in user 2", 200, response.getStatus());

            sender2Token = ((UserLoginResponse) response.getData()).getToken();
            ws2.setAuthInfo(user2ID, sender2Token);
            sender2ID = user2ID;

            waiter.release();
        }, errHandler);
        ws2.sendRequest(req);

        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
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

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        Assert.assertNotEquals("Constants map empty", 0, apiConstants.size());

        // NOTE: I chose a permission which (in my opinion) will probably always be around
        //       if the "read" permission is ever removed from the server, this will fail - Joel
        Assert.assertTrue("Constants map does not contain correct provlages", apiConstants.containsKey("read"));
    }

    private void testUserLookup() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        logger.info(String.format("Looking up user with ID %s", user2ID));
        req = new UserLookupRequest(new String[]{user2ID}).getRequest(response -> {
            Assert.assertEquals("Failed to lookup user 2", 200, response.getStatus());

            Assert.assertEquals("Incorrect number of users returned when looking up user 2", 1, ((UserLookupResponse) response.getData()).getUsers().length);
            Assert.assertEquals("Incorrect first name returned when looking up user 2", user2FirstName, ((UserLookupResponse) response.getData()).getUsers()[0].getFirstName());
            Assert.assertEquals("Incorrect last name returned when looking up user 2", user2LastName, ((UserLookupResponse) response.getData()).getUsers()[0].getLastName());
            Assert.assertEquals("Incorrect email returned when looking up user 2", user2Email, ((UserLookupResponse) response.getData()).getUsers()[0].getEmail());
            Assert.assertEquals("Incorrect username returned when looking up user 2", user2ID, ((UserLookupResponse) response.getData()).getUsers()[0].getUsername());

            waiter.release();
        }, errHandler);
        ws1.sendRequest(req);

        logger.info(String.format("Looking up user with ID %s", user1ID));
        req = new UserLookupRequest(new String[]{user1ID}).getRequest(response -> {
            Assert.assertEquals("Failed to lookup user 1", 200, response.getStatus());

            Assert.assertEquals("Incorrect number of users returned when looking up user 1", 1, ((UserLookupResponse) response.getData()).getUsers().length);
            Assert.assertEquals("Incorrect first name returned when looking up user 1", user1FirstName, ((UserLookupResponse) response.getData()).getUsers()[0].getFirstName());
            Assert.assertEquals("Incorrect last name returned when looking up user 1", user1LastName, ((UserLookupResponse) response.getData()).getUsers()[0].getLastName());
            Assert.assertEquals("Incorrect email returned when looking up user 1", user1Email, ((UserLookupResponse) response.getData()).getUsers()[0].getEmail());
            Assert.assertEquals("Incorrect username returned when looking up user 1", user1ID, ((UserLookupResponse) response.getData()).getUsers()[0].getUsername());

            waiter.release();
        }, errHandler);
        ws2.sendRequest(req);

        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserProjects() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        logger.info(String.format("Looking up projects for user with ID %s", user1ID));
        req = new UserProjectsRequest().getRequest( response -> {
            Assert.assertEquals("Failed to lookup projects for user 1", 200, response.getStatus());

            if (proj1.getProjectID() != -1) {
                Assert.assertEquals("Incorrect number of projects returned when looking up projects for user 1", 1, ((UserProjectsResponse) response.getData()).getProjects().length);
                Assert.assertEquals("Incorrect ProjectID returned when looking up projects for user 1", proj1.getProjectID(), ((UserProjectsResponse) response.getData()).getProjects()[0].getProjectID());
                Assert.assertEquals("Incorrect project name returned when looking up projects for user 1", proj1.getName(), ((UserProjectsResponse) response.getData()).getProjects()[0].getName());
                Assert.assertEquals("Incorrect project permissions level returned when looking up projects for user 1", 10, ((UserProjectsResponse) response.getData()).getProjects()[0].getPermissionLevel());
            } else {
                Assert.assertEquals("Incorrect number of projects returned when looking up projects for user 1", 0, ((UserProjectsResponse) response.getData()).getProjects().length);
            }

            waiter.release();
        }, errHandler);
        ws1.sendRequest(req);

        logger.info(String.format("Looking up projects for user with ID %s", user2ID));
        req = new UserProjectsRequest().getRequest( response -> {
            Assert.assertEquals("Failed to lookup projects for user 2", 200, response.getStatus());

            if (proj2.getProjectID() != -1) {
                Assert.assertEquals("Incorrect number of projects returned when looking up projects for user 2", 1, ((UserProjectsResponse) response.getData()).getProjects().length);
                Assert.assertEquals("Incorrect ProjectID returned when looking up projects for user 2", proj2.getProjectID(), ((UserProjectsResponse) response.getData()).getProjects()[0].getProjectID());
                Assert.assertEquals("Incorrect project name returned when looking up projects for user 2", proj2.getName(), ((UserProjectsResponse) response.getData()).getProjects()[0].getName());
                Assert.assertEquals("Incorrect project permissions level returned when looking up projects for user 2", 10, ((UserProjectsResponse) response.getData()).getProjects()[0].getPermissionLevel());
            } else {
                Assert.assertEquals("Incorrect number of projects returned when looking up projects for user 2", 0, ((UserProjectsResponse) response.getData()).getProjects().length);
            }

            waiter.release();
        }, errHandler);
        ws2.sendRequest(req);

        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }












    private void registerFileChangeNotificationHandler(String[] changes, Semaphore waiter) {
        wsMgr.registerNotificationHandler("File", "Change", notification -> { // Create notification handler
            Assert.assertEquals("FileChangeNotification gave wrong file ID", fileID, notification.getResourceID());
            Assert.assertArrayEquals("FileChangeNotification gave wrong changes", changes, ((FileChangeNotification) notification.getData()).changes);
            Assert.assertEquals("FileChangeNotification gave wrong file version", fileVersion + 1, ((FileChangeNotification) notification.getData()).fileVersion);
            Assert.assertEquals("FileChangeNotification gave wrong base file version", fileVersion, ((FileChangeNotification) notification.getData()).baseFileVersion);

            wsMgr.deregisterNotificationHandler("File", "Change");
            waiter.release();
        });
    }

    private void testValidProjectWrite() throws InterruptedException {
        // switch to user2
        wsMgr.setAuthInfo(user2ID, sender2Token);

        logger.info(String.format("Attempting to legally change file %d as user %s", fileID, user2ID));
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[]{"+5:6:newData" + fileVersion};
        req = new FileChangeRequest(fileID, changes, fileVersion).getRequest( response -> {
            Assert.assertEquals("Failed to change file", 200, response.getStatus());
            Assert.assertEquals("FileChangeResponse gave wrong file version", fileVersion + 1, ((FileChangeResponse) response.getData()).getFileVersion());

            waiter.release();
        }, errHandler);

        registerFileChangeNotificationHandler(changes, waiter);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        fileVersion++;

        // switch back to user 1
        wsMgr.setAuthInfo(user1ID, sender1Token);
    }

    private void testProjectUpdatePermission() throws InterruptedException {
        logger.info(String.format("Granting read permission to user with id %s", user2ID));
        Semaphore waiter = new Semaphore(0);

        Byte readPerm = apiConstants.get("write");
        req = new ProjectGrantPermissionsRequest(projectID, user2ID, readPerm).getRequest( response -> {
            Assert.assertEquals("Failed to grant permission", 200, response.getStatus());
            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testInvalidProjectWrite() throws InterruptedException {
        // switch to user2
        wsMgr.setAuthInfo(user2ID, sender2Token);

        logger.info(String.format("Attempting to illegally change file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[]{"+5:6:newData" + fileVersion};
        req = new FileChangeRequest(fileID, changes, fileVersion).getRequest( response -> {
            Assert.assertNotEquals("Failed to change file", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // switch back to user 1
        wsMgr.setAuthInfo(user1ID, sender1Token);
    }

    private void testValidRevokePermission() throws InterruptedException {
        logger.info(String.format("Revoking permission for user with id %s", user2ID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectRevokePermissionsRequest(projectID, user2ID).getRequest( response -> {
            Assert.assertEquals("Failed to revoke permission", 200, response.getStatus());
            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectGrantReadPermission() throws InterruptedException {
        logger.info(String.format("Granting read permission to user with id %s", user2ID));
        Semaphore waiter = new Semaphore(0);

        Byte readPerm = apiConstants.get("read");
        req = new ProjectGrantPermissionsRequest(projectID, user2ID, readPerm).getRequest( response -> {
            Assert.assertEquals("Failed to grant permission", 200, response.getStatus());
            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testInvalidAccess() throws InterruptedException {
        // switch to user2
        wsMgr.setAuthInfo(user2ID, sender2Token);
        logger.info(String.format("Illegally trying to subscribe to project with id %d", projectID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectSubscribeRequest(projectID).getRequest( response -> {
            Assert.assertNotEquals("Subscribed to project improperly", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // switch back to user 1
        wsMgr.setAuthInfo(user1ID, sender1Token);
    }

    private void testOtherUserRegisterAndLogin() throws ConnectException, InterruptedException {
        testUserRegister(user2ID, user2FirstName, user2LastName, user2Email, user2Pass);

        logger.info(String.format("Logging in: %s", user2ID));
        Semaphore waiter = new Semaphore(0);

        req = new UserLoginRequest(user2ID, user2Pass).getRequest(response -> {
            Assert.assertEquals("Failed to log in", 200, response.getStatus());

            sender2Token = ((UserLoginResponse) response.getData()).getToken();
            sender2ID = user2ID;

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
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
        registerFileChangeNotificationHandler(changes, waiter);

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
            Assert.assertEquals("Incorrect project permissions name for owner returned", user1ID, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(user1ID).getUsername());
            Assert.assertEquals("Incorrect project permissions level for owner returned", 10, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(user1ID).getPermissionLevel());
            Assert.assertEquals("Incorrect project permissions granted by field for owner returned", user1ID, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(user1ID).getGrantedBy());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserDelete() throws InterruptedException, ConnectException {
        logger.info(String.format("Deleting user: %s", user1ID));
        Semaphore waiter = new Semaphore(0);
        Semaphore loginWaiter = new Semaphore(0);

        req = new UserDeleteRequest().getRequest( deleteResponse -> {
            Assert.assertEquals("Failed to delete user", 200, deleteResponse.getStatus());

            // verifying user was deleted
            req = new UserLoginRequest(user1ID, user1Pass).getRequest( loginResponse -> {
                Assert.assertNotEquals("Failed to log in", 200, loginResponse.getStatus());
                loginWaiter.release();
            }, errHandler);
            wsMgr.sendRequest(req);

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);

        if (!loginWaiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        if (!waiter.tryAcquire(10, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

}
