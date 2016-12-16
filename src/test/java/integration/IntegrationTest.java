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
import websocket.models.notifications.FileChangeNotification;
import websocket.models.notifications.FileCreateNotification;
import websocket.models.notifications.FileMoveNotification;
import websocket.models.notifications.FileRenameNotification;
import websocket.models.notifications.ProjectGrantPermissionsNotification;
import websocket.models.notifications.ProjectRenameNotification;
import websocket.models.notifications.ProjectRevokePermissionsNotification;
import websocket.models.requests.FileChangeRequest;
import websocket.models.requests.FileCreateRequest;
import websocket.models.requests.FileDeleteRequest;
import websocket.models.requests.FileMoveRequest;
import websocket.models.requests.FilePullRequest;
import websocket.models.requests.FileRenameRequest;
import websocket.models.requests.ProjectCreateRequest;
import websocket.models.requests.ProjectDeleteRequest;
import websocket.models.requests.ProjectGetFilesRequest;
import websocket.models.requests.ProjectGetPermissionConstantsRequest;
import websocket.models.requests.ProjectGrantPermissionsRequest;
import websocket.models.requests.ProjectLookupRequest;
import websocket.models.requests.ProjectRenameRequest;
import websocket.models.requests.ProjectRevokePermissionsRequest;
import websocket.models.requests.ProjectSubscribeRequest;
import websocket.models.requests.ProjectUnsubscribeRequest;
import websocket.models.requests.UserDeleteRequest;
import websocket.models.requests.UserLoginRequest;
import websocket.models.requests.UserLookupRequest;
import websocket.models.requests.UserProjectsRequest;
import websocket.models.requests.UserRegisterRequest;
import websocket.models.responses.FileChangeResponse;
import websocket.models.responses.FileCreateResponse;
import websocket.models.responses.FilePullResponse;
import websocket.models.responses.ProjectCreateResponse;
import websocket.models.responses.ProjectGetFilesResponse;
import websocket.models.responses.ProjectGetPermissionConstantsResponse;
import websocket.models.responses.ProjectLookupResponse;
import websocket.models.responses.UserLoginResponse;
import websocket.models.responses.UserLookupResponse;
import websocket.models.responses.UserProjectsResponse;

import java.net.ConnectException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class IntegrationTest extends UserBasedIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger("integrationTest");

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
    private static final IRequestSendErrorHandler errHandler = () -> Assert.fail("Failed to send message");
    private static WSManager ws1 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static WSManager ws2 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static WSManager ws3 = new WSManager(new ConnectionConfig(SERVER_URL, false, 1));
    private static Project proj1 = new Project(-1, "_testProject1", new HashMap<>());
    private static File file1 = new File(-1, "_testFile1", "_test1/file/path", 1, null, null);
    private static Project proj2 = new Project(-1, "_testProject2", new HashMap<>());
    private static File file2 = new File(-1, "_testFile2", "_test2/file/path", 1, null, null);

    private static Map<Project, HashSet<WSManager>> projToWS = new HashMap<>();

    //    private static WSManager wsMgr = new WSManager(new ConnectionConfig(SERVER_URL, false, 5));
    private static BiMap<String, Byte> apiConstants;

    private Request req;

    @After
    public void cleanup() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        cleanupUser(logger, user1ID, user1Pass, waiter, errHandler);
        cleanupUser(logger, user2ID, user2Pass, waiter, errHandler);
        cleanupUser(logger, user3ID, user3Pass, waiter, errHandler);

        // Wait for cleanup to finish
        waiter.tryAcquire(3, 10, TimeUnit.SECONDS);
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
        testProjectLookupInvalid();
        testProjectGetFiles(); // should have 0 files
        testProjectGetFilesInvalid();
        testProjectSubscribe(ws1, proj1);
        testProjectSubscribe(ws2, proj2);
        testProjectSubscribeInvalid();
        testProjectGrantPermission(ws1, proj1, user2ID, "read", new WSManager[]{ws2});
        testProjectCrossSubscribeInvalid();
        testProjectSubscribe(ws2, proj1);
        testProjectCrossSubscribeInvalid(); // Check to make sure User1 cannot subscribe to Proj2
        testProjectRename(ws1, proj1, "_Renamed", new WSManager[]{ws2});
        testProjectRename(ws2, proj2, "_Renamed", new WSManager[]{});
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
        testFileChange(ws2, file2, new WSManager[]{}); // Test User2 changing File1
        testFilePull(); // Should have 1 changes on both files
        testFileChangeInvalid();
        testProjectGrantPermission(ws1, proj1, user2ID, "write", new WSManager[]{ws2});
        testFileChange(ws2, file1, new WSManager[]{ws1}); // Test User2 changing File1
        testFilePull(); // Should have 2 changes on File1, 1 change on File2

        // Test File Moves
        testFileMove(ws1, file1, "newPathWS1", new WSManager[]{ws2});
        testProjectGetFiles();
        testFileMove(ws2, file1, "newPathWS2", new WSManager[]{ws1});
        testProjectGetFiles();
        testFileMove(ws2, file2, "newPathWS2", new WSManager[]{});
        testProjectGetFiles();
        testFileMoveInvalid();

        testFileChange(ws1, file1, new WSManager[]{ws2}); // Test User1 changing File1
        testFilePull(); // Should have 3 changes on File1, 1 change on File2
        testFileChange(ws2, file2, new WSManager[]{}); // Test User2 changing File2
        testFilePull(); // Should have 3 changes on File1, 2 change on File2

        testFileRename(ws1, file1, "_newNameWS1", new WSManager[]{ws2});
        testProjectGetFiles();
        testFileRename(ws2, file1, "_newNameWS2", new WSManager[]{ws1});
        testProjectGetFiles();
        testFileRename(ws2, file2, "_newNameWS2", new WSManager[]{});
        testProjectGetFiles();

        testFileChange(ws1, file1, new WSManager[]{ws2}); // Test User1 changing File1
        testFilePull(); // Should have 4 changes on File1, 2 change on File2
        testFileChange(ws2, file2, new WSManager[]{}); // Test User2 changing File2
        testFilePull(); // Should have 4 changes on File1, 3 change on File2

        testProjectGrantPermission(ws1, proj1, user2ID, "admin", new WSManager[]{ws2});
        testProjectGrantPermission(ws2, proj1, user3ID, "read", new WSManager[]{ws1});

        testProjectDeleteInvalid(); // Test this here to make sure even admins don't have deletion rights.

        testProjectRevokePermissionInvalid();
        testProjectRevokePermission(ws2, proj1, user3ID, new WSManager[]{ws1});
        testProjectRevokePermission(ws2, proj1, user2ID, new WSManager[]{ws1});
        testFileChange(ws1, file1, new WSManager[]{}); // Test User1 changing File1; Should have 5 changes on File1, 3 change on File2
        testProjectGrantPermission(ws1, proj1, user2ID, "write", new WSManager[]{ws2});
        testProjectSubscribe(ws2, proj1);

        testProjectUnsubscribe(ws2, proj1);
        testFileChange(ws1, file1, new WSManager[]{}); // Test User1 changing File1; Should have 6 changes on File1, 3 change on File2
        testProjectSubscribe(ws2, proj1);

        testFileDeleteInvalid();
        testFileDelete(ws2, file1, new WSManager[]{ws1});
        testFileDelete(ws2, file2, new WSManager[]{});
        testProjectGetFiles(); // should have 0 files each

        testProjectLookup(ws1, user1ID, proj1);
        testProjectLookup(ws2, user2ID, proj2);

        testProjectDelete(ws1, proj1, new WSManager[]{ws2});
        testProjectDelete(ws2, proj2, new WSManager[]{});

        testUserProjects();

        testUserDelete(ws1, user1ID, user1Pass);
        testUserDelete(ws2, user2ID, user2Pass);

        testUserLogin(ws3, user3ID, user3Pass);
        testUserDelete(ws3, user3ID, user3Pass);

        Thread.sleep(1000);
    }

    private void testUserRegister() throws ConnectException, InterruptedException {
        testUserRegister(ws1, user1ID, user1FirstName, user1LastName, user1Email, user1Pass);
        testUserRegister(ws2, user2ID, user2FirstName, user2LastName, user2Email, user2Pass);
        testUserRegister(ws3, user3ID, user3FirstName, user3LastName, user3Email, user3Pass);
    }

    private void testUserRegister(WSManager wsMgr, String userID, String userFirstName, String userLastName, String userEmail, String userPass) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new UserRegisterRequest(userID, userFirstName, userLastName, userEmail, userPass).getRequest(response -> {
            // If registration fails, probably is already there.
            Assert.assertNotEquals(String.format("user %s already registered, rerun test", userID), 404, response.getStatus());
            Assert.assertEquals(String.format("Failed to register user: %s", userID), 200, response.getStatus());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
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
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("testUserRegisterDuplicateUsername timed out");
        }

        req = new UserRegisterRequest("dummyUsername", "dummy", "dummy", user1Email, "dummyPassword").getRequest(response -> {
            Assert.assertNotEquals(String.format("Failed to throw error when registering user with duplicate email: %s", user1Email), 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("testUserRegisterDuplicateEmail timed out");
        }
    }

    private void testUserLogin() throws ConnectException, InterruptedException {
        testUserLogin(ws1, user1ID, user1Pass);
        testUserLogin(ws2, user2ID, user2Pass);
    }

    private void testUserLogin(WSManager wsMgr, String userID, String userPass) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new UserLoginRequest(userID, userPass).getRequest(response -> {
            Assert.assertEquals("Failed to log in user " + userID, 200, response.getStatus());

            String senderToken = ((UserLoginResponse) response.getData()).getToken();
            wsMgr.setAuthInfo(userID, senderToken);

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
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
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("testUserLogin timed out");
        }

        req = new UserLoginRequest("dummyUsername", user1Pass).getRequest(response -> {
            Assert.assertNotEquals("Error not thrown for invalid username", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
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

    private void testUserLookup() throws InterruptedException, ConnectException {
        testUserLookup(ws1, user1ID, user1FirstName, user1LastName, user1Email);
        testUserLookup(ws2, user1ID, user1FirstName, user1LastName, user1Email);
        testUserLookup(ws1, user2ID, user2FirstName, user2LastName, user2Email);
        testUserLookup(ws2, user2ID, user2FirstName, user2LastName, user2Email);
    }

    private void testUserLookup(WSManager wsMgr, String userID, String userFirstName, String userLastName, String userEmail) throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new UserLookupRequest(new String[]{userID}).getRequest(response -> {
            Assert.assertEquals("Failed to lookup user 2", 200, response.getStatus());

            Assert.assertEquals("Incorrect number of users returned when looking up user 2", 1, ((UserLookupResponse) response.getData()).getUsers().length);
            Assert.assertEquals("Incorrect first name returned when looking up user 2", userFirstName, ((UserLookupResponse) response.getData()).getUsers()[0].getFirstName());
            Assert.assertEquals("Incorrect last name returned when looking up user 2", userLastName, ((UserLookupResponse) response.getData()).getUsers()[0].getLastName());
            Assert.assertEquals("Incorrect email returned when looking up user 2", userEmail, ((UserLookupResponse) response.getData()).getUsers()[0].getEmail());
            Assert.assertEquals("Incorrect username returned when looking up user 2", userID, ((UserLookupResponse) response.getData()).getUsers()[0].getUsername());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
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

        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new UserLookupRequest(new String[]{"dummyUser"}).getRequest(response -> {
            Assert.assertNotEquals("User lookup of invalid user did not fail", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserProjects() throws ConnectException, InterruptedException {
        testUserProjects(ws1, proj1);
        testUserProjects(ws2, proj2);
    }

    private void testUserProjects(WSManager wsMgr, Project proj) throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new UserProjectsRequest().getRequest(response -> {
            Assert.assertEquals("Failed to lookup projects", 200, response.getStatus());

            if (proj.getProjectID() != -1) {
                Assert.assertEquals("Incorrect number of projects returned when looking up projects", 1, ((UserProjectsResponse) response.getData()).getProjects().length);
                Assert.assertEquals("Incorrect ProjectID returned when looking up projects", proj.getProjectID(), ((UserProjectsResponse) response.getData()).getProjects()[0].getProjectID());
                Assert.assertEquals("Incorrect project name returned when looking up projects", proj.getName(), ((UserProjectsResponse) response.getData()).getProjects()[0].getName());
            } else {
                Assert.assertEquals("Incorrect number of projects returned when looking up projects", 0, ((UserProjectsResponse) response.getData()).getProjects().length);
            }

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
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
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectCreate() throws InterruptedException, ConnectException {
        testProjectCreate(ws1, proj1);
        testProjectCreate(ws2, proj2);
    }

    private void testProjectCreate(WSManager wsMgr, Project proj) throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectCreateRequest(proj.getName()).getRequest(response -> {
            Assert.assertEquals("Failed to create project", 200, response.getStatus());

            proj.setProjectID(((ProjectCreateResponse) response.getData()).getProjectID());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
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

    private void testProjectGetFiles() throws InterruptedException, ConnectException {
        testProjectGetFiles(ws1, proj1, file1);
        testProjectGetFiles(ws2, proj2, file2);
    }

    private void testProjectGetFiles(WSManager wsMgr, Project proj, File file) throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectGetFilesRequest(proj.getProjectID()).getRequest(response -> {
            Assert.assertEquals("Failed to get files for project1", 200, response.getStatus());

            if (file.getFileID() != -1) {
                Assert.assertEquals("Incorrect number of files for Proj returned", 1, ((ProjectGetFilesResponse) response.getData()).files.length);
                Assert.assertEquals("Incorrect file ID returned for Proj, file at index 0", file.getFileID(), ((ProjectGetFilesResponse) response.getData()).files[0].getFileID());
                Assert.assertEquals("Incorrect file name returned for Proj, file at index 0", file.getFilename(), ((ProjectGetFilesResponse) response.getData()).files[0].getFilename());
                // Disabled until mysql os-filepath fix implemented in server
//                Assert.assertEquals("Incorrect file path returned for file at index 0", filePath, ((ProjectGetFilesResponse) response.getData()).files[0].getPermissions());
                Assert.assertEquals("Incorrect file version returned for Proj, file at index 0", file.getFileVersion(), ((ProjectGetFilesResponse) response.getData()).files[0].getFileVersion());
                Assert.assertEquals("Incorrect file location returned for Proj, file at index 0", file.getRelativePath(), ((ProjectGetFilesResponse) response.getData()).files[0].getRelativePath().replace('\\', '/'));
            } else {
                Assert.assertEquals("Incorrect number of files returned for Proj", 0, ((ProjectGetFilesResponse) response.getData()).files.length);
            }

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
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
            otherWSMgr.registerNotificationHandler("Project", "GrantPermissions", notification -> { // Create notification handler
                Assert.assertEquals("ProjectGrantPermissionNotification gave wrong project ID", proj.getProjectID(), notification.getResourceID());
                Assert.assertEquals("ProjectGrantPermissionNotification gave wrong username", grantUsername, ((ProjectGrantPermissionsNotification) notification.getData()).grantUsername);
                Assert.assertEquals("ProjectGrantPermissionNotification gave wrong permission level", permByte, ((ProjectGrantPermissionsNotification) notification.getData()).permissionLevel);

                otherWSMgr.deregisterNotificationHandler("Project", "GrantPermissions");
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
            Assert.assertNotEquals("Failed to throw permissions error on lack of Admin permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectRename(WSManager wsMgr, Project proj, String postfix, WSManager[] expectNotification) throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        String newName = proj.getName() + postfix;

        req = new ProjectRenameRequest(proj.getProjectID(), newName).getRequest(response -> {
            Assert.assertEquals("Failed to rename project", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {
            otherWSMgr.registerNotificationHandler("Project", "Rename", notification -> { // Create notification handler
                Assert.assertEquals("ProjectRenameNotification gave wrong project ID for Proj1", proj.getProjectID(), notification.getResourceID());
                Assert.assertEquals("ProjectRenameNotification gave wrong newName for Proj1", newName, ((ProjectRenameNotification) notification.getData()).newName);

                otherWSMgr.deregisterNotificationHandler("Project", "Rename");
                waiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        proj.setName(newName);
    }

    private void testProjectRenameInvalid() throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectRenameRequest(proj1.getProjectID(), proj1.getName()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectRenameRequest(proj2.getProjectID(), proj2.getName()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectRenameRequest(proj1.getProjectID(), proj1.getName()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileCreate(WSManager wsMgr, File file, Project proj, String data, WSManager[] expectNotification) throws ConnectException, InterruptedException {
        Semaphore responsewaiter = new Semaphore(0);
        Semaphore notificationwaiter = new Semaphore(0);

        req = new FileCreateRequest(file.getFilename(), file.getRelativePath(), proj.getProjectID(), data.getBytes()).getRequest(response -> {
            Assert.assertEquals("Failed to create file", 200, response.getStatus());

            file.setFileID(((FileCreateResponse) response.getData()).getFileID());

            responsewaiter.release(1 + expectNotification.length);
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

                System.out.println(file.getFileID());

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
        if (!responsewaiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
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

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new FileCreateRequest(file2.getFilename(), file2.getRelativePath(), proj2.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Check invalid projectID
         */

        req = new FileCreateRequest("dummyFileName", "dummyFilePath", -1, "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid projectID", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Check missing write permissions
         */

        req = new FileCreateRequest("dummyFileName", "dummyFilePath", proj1.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new FileCreateRequest("dummyFileName", "dummyFilePath", proj2.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Check duplicate file
         */
       req = new FileCreateRequest(file1.getFilename(), file1.getRelativePath(), proj1.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
           Assert.assertNotEquals("Failed to throw error on duplicate file", 200, response.getStatus());

           waiter.release();
       }, errHandler);

       ws1.sendRequest(req);
       if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
           Assert.fail("Acquire timed out");
       }

       req = new FileCreateRequest(file2.getFilename(), file2.getRelativePath(), proj2.getProjectID(), "dummyData".getBytes()).getRequest(response -> {
           Assert.assertNotEquals("Failed to throw error on duplicate file", 200, response.getStatus());

           waiter.release();
       }, errHandler);

       ws2.sendRequest(req);
       if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
           Assert.fail("Acquire timed out");
       }
    }

    private void testFilePull() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        String[] changes1 = new String[(int) file1.getFileVersion() - 1];
        for (int i = 0; i < changes1.length; i++) {
            changes1[i] = generateDummyPatch(i + 1);
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
            changes2[i] = generateDummyPatch(i + 1);
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

            wsMgr.deregisterNotificationHandler("File", "Change");
            waiter.release();
        });
    }

    private void testFileChange(WSManager wsMgr, File file, WSManager[] expectNotification) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[]{generateDummyPatch(file.getFileVersion())};
        req = new FileChangeRequest(file.getFileID(), changes).getRequest(response -> {
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
        String[] changes = new String[]{generateDummyPatch(1)};
        req = new FileChangeRequest(file1.getFileID(), changes).getRequest(response -> {
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
        req = new FileChangeRequest(-1, changes).getRequest(response -> {
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
        req = new FileChangeRequest(file2.getFileID(), changes).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        /*
         * Test file version too high
         */
    }

    private void testFileMove(WSManager wsMgr, File file, String newPath, WSManager[] expectNotification) throws InterruptedException, ConnectException {
        String newFilePath = Paths.get(file.getRelativePath(), newPath).toString().replace('\\', '/');

        Semaphore waiter = new Semaphore(0);

        req = new FileMoveRequest(file.getFileID(), newFilePath).getRequest(response -> {
            Assert.assertEquals("Failed to move file", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {
            otherWSMgr.registerNotificationHandler("File", "Move", notification -> { // Create notification handler
                Assert.assertEquals("FileMoveNotification gave wrong file ID", file.getFileID(), notification.getResourceID());
                Assert.assertEquals("FileMoveNotification gave wrong path", newFilePath, ((FileMoveNotification) notification.getData()).newPath);

                otherWSMgr.deregisterNotificationHandler("File", "Move");
                waiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
        file.setRelativePath(newFilePath);
    }

    private void testFileMoveInvalid() throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        // Test Authentication
        req = new FileMoveRequest(file1.getFileID(), file1.getRelativePath() + "/test").getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test Permissions
        req = new FileMoveRequest(file2.getFileID(), file2.getRelativePath() + "/test").getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test Permissions
        req = new FileMoveRequest(file1.getFileID(), "../test").getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on invalid newDir", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileRename(WSManager wsMgr, File file, String name_postfix, WSManager[] expectNotification) throws InterruptedException, ConnectException {
        String newName = file.getFilename() + name_postfix;

        Semaphore waiter = new Semaphore(0);

        req = new FileRenameRequest(file.getFileID(), newName).getRequest(response -> {
            Assert.assertEquals("Failed to rename file", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {
            otherWSMgr.registerNotificationHandler("File", "Rename", notification -> { // Create notification handler
                Assert.assertEquals("FileRenameNotification gave wrong file ID", file.getFileID(), notification.getResourceID());
                Assert.assertEquals("FileRenameNotification gave wrong new name", newName, ((FileRenameNotification) notification.getData()).newName);

                otherWSMgr.deregisterNotificationHandler("File", "Rename");
                waiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        file.setFilename(newName);
    }

    private void testProjectRevokePermission(WSManager wsMgr, Project proj, String revokeUsername, WSManager[] expectNotification) throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectRevokePermissionsRequest(proj.getProjectID(), revokeUsername).getRequest(response -> {
            Assert.assertEquals("Failed to revoke permissions on " + proj.getName() + " for " + revokeUsername, 200, response.getStatus());
            waiter.release();
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {
            otherWSMgr.registerNotificationHandler("Project", "RevokePermissions", notification -> { // Create notification handler
                Assert.assertEquals("ProjectRevokePermissionNotification gave wrong project ID", proj1.getProjectID(), notification.getResourceID());
                Assert.assertEquals("ProjectRevokePermissionNotification gave wrong username", revokeUsername, ((ProjectRevokePermissionsNotification) notification.getData()).revokeUsername);

                otherWSMgr.deregisterNotificationHandler("Project", "RevokePermissions");
                waiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    /**
     * Tests the failure cases of the revoke permission - should be called before the actual revoke test
     */
    private void testProjectRevokePermissionInvalid() throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        // Test Authentication
        req = new ProjectRevokePermissionsRequest(proj1.getProjectID(), user2ID).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test Permissions
        req = new ProjectRevokePermissionsRequest(proj2.getProjectID(), user1ID).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectRevokePermissionsRequest(proj1.getProjectID(), user1ID).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on attempting to revoke owner", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectRevokePermissionsRequest(proj2.getProjectID(), user2ID).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on attempting to revoke owner", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test invalid projectID
        req = new ProjectRevokePermissionsRequest(-1, user2ID).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid project ID", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test invalid username
        req = new ProjectRevokePermissionsRequest(proj1.getProjectID(), "dummyUser").getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid project ID", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testFileDelete(WSManager wsMgr, File file, WSManager[] expectNotification) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new FileDeleteRequest(file.getFileID()).getRequest(response -> {
            Assert.assertEquals("Failed to delete file", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {
            otherWSMgr.registerNotificationHandler("File", "Delete", notification -> { // Create notification handler
                Assert.assertEquals("FileDeleteNotification gave wrong file ID", file.getFileID(), notification.getResourceID());

                wsMgr.deregisterNotificationHandler("File", "Delete");
                waiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        file.setFileID(-1);
    }

    /**
     * Tests the failure cases of the revoke permission - should be called before the actual revoke test
     */
    private void testFileDeleteInvalid() throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        // Test Authentication
        req = new FileDeleteRequest(file1.getFileID()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test Permissions
        req = new FileDeleteRequest(file2.getFileID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test invalid fileID
        req = new FileDeleteRequest(-1).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid file ID", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectUnsubscribe(WSManager wsMgr, Project proj) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectUnsubscribeRequest(proj.getProjectID()).getRequest(response -> {
            Assert.assertEquals("Failed to unsubscribe from project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);

        // Wait till subscription is done before doing anything else.
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectDelete(WSManager wsMgr, Project proj, WSManager[] expectNotification) throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        req = new ProjectDeleteRequest(proj.getProjectID()).getRequest(response -> {
            Assert.assertEquals("Failed to delete project", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        for (WSManager otherWSMgr : expectNotification) {
            otherWSMgr.registerNotificationHandler("Project", "Delete", notification -> { // Create notification handler
                Assert.assertEquals("ProjectDeleteNotification gave wrong project ID", proj.getProjectID(), notification.getResourceID());

                otherWSMgr.deregisterNotificationHandler("Project", "Delete");
                waiter.release();
            });
        }

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(1 + expectNotification.length, 5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        proj.setProjectID(-1);
    }


    /**
     * Tests the failure cases of the project delete request - should be called before the actual delete test
     */
    private void testProjectDeleteInvalid() throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        // Test Authentication
        req = new ProjectDeleteRequest(proj1.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test Permissions
        req = new ProjectDeleteRequest(proj2.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        req = new ProjectDeleteRequest(proj1.getProjectID()).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of write permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws2.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test invalid fileID
        req = new ProjectDeleteRequest(-1).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid project ID", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testProjectLookup() throws ConnectException, InterruptedException {
        testProjectLookup(ws1, user1ID, proj1);
        testProjectLookup(ws2, user2ID, proj2);
    }

    private void testProjectLookup(WSManager wsMgr, String ownerID, Project proj) throws ConnectException, InterruptedException {
        Semaphore waiter = new Semaphore(0);

        Byte permByte = apiConstants.get("owner");

        req = new ProjectLookupRequest(new Long[]{proj.getProjectID()}).getRequest(response -> {
            Assert.assertEquals("Failed to lookup project", 200, response.getStatus());

            Assert.assertEquals("Incorrect number of projects returned", 1, ((ProjectLookupResponse) response.getData()).getProjects().length);
            Assert.assertEquals("Incorrect ProjectID returned", proj.getProjectID(), ((ProjectLookupResponse) response.getData()).getProjects()[0].getProjectID());
            Assert.assertEquals("Incorrect project name returned", proj.getName(), ((ProjectLookupResponse) response.getData()).getProjects()[0].getName());
            Assert.assertEquals("Incorrect project permissions name for owner returned", ownerID, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(ownerID).getUsername());
            Assert.assertEquals("Incorrect project permissions level for owner returned", (int) permByte, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(ownerID).getPermissionLevel());
            Assert.assertEquals("Incorrect project permissions granted by field for owner returned", ownerID, ((ProjectLookupResponse) response.getData()).getProjects()[0].getPermissions().get(ownerID).getGrantedBy());

            waiter.release();
        }, errHandler);

        wsMgr.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }


    /**
     * Tests the failure cases of the project delete request - should be called before the actual delete test
     */
    private void testProjectLookupInvalid() throws InterruptedException {
        Semaphore waiter = new Semaphore(0);

        // Test Authentication
        req = new ProjectLookupRequest(new Long[]{proj1.getProjectID()}).getRequest(response -> {
            Assert.assertNotEquals("Authenticated method succeeded with no auth info", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws3.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test Permissions
        req = new ProjectLookupRequest(new Long[]{proj2.getProjectID()}).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw permissions error on lack of read permissions", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }

        // Test invalid fileID
        req = new ProjectLookupRequest(new Long[]{-1L}).getRequest(response -> {
            Assert.assertNotEquals("Failed to throw error on invalid project ID", 200, response.getStatus());

            waiter.release();
        }, errHandler);

        ws1.sendRequest(req);
        if (!waiter.tryAcquire(5, TimeUnit.SECONDS)) {
            Assert.fail("Acquire timed out");
        }
    }

    private void testUserDelete(WSManager wsMgr, String userID, String userPass) throws InterruptedException, ConnectException {
        Semaphore waiter = new Semaphore(0);
        Semaphore loginWaiter = new Semaphore(0);

        req = new UserDeleteRequest().getRequest(deleteResponse -> {
            Assert.assertEquals("Failed to delete user", 200, deleteResponse.getStatus());

            // verifying user was deleted
            req = new UserLoginRequest(userID, userPass).getRequest(loginResponse -> {
                Assert.assertNotEquals("Failed to throw error when logging in", 200, loginResponse.getStatus());
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

    private String generateDummyPatch(long baseVersion) {
        int versionNumChars = Long.toString(baseVersion).length();

        return String.format("v%d:\n%d:+%d:newData%d", baseVersion, baseVersion, 7 + versionNumChars, baseVersion);
    }

}
