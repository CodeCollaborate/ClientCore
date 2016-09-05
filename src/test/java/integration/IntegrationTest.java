package integration;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.IRequestData;
import websocket.models.Request;
import websocket.models.notifications.*;
import websocket.models.requests.*;
import websocket.models.responses.FileChangeResponse;
import websocket.models.responses.FileCreateResponse;
import websocket.models.responses.ProjectCreateResponse;
import websocket.models.responses.UserLoginResponse;

import java.net.ConnectException;
import java.util.concurrent.Semaphore;

/**
 * Created by Benedict on 7/25/2016.
 */
public class IntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger("integrationTest");

    private static final String userID = "testUser";
    private static final String userPass = "testPass";

    private static final String projectName = "_testProject";
    private static final String filePath = "test/file/path";
    private static final String fileName = "_testFile";
    private static final String fileData = "test data1\ntest data 2";
    private static final WSManager wsMgr = new WSManager(new ConnectionConfig("ws://localhost:8000/ws/", false, 5));
    private static final IRequestSendErrorHandler errHandler = new IRequestSendErrorHandler() {
        @Override
        public void handleRequestSendError() {
            System.out.println("Failed to send");
        }
    };
    private static int fileVersion = 1;
    private static int projectID = -1;
    private static int fileID = -1;
    private Request req;
    private IRequestData data;

    @Test
    public void integrationTest() throws Exception {
        // Test valid flow
        testLogin();
        testCreateProject();
        testSubscribeToProject();
        testRenameProject();
        testCreateFile();
        testChangeFile();
        testMoveFile();
        testChangeFile();
        testRenameFile();
        testChangeFile();
        testDeleteFile();
        testUnsubscribeFromProject();
        testSubscribeToProject();
        testDeleteProject();

        // TODO(wongb): Test Project.GrantPermissions and Project.RevokePermissions


        // Run invalid method type, expect error
        // Re-run login, expect error(?)
        // Run invalid login, expect error

        Thread.sleep(1000);
    }

    private void testRegister() throws InterruptedException, ConnectException {
        logger.info(String.format("\nRegistering user"));
        Semaphore waiter = new Semaphore(0);

        data = new UserRegisterRequest(userID, "testFirstName", "testLastName", "testEmail@testDomain.com", userPass);
        req = new Request("User", "Register", data, response -> {
            Assert.assertEquals("Failed to register", 200, response.getStatus());

            wsMgr.setAuthInfo(userID, ((UserLoginResponse) response.getData()).getToken());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        waiter.acquire();
    }

    private void testLogin() throws InterruptedException, ConnectException {
        logger.info(String.format("\nLogging in"));
        Semaphore waiter = new Semaphore(0);

        data = new UserLoginRequest(userID, userPass);
        req = new Request("User", "Login", data, response -> {
            // TODO(wongb) Add login logic for server
            if (response.getStatus() != 200){
                try {
                    testRegister();
                    testLogin();
                } catch (InterruptedException | ConnectException e) {
                    Assert.fail("Failed to register and login");
                }
                waiter.release();
                return;
            }
            Assert.assertEquals("Failed to log in", 200, response.getStatus());

            wsMgr.setAuthInfo(userID, ((UserLoginResponse) response.getData()).getToken());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        waiter.acquire();
    }

    private void testCreateProject() throws ConnectException, InterruptedException {
        logger.info(String.format("\nCreating project with name %s", projectName));
        Semaphore waiter = new Semaphore(0);

        data = new ProjectCreateRequest(projectName);
        req = new Request("Project", "Create", data, response -> {
            Assert.assertEquals("Failed to create project", 200, response.getStatus());

            projectID = ((ProjectCreateResponse) response.getData()).getProjectID();

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        waiter.acquire();
    }

    private void testSubscribeToProject() throws InterruptedException, ConnectException {
        logger.info(String.format("\nSubscribing to project with id %d", projectID));
        Semaphore waiter = new Semaphore(0);

        data = new ProjectSubscribeRequest(projectID);
        req = new Request("Project", "Subscribe", data, response -> {
            Assert.assertEquals("Failed to subscribe to project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);
        waiter.acquire();
    }

    private void testRenameProject() throws ConnectException, InterruptedException {
        logger.info(String.format("\nRenaming project with id %d to %s", projectID, projectName + "_Renamed"));
        Semaphore waiter = new Semaphore(0);

        data = new ProjectRenameRequest(projectID, projectName + "_Renamed");
        req = new Request("Project", "Rename", data, response -> {
            Assert.assertEquals("Failed to rename project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("Project", "Rename", notification -> { // Create notification handler
            Assert.assertEquals("ProjectRenameNotification gave wrong project ID", projectID, notification.getResourceID());
            Assert.assertEquals("ProjectRenameNotification gave wrong value", projectName + "_Renamed", ((ProjectRenameNotification) notification.getData()).newName);

            wsMgr.deregisterNotificationHandler("Project", "Rename");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        waiter.acquire(2);
    }

    private void testCreateFile() throws ConnectException, InterruptedException {
        logger.info(String.format("\nCreating file %s in project with id %d", filePath + "/" + fileName, projectID));
        Semaphore waiter = new Semaphore(0);

        data = new FileCreateRequest(fileName, filePath, projectID, fileData.getBytes());
        req = new Request("File", "Create", data, response -> {
            Assert.assertEquals("Failed to create file", 200, response.getStatus());

            fileID = ((FileCreateResponse) response.getData()).getFileID();

            waiter.release(2);
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Create", notification -> { // Create notification handler
            try {
                waiter.acquire();
            } catch (InterruptedException e) {
                // do nothing
            }

            Assert.assertEquals("FileCreateNotification gave wrong project ID", projectID, notification.getResourceID());
            Assert.assertEquals("FileCreateNotification gave wrong file name", fileName, ((FileCreateNotification) notification.getData()).file.getFileName());
            Assert.assertEquals("FileCreateNotification gave wrong file path", filePath, ((FileCreateNotification) notification.getData()).file.getRelativePath());
            Assert.assertEquals("FileCreateNotification gave wrong file ID", fileID, ((FileCreateNotification) notification.getData()).file.getFileID());
            Assert.assertEquals("FileCreateNotification gave wrong file version", fileVersion, ((FileCreateNotification) notification.getData()).file.getFileVersion());

            wsMgr.deregisterNotificationHandler("File", "Create");
            waiter.release(2);
        });

        wsMgr.sendRequest(req);
        waiter.acquire(3);
    }

    private void testMoveFile() throws InterruptedException, ConnectException {
        logger.info(String.format("\nMoving file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        data = new FileMoveRequest(fileID, filePath + "/new");
        req = new Request("File", "Move", data, response -> {
            Assert.assertEquals("Failed to move file", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Move", notification -> { // Create notification handler
            Assert.assertEquals("FileMoveNotification gave wrong file ID", fileID, notification.getResourceID());
            Assert.assertEquals("FileMoveNotification gave wrong changes", filePath + "/new", ((FileMoveNotification) notification.getData()).newPath);

            wsMgr.deregisterNotificationHandler("File", "Move");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        waiter.acquire(2);
    }

    private void testRenameFile() throws InterruptedException, ConnectException {
        logger.info(String.format("\nRenaming file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        data = new FileRenameRequest(fileID, fileName + "_new");
        req = new Request("File", "Rename", data, response -> {
            Assert.assertEquals("Failed to rename file", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Rename", notification -> { // Create notification handler
            Assert.assertEquals("FileRenameNotification gave wrong file ID", fileID, notification.getResourceID());
            Assert.assertEquals("FileRenameNotification gave wrong new name", fileName + "_new", ((FileRenameNotification) notification.getData()).newName);

            wsMgr.deregisterNotificationHandler("File", "Rename");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        waiter.acquire(2);
    }

    private void testChangeFile() throws InterruptedException, ConnectException {
        logger.info(String.format("\nChanging file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        String[] changes = new String[]{"+5:6:newData"};
        data = new FileChangeRequest(fileID, changes, fileVersion);
        req = new Request("File", "Change", data, response -> {
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
        waiter.acquire(2);
        fileVersion++;
    }

    private void testDeleteFile() throws InterruptedException, ConnectException {
        logger.info(String.format("\nDeleting file %d", fileID));
        Semaphore waiter = new Semaphore(0);

        data = new FileDeleteRequest(fileID);
        req = new Request("File", "Delete", data, response -> {
            Assert.assertEquals("Failed to delete file", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("File", "Delete", notification -> { // Create notification handler
            Assert.assertEquals("FileDeleteNotification gave wrong file ID", fileID, notification.getResourceID());

            wsMgr.deregisterNotificationHandler("File", "Delete");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        waiter.acquire(2);
    }

    private void testUnsubscribeFromProject() throws InterruptedException, ConnectException {
        logger.info(String.format("\nUnsubscribing from project with id %d", projectID));
        Semaphore waiter = new Semaphore(0);

        data = new ProjectUnsubscribeRequest(projectID);
        req = new Request("Project", "Unsubscribe", data, response -> {
            Assert.assertEquals("Failed to unsubscribe from project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.sendRequest(req);

        // Wait till subscription is done before doing anything else.
        waiter.acquire();
    }

    private void testDeleteProject() throws ConnectException, InterruptedException {
        logger.info(String.format("\nDeleting project with ID %d", projectID));
        Semaphore waiter = new Semaphore(0);

        data = new ProjectDeleteRequest(projectID);
        req = new Request("Project", "Delete", data, response -> {
            Assert.assertEquals("Failed to delete project", 200, response.getStatus());

            waiter.release();
        }, errHandler);
        wsMgr.registerNotificationHandler("Project", "Delete", notification -> { // Create notification handler
            Assert.assertEquals("ProjectDeleteNotification gave wrong project ID", projectID, notification.getResourceID());

            wsMgr.deregisterNotificationHandler("Project", "Delete");
            waiter.release();
        });

        wsMgr.sendRequest(req);
        waiter.acquire(2);
    }
}
