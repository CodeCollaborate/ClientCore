package integration;

import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.WSManager;
import clientcore.websocket.models.ConnectionConfig;
import clientcore.websocket.models.requests.UserDeleteRequest;
import clientcore.websocket.models.requests.UserLoginRequest;
import clientcore.websocket.models.responses.UserLoginResponse;

import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Logger;

/**
 * Created by fahslaj on 12/11/2016.
 */
public class UserBasedIntegrationTest {

    protected static final String SERVER_URL = "ws://cody.csse.rose-hulman.edu:8000/ws/";

    void cleanupUser(Logger logger, String userID, String userPass, Semaphore waiter, IRequestSendErrorHandler errHandler) {
        logger.info("Cleaning up user " + userID);
        WSManager wsMgr = new WSManager(new ConnectionConfig(SERVER_URL, false, 5));

        wsMgr.sendRequest(new UserLoginRequest(userID, userPass).getRequest(response -> {
            if (response.getStatus() == 200) {
                String senderToken = ((UserLoginResponse) response.getData()).token;
                wsMgr.setAuthInfo(userID, senderToken);

                wsMgr.sendRequest(new UserDeleteRequest().getRequest(null, null));
            }
            waiter.release();
        }, errHandler));
    }
}
