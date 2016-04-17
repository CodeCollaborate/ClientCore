package websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websocket.models.*;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;

/**
 * Created by fahslaj on 4/14/2016.
 */
public class WSManager implements IMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger("websocket");
    // HashMap for keeping track of sent requests (Tag -> Request)
    HashMap<Long, Request> requestHashMap;
    // HashMap for registered notification handlers (Resource.Method -> Handler)
    private HashMap<String, INotificationHandler> notificationHandlerHashMap;
    // Jackson Mapper
    private ObjectMapper mapper = new ObjectMapper();

    // WebSocket connection
    private WSConnection socket;

    public WSManager(ConnectionConfig config) {
        this.notificationHandlerHashMap = new HashMap<>();
        this.requestHashMap = new HashMap<>();
        this.socket = new WSConnection(config);
    }

    // used for testing
    WSManager(WSConnection socket) {
        this.notificationHandlerHashMap = new HashMap<>();
        this.requestHashMap = new HashMap<>();
        this.socket = socket;
    }

    /**
     * Register an INotificationHandler to receive notifications from the server.
     *
     * @param type
     * @param handler
     */
    public void registerNotificationHandler(String type, INotificationHandler handler) {
        notificationHandlerHashMap.put(type, handler);
    }

    /**
     * Deregister the INotificationHandler saved for the given type of notification.
     *
     * @param type
     */
    public void deregisterNotificationHandler(String type) {
        notificationHandlerHashMap.remove(type);
    }

    /**
     * Send a request over the WebSocket connection.
     *
     * @param request
     */
    public void sendRequest(Request request) throws ConnectException {
        if (socket.getState() == WSConnection.State.CREATED || socket.getState() == WSConnection.State.ERROR) {
            try {
                socket.connect();
            } catch (Exception e) {
                logger.error("WebSocket connection could not connect.");
                throw new ConnectException("Could not connect to WebSocket.");
            }
        }
        String messageText;
        try {
            messageText = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            logger.error("Could not map request to Json string: " + request);
            return;
        }
        socket.enqueueMessage(messageText);
    }

    @Override
    public void handleMessage(String message) {
        ServerMessageWrapper wobject;
        try {
            wobject = mapper.readValue(message, ServerMessageWrapper.class);
        } catch (IOException e) {
            logger.error("Malformed message from server: " + message);
            return;
        }
        switch (wobject.getType()) {
            case ServerMessageWrapper.TYPE_NOTIFICATION:
                handleNotification(wobject);
                break;
            case ServerMessageWrapper.TYPE_RESPONSE:
                handleResponse(wobject);
                break;
        }
    }

    private void handleNotification(ServerMessageWrapper wobject) {
        Notification an;
        try {
            an = mapper.convertValue(wobject.getMessageJson(), Notification.class);
        } catch (IllegalArgumentException e) {
            String notificationMessage = wobject.getMessageJson().toString();
            logger.error("Malformed notification from server: " + notificationMessage);
            return;
        }

        String key = an.getResource() + '.' + an.getMethod();
        INotificationHandler handler = notificationHandlerHashMap.get(key);
        if (handler == null) {
            String notificationMessage = wobject.getMessageJson().toString();
            logger.warn("No handler registered for notification: " + notificationMessage);
            return;
        }
        handler.handleNotification(an);
    }

    private void handleResponse(ServerMessageWrapper wobject) {
        Response resp;
        try {
            resp = mapper.convertValue(wobject.getMessageJson(), Response.class);
        } catch (IllegalArgumentException e) {
            String responseMessage = wobject.getMessageJson().toString();
            logger.error("Malformed response from server: " + responseMessage);
            return;
        }
        long tag = resp.getTag();
        Request request = requestHashMap.get(tag);
        if (request == null) {
            String responseMessage = wobject.getMessageJson().toString();
            logger.warn("Received extraneous response from server: " + responseMessage);
            return;
        }
        IResponseHandler handler = request.getResponseHandler();
        if (handler == null) {
            String responseMessage = wobject.getMessageJson().toString();
            logger.warn("No handler specified for request: " + responseMessage);
            return;
        }
        handler.handleResponse(resp);
    }

    @Override
    public void handleMessageSendError(String message) {
        Request request;
        try {
            request = mapper.readValue(message, Request.class);
        } catch (IOException e) {
            logger.error("Request that failed to send was malformed");
            return;
        }
        request = requestHashMap.get(request.getTag());
        IRequestSendErrorHandler handler = request.getErrorHandler();
        handler.handleRequestSendError();
    }
}
