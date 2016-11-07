package integration;

import com.google.common.collect.BiMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// TODO: Remove all "Acquire timed out"
// NOTE: User2 should always have 1 level lower permissions on User1's projects.

public class IntegrationTestNew {
    private static final Logger logger = LoggerFactory.getLogger("integrationTest");

    private static final String SERVER_URL = "ws://localhost:8000/ws/";
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
    private static final String user3ID = "_testUser3";
    private static final String user3Pass = "_testPass3";
    private static final String user3FirstName = "_testFirstName3";
    private static final String user3LastName = "_testLastName3";
    private static final String user3Email = "_testEmail3@testDomain.com";
    private static final String fileData1 = "FileData1: _test data1\ntest data 2";
    private static final String fileData2 = "FileData2: _test data1\ntest data 2";
    private static final IRequestSendErrorHandler errHandler = new IRequestSendErrorHandler() {
        @Override
        public void handleRequestSendError() {
            Assert.fail("Failed to send message");
        }
    };
    private static WSManager ws1 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static WSManager ws2 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static WSManager ws3 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static String sender1ID = "";
    private static String sender1Token = "";
    private static String sender2ID = "";
    private static String sender2Token = "";
    private static Project proj1 = new Project(-1, "_testProject1", new HashMap<>(), -1);
    private static File file1 = new File(-1, "_testFile1", "_test1/file/path", 1, null, null);
    private static Project proj2 = new Project(-1, "_testProject2", new HashMap<>(), -1);
    private static File file2 = new File(-1, "_testFile2", "_test2/file/path", 1, null, null);

    private static Map<Project, HashSet<WSManager>> projToWS = new HashMap<>();

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

    @Before
    public void setup() {
        // Set some invalid auth info; avoid null checks.
        ws3.setAuthInfo("", "");
    }

    @Test
    public void integrationTest() throws Exception {
        // Test valid flow
        testUserRegister();
        testUserRegisterDuplicate();
        testUserLogin();
        testUserLoginInvalidCredentials();
        testProjectGetPermissionConstants();
        testProjectGetPermissionConstantsInvalid();
        testUserLookup();
        testUserLookupInvalid();
        testUserProjects();
        testUserProjectsInvalid();
        testProjectCreate();
        testProjectCreateInvalid();
        testUserProjects();
        testProjectLookup();
        testProjectGetFiles(); // should have 0 files
        testProjectGetFilesInvalid();
        testProjectSubscribe(ws1, proj1);
        testProjectSubscribe(ws2, proj2);
        testProjectSubscribeInvalid();
        testProjectGrantPermission(ws1, proj1, user2ID, "read", new WSManager[]{ws2});
        testProjectSubscribe(ws2, proj1);
        testProjectCrossSubscribeInvalid(); // Check to make sure User1 cannot subscribe to Proj2
        testProjectRename();
        testProjectRenameInvalid();
        testFileCreate(ws1, file1, proj1, fileData1, new WSManager[]{ws2});
        testProjectGetFiles(); // should have 1 file in Proj1, 0 in Proj2
        testFileCreate(ws2, file2, proj2, fileData2, new WSManager[]{});
        testProjectGetFiles(); // should have 1 file in Proj1 and Proj2
        testFileCreateInvalid();
        testFilePull(); // Should have no changes
        testFilePullInvalid();
        testFileChange(ws1, file1, new WSManager[]{ws2}); // Test User1 changing File1
        testFilePull(); // Should have 1 change
        testFileChange(ws2, file1, new WSManager[]{ws2}); // Test User2 changing File1
        testFilePull(); // Should have 2 changes

        testFileChange(ws2, file2, new WSManager[]{ws2}); // Test User2 changing File1
        testFilePull(); // Should have 1 change
        testFileChangeInvalid();
        // testProjectGrantWritePermissions
        // testFileCrossChange
        // testFileCrossChangeInvalid
        // testProjectGrantAdminPermissions

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

    private void testUserRegister() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new UserRegisterRequest(user1ID, user1FirstName, user1LastName, user1Email, user1Pass).getRequest(response -> {
            // If registration fails, probably is already there.
            Assert.assertNotEquals(String.format("user %s already registered, rerun test", user1ID), 404, response.getStatus());
            Assert.assertEquals(String.format("Failed to register user: %s", user1ID), 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserRegister timed out");
        }

        req = new UserRegisterRequest(user2ID, user2FirstName, user2LastName, user2Email, user2Pass).getRequest(response -> {
            // If registration fails, probably is already there.
            Assert.assertNotEquals(String.format("user %s already registered, rerun test", user2ID), 404, response.getStatus());
            Assert.assertEquals(String.format("Failed to register user: %s", user2ID), 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserRegister timed out");
        }
    }

    private void testUserRegisterDuplicate() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new UserRegisterRequest(user1ID, "dummy", "dummy", "dummyEmail@domain.tld", "dummyPassword").getRequest(response -> {
            Assert.assertNotEquals(String.format("Failed to throw error when registering user with duplicate username: %s", user1ID), 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserRegisterDuplicateUsername timed out");
        }

        req = new UserRegisterRequest("dummyUsername", "dummy", "dummy", user1Email, "dummyPassword").getRequest(response -> {
            Assert.assertNotEquals(String.format("Failed to throw error when registering user with duplicate email: %s", user1Email), 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserRegisterDuplicateEmail timed out");
        }
    }

    private void testUserLogin() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new UserLoginRequest(user1ID, user1Pass).getRequest(response -> {
            Assert.assertEquals("Failed to log in user 1", 200, response.getStatus());

            sender1Token = ((UserLoginResponse) response.getData()).getToken();
            ws1.setAuthInfo(user1ID, sender1Token);
            sender1ID = user1ID;

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserLogin timed out");
        }

        req = new UserLoginRequest(user2ID, user2Pass).getRequest(response -> {
            Assert.assertEquals("Failed to log in user 2", 200, response.getStatus());

            sender2Token = ((UserLoginResponse) response.getData()).getToken();
            ws2.setAuthInfo(user2ID, sender2Token);
            sender2ID = user2ID;

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserLogin timed out");
        }
    }

    private void testUserLoginInvalidCredentials() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new UserLoginRequest(user1ID, "dummy").getRequest(response -> {
            Assert.assertNotEquals("Error not thrown for incorrect password", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserLogin timed out");
        }

        req = new UserLoginRequest("dummyUsername", user1Pass).getRequest(response -> {
            Assert.assertNotEquals("Error not thrown for invalid username", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("testUserLogin timed out");
        }
    }

    private void testProjectGetPermissionConstants() throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectGetPermissionConstantsRequest().getRequest(response -> {
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
        Assert.assertTrue("Constants map does not contain correct privileges", apiConstants.containsKey("read"));
    }

    private void testProjectGetPermissionConstantsInvalid() throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectGetPermissionConstantsRequest().getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserLookup() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

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
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

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
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserLookupInvalid() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new UserLookupRequest(new String[]{user2ID}).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        ws3.sendRequest(req);

        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new UserLookupRequest(new String[]{"dummyUser"}).getRequest(response -> {
            Assert.assertNotEquals("User lookup of invalid user did not fail", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserProjects() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new UserProjectsRequest().getRequest(response -> {
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
        req = new UserProjectsRequest().getRequest(response -> {
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

    private void testUserProjectsInvalid() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new UserProjectsRequest().getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectCreate() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectCreateRequest(proj1.getName()).getRequest(response -> {
            Assert.assertEquals("Failed to create project", 200, response.getStatus());

            proj1.setProjectID(((ProjectCreateResponse) response.getData()).getProjectID());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        projToWS.put(proj1, new HashSet<>()); // Don't put ws1 in, since it won't receive notifications

        req = new ProjectCreateRequest(proj2.getName()).getRequest(response -> {
            Assert.assertEquals("Failed to create project", 200, response.getStatus());

            proj2.setProjectID(((ProjectCreateResponse) response.getData()).getProjectID());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        projToWS.put(proj2, new HashSet<>()); // Don't put ws1 in, since it won't receive notifications
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectCreateInvalid() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectCreateRequest(proj1.getName()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectCreateRequest(proj1.getName()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on duplicate project name for same user", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectGetFiles() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectGetFilesRequest(proj1.getProjectID()).getRequest(response -> {
            Assert.assertEquals("Failed to get files for project1", 200, response.getStatus());

            if (file1.getFileID() != -1) {
                Assert.assertEquals("Incorrect number of files for Proj1 returned", 1, ((ProjectGetFilesResponse) response.getData()).files.length);
                Assert.assertEquals("Incorrect file ID returned for Proj1, file at index 0", file1.getFileID(), ((ProjectGetFilesResponse) response.getData()).files[0].getFileID());
                Assert.assertEquals("Incorrect file name returned for Proj1, file at index 0", file1.getFilename(), ((ProjectGetFilesResponse) response.getData()).files[0].getFilename());
                // Disabled until mysql os-filepath fix implemented in server
//                Assert.assertEquals("Incorrect file path returned for file at index 0", filePath, ((ProjectGetFilesResponse) response.getData()).files[0].getPermissions());
                Assert.assertEquals("Incorrect file version returned for Proj1, file at index 0", file1.getFileVersion(), ((ProjectGetFilesResponse) response.getData()).files[0].getFileVersion());
            } else {
                Assert.assertEquals("Incorrect number of files returned for Proj1", 0, ((ProjectGetFilesResponse) response.getData()).files.length);
            }

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectGetFilesRequest(proj2.getProjectID()).getRequest(response -> {
            Assert.assertEquals("Failed to get files for project2", 200, response.getStatus());

            if (file2.getFileID() != -1) {
                Assert.assertEquals("Incorrect number of files for Proj2 returned", 1, ((ProjectGetFilesResponse) response.getData()).files.length);
                Assert.assertEquals("Incorrect file ID returned for Proj2, file at index 0", file2.getFileID(), ((ProjectGetFilesResponse) response.getData()).files[0].getFileID());
                Assert.assertEquals("Incorrect file name returned for Proj2, file at index 0", file2.getFilename(), ((ProjectGetFilesResponse) response.getData()).files[0].getFilename());
                // Disabled until mysql os-filepath fix implemented in server
//                Assert.assertEquals("Incorrect file path returned for file at index 0", filePath, ((ProjectGetFilesResponse) response.getData()).files[0].getPermissions());
                Assert.assertEquals("Incorrect file version returned for Proj2, file at index 0", file2.getFileVersion(), ((ProjectGetFilesResponse) response.getData()).files[0].getFileVersion());
            } else {
                Assert.assertEquals("Incorrect number of files returned for Proj2", 0, ((ProjectGetFilesResponse) response.getData()).files.length);
            }

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectGetFilesInvalid() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectGetFilesRequest(proj1.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectGetFilesRequest(proj2.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectGetFilesRequest(proj1.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectSubscribe(WSManager wsMgr, Project proj) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectSubscribeRequest(proj.getProjectID()).getRequest(response -> {
            Assert.assertEquals("Failed to subscribe to project", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        projToWS.get(proj).add(wsMgr);

        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectSubscribeInvalid() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectSubscribeRequest(proj1.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectSubscribeRequest(proj2.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectSubscribeRequest(proj1.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectGrantPermission(WSManager wsMgr, Project proj, String grantUsername, String projPerm, WSManager[] expectNotification) throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        if (projPerm.equalsIgnoreCase("none")) {
            return;
        }

        Byte permByte = apiConstants.get(projPerm);
        req = new ProjectGrantPermissionsRequest(proj.getProjectID(), grantUsername, permByte).getRequest(response -> {
            Assert.assertEquals("Failed to grant " + projPerm + " permission on " + proj.getName() + " to " + grantUsername, 200, response.getStatus());
            waiter.release();
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {
            otherWSMgr.registerNotificationHandler("Project", "GrantPermission", notification -> { // Create notification handler
                Assert.assertEquals("ProjectGrantPermissionNotification gave wrong project ID", proj1.getProjectID(), notification.getResourceID());
                Assert.assertEquals("ProjectGrantPermissionNotification gave wrong username", proj1.getName(), ((ProjectGrantPermissionsNotification) notification.getData()).grantUsername);
                Assert.assertEquals("ProjectGrantPermissionNotification gave wrong permission level", permByte, ((ProjectGrantPermissionsNotification) notification.getData()).permissionLevel);

                otherWSMgr.deregisterNotificationHandler("Project", "GrantPermission");
                waiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectCrossSubscribeInvalid() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectSubscribeRequest(proj2.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectRename() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        proj1.setName(proj1.getName() + "_Renamed");
        proj2.setName(proj2.getName() + "_Renamed");

        req = new ProjectRenameRequest(proj1.getProjectID(), proj1.getName()).getRequest(response -> {
            Assert.assertEquals("Failed to rename project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        ws2.registerNotificationHandler("Project", "Rename", notification -> { // Create notification handler
            Assert.assertEquals("ProjectRenameNotification gave wrong project ID for Proj1", proj1.getProjectID(), notification.getResourceID());
            Assert.assertEquals("ProjectRenameNotification gave wrong newName for Proj1", proj1.getName(), ((ProjectRenameNotification) notification.getData()).newName);

            ws1.deregisterNotificationHandler("Project", "Rename");
            waiter.release();
        });

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectRenameRequest(proj2.getProjectID(), proj2.getName()).getRequest(response -> {
            Assert.assertEquals("Failed to rename project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        ws1.registerNotificationHandler("Project", "Rename", notification -> { // Create notification handler
            Assert.assertEquals("ProjectRenameNotification gave wrong project ID for Proj2", proj2.getProjectID(), notification.getResourceID());
            Assert.assertEquals("ProjectRenameNotification gave wrong newName for Proj2", proj2.getName(), ((ProjectRenameNotification) notification.getData()).newName);

            ws2.deregisterNotificationHandler("Project", "Rename");
            waiter.release();
        });

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectRenameInvalid() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectRenameRequest(proj1.getProjectID(), proj1.getName()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectRenameRequest(proj2.getProjectID(), proj2.getName()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectRenameRequest(proj1.getProjectID(), proj1.getName()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileCreate(WSManager wsMgr, File file, Project proj, String data, WSManager[] expectNotification) throws ConnectException, InterruptedException {
        Semaphore responsewaiter = new Semaphore(0);
        Semaphore notificationwaiter = new Semaphore(0);

        req = new FileCreateRequest(file.getFilename(), file.getRelativePath(), proj.getProjectID(), data.getBytes()).getRequest(response -> {
            Assert.assertEquals("Failed to create file", 200, response.getStatus());

            file.setFileID(((FileCreateResponse) response.getData()).getFileID());

            responsewaiter.release(expectNotification.length);
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {

            otherWSMgr.registerNotificationHandler("File", "Create", notification -> { // Create notification handler
                try {
                    if (!responsewaiter.tryAcquire(5, TimeUnit.SECONDS)) {
                        Assert.fail("Acquire timed out");
                    }
                } catch (InterruptedException e) {
                    // do nothing
                }

                Assert.assertEquals("FileCreateNotification gave wrong project ID", proj.getProjectID(), notification.getResourceID());
                Assert.assertEquals("FileCreateNotification gave wrong file name", file.getFilename(), ((FileCreateNotification) notification.getData()).file.getFilename());
                Assert.assertEquals("FileCreateNotification gave wrong file path", file.getRelativePath(), ((FileCreateNotification) notification.getData()).file.getRelativePath());
                Assert.assertEquals("FileCreateNotification gave wrong file ID", file.getFileID(), ((FileCreateNotification) notification.getData()).file.getFileID());
                Assert.assertEquals("FileCreateNotification gave wrong file version", file.getFileVersion(), ((FileCreateNotification) notification.getData()).file.getFileVersion());

                otherWSMgr.deregisterNotificationHandler("File", "Create");
                notificationwaiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!notificationwaiter.tryAcquire(expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileCreateInvalid() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        /*
         * Check authentication
         */
        req = new FileCreateRequest(file1.getFilename(), file1.getRelativePath(), proj1.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release(1);
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new FileCreateRequest(file2.getFilename(), file2.getRelativePath(), proj2.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release(1);
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Check invalid projectID
         */

        req = new FileCreateRequest("dummyFileName", "dummyFilePath", -1, "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid projectID", 200, response.getStatus());

            waiter.release(1);
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Check missing write permissions
         */

        req = new FileCreateRequest("dummyFileName", "dummyFilePath", proj1.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release(1);
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new FileCreateRequest("dummyFileName", "dummyFilePath", proj2.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release(1);
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Check duplicate file
         */

        req = new FileCreateRequest(file1.getFilename(), file1.getRelativePath(), proj1.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on duplicate file", 200, response.getStatus());

            waiter.release(1);
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new FileCreateRequest(file2.getFilename(), file2.getRelativePath(), proj2.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on duplicate file", 200, response.getStatus());

            waiter.release(1);
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFilePull() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        String[] changes1 = new String[(int) file1.getFileVersion() - 1];
        for (int i = 0; i < changes1.length; i++) {
            changes1[i] = "+5:6:newData" + (i + 1);
        }
        req = new FilePullRequest(file1.getFileID()).getRequest(response -> {
            Assert.assertEquals("Failed to pull file", 200, response.getStatus());
            Assert.assertArrayEquals("FilePullResponse gave wrong base file text for File1", fileData1.getBytes(), ((FilePullResponse) response.getData()).getFileBytes());
            Assert.assertArrayEquals("FilePullResponse gave wrong changes for File1", changes1, ((FilePullResponse) response.getData()).getChanges());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        String[] changes2 = new String[(int) file2.getFileVersion() - 1];
        for (int i = 0; i < changes2.length; i++) {
            changes2[i] = "+5:6:newData" + (i + 1);
        }
        req = new FilePullRequest(file2.getFileID()).getRequest(response -> {
            Assert.assertEquals("Failed to pull file", 200, response.getStatus());
            Assert.assertArrayEquals("FilePullResponse gave wrong base file text for File2", fileData2.getBytes(), ((FilePullResponse) response.getData()).getFileBytes());
            Assert.assertArrayEquals("FilePullResponse gave wrong changes for File2", changes2, ((FilePullResponse) response.getData()).getChanges());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFilePullInvalid() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        /*
         * Check authentication
         */

        req = new FilePullRequest(file1.getFileID()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new FilePullRequest(file2.getFileID()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Test invalid fileID
         */

        req = new FilePullRequest(-1).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid file", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Test permissions
         */

        req = new FilePullRequest(file1.getFileID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new FilePullRequest(file2.getFileID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void registerFileChangeNotificationHandler(WSManager wsMgr, File file, String[] changes, Semaphore waiter) {
        wsMgr.registerNotificationHandler("File", "Change", notification -> { // Create notification handler
            Assert.assertEquals("FileChangeNotification gave wrong file ID", file.getFileID(), notification.getResourceID());
            Assert.assertArrayEquals("FileChangeNotification gave wrong changes", changes, ((FileChangeNotification) notification.getData()).changes);
            Assert.assertEquals("FileChangeNotification gave wrong file version", file.getFileVersion() + 1, ((FileChangeNotification) notification.getData()).fileVersion);
            Assert.assertEquals("FileChangeNotification gave wrong base file version", file.getFileVersion(), ((FileChangeNotification) notification.getData()).baseFileVersion);

            wsMgr.deregisterNotificationHandler("File", "Change");
            waiter.release();
        });
    }

    private void testFileChange(WSManager wsMgr, File file, WSManager[] expectNotification) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[]{"+5:6:newData" + file.getFileVersion()};
        req = new FileChangeRequest(file.getFileID(), changes, file.getFileVersion()).getRequest(response -> {
            Assert.assertEquals("Failed to change File1", 200, response.getStatus());
            Assert.assertEquals("FileChangeResponse gave wrong version for File1", file.getFileVersion() + 1, ((FileChangeResponse) response.getData()).getFileVersion());

            waiter.release();
        }, errHandler);
        for (WSManager otherWSMgr : expectNotification) {
            registerFileChangeNotificationHandler(otherWSMgr, file, changes, waiter);
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        file.setFileVersion(file.getFileVersion() + 1);
    }

    private void testFileChangeInvalid() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        /*
         * Check authentication
         */
        String[] changes = new String[]{"+5:6:newData" + file1.getFileVersion()};
        req = new FileChangeRequest(file1.getFileID(), changes, file1.getFileVersion()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }


        /*
         * Test invalid fileID
         */
        req = new FileChangeRequest(-1, changes, file1.getFileVersion()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid file", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Test invalid permissions
         */
        req = new FileChangeRequest(file2.getFileID(), changes, file2.getFileVersion()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Test outdated file version (?)
         */
//        req = new FileChangeRequest(file1.getFileID(), changes, file1.getFileVersion()-1).getRequest(response -> {
//            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());
//
//            waiter.release();
//        }, errHandler);
//
//        ws1.sendRequest(req);
//        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
//            Assert.fail("Acquire timed out");
//        }

        /*
         * Test file version too high
         */

        /*
         * Test invalid patch
         */

        req = new FileChangeRequest(file2.getFileID(), new String[]{"invalidPatch"}, file2.getFileVersion()).getRequest(response -> {
            Assert.assertEquals("Failed to change File2", 200, response.getStatus());
            Assert.assertEquals("FileChangeResponse gave wrong version for File2", file2.getFileVersion() + 1, ((FileChangeResponse) response.getData()).getFileVersion());

            waiter.release();
        }, errHandler);
        registerFileChangeNotificationHandler(ws2, file2, new String[]{"invalidPatch"}, waiter);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        file2.setFileVersion(file2.getFileVersion() + 1);
    }


    /*
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */


    private void testValidProjectWrite() throws InterruptedException {
        // switch to user2
        wsMgr.setAuthInfo(user2ID, sender2Token);

        logger.info(String.format("Attempting to legally change file %d as user %s", fileID, user2ID));
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[]{"+5:6:newData" + fileVersion};
        req = new FileChangeRequest(fileID, changes, fileVersion).getRequest(response -> {
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
        req = new ProjectGrantPermissionsRequest(projectID, user2ID, readPerm).getRequest(response -> {
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
        req = new FileChangeRequest(fileID, changes, fileVersion).getRequest(response -> {
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

        req = new ProjectRevokePermissionsRequest(projectID, user2ID).getRequest(response -> {
            Assert.assertEquals("Failed to revoke permission", 200, response.getStatus());
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

        req = new ProjectSubscribeRequest(projectID).getRequest(response -> {
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

    private void testFileMove() throws InterruptedException, ConnectException {
        filePath = filePath + "/new";

        logger.info(String.format("Moving file %d to %s", fileID, filePath));
        Semaphore waiter = new Semaphore(0);

        req = new FileMoveRequest(fileID, filePath).getRequest(response -> {
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

        req = new FileRenameRequest(fileID, fileName).getRequest(response -> {
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

    private void testFileDelete() throws InterruptedException, ConnectException {
        logger.info(String.format("Deleting file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        req = new FileDeleteRequest(fileID).getRequest(response -> {
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

        req = new ProjectUnsubscribeRequest(projectID).getRequest(response -> {
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

        req = new ProjectDeleteRequest(projectID).getRequest(response -> {
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

    private void testProjectLookup() throws ConnectException, InterruptedException {
        logger.info(String.format("Looking up project with ID %d", projectID));
        Semaphore waiter = new Semaphore(0);

        req = new ProjectLookupRequest(new Long[]{projectID}).getRequest(response -> {
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

        req = new UserDeleteRequest().getRequest(deleteResponse -> {
            Assert.assertEquals("Failed to delete user", 200, deleteResponse.getStatus());

            // verifying user was deleted
            req = new UserLoginRequest(user1ID, user1Pass).getRequest(loginResponse -> {
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
