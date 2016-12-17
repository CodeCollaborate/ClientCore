package integration;

import org.slf4j.Logger;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.requests.UserDeleteRequest;
import websocket.models.requests.UserLoginRequest;
import websocket.models.responses.UserLoginResponse;

import java.util.concurrent.Semaphore;

/**
 * Created by fahslaj on 12/11/2016.
 */
public class UserBasedIntegrationTest {

    protected static final String SERVER_URL = "ws://localhost:8000/ws/";

    void cleanupUser(Logger logger, String userID, String userPass, Semaphore waiter, IRequestSendErrorHandler errHandler) {
        logger.info("Cleaning up user " + userID);
        WSManager wsMgr = new WSManager(new ConnectionConfig(SERVER_URL, false, 5));

        wsMgr.sendRequest(new UserLoginRequest(userID, userPass).getRequest(response -> {
            if (response.getStatus() == 200) {
                String senderToken = ((UserLoginResponse) response.getData()).getToken();
                wsMgr.setAuthInfo(userID, senderToken);

                wsMgr.sendRequest(new UserDeleteRequest().getRequest(null, null));
            }
            waiter.release();
        }, errHandler));
    }
}
