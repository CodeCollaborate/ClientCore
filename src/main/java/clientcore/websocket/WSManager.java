package clientcore.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import clientcore.websocket.models.ConnectionConfig;
import clientcore.websocket.models.Notification;
import clientcore.websocket.models.Request;
import clientcore.websocket.models.Response;
import clientcore.websocket.models.ServerMessageWrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by fahslaj on 4/14/2016.
 */
public class WSManager implements IMessageHandler {
    public static Logger logger = LogManager.getLogger("clientcore/websocket");
    private OutputStream loggerOutputStream;
    // HashMap for keeping track of sent requests (Tag -> Request)
    HashMap<Long, Request> requestHashMap;
    // HashMap for registered notification handlers (Resource.Method -> Handler)
    HashMap<String, INotificationHandler> notificationHandlerHashMap;
    // WebSocket connection
    WSConnection socket;
    private String userID;
    private String userToken;
    // Jackson Mapper
    private ObjectMapper mapper = new ObjectMapper();
    // queued requests that require authentication
    private final List<Request> queuedAuthenticatedRequests;

    public WSManager(ConnectionConfig config) {
        this(new WSConnection(config));
    }

    // used for testing
    WSManager(WSConnection socket) {
        this.notificationHandlerHashMap = new HashMap<>();
        this.requestHashMap = new HashMap<>();
        this.queuedAuthenticatedRequests = new ArrayList<>();
        this.socket = socket;

        socket.registerIncomingMessageHandler(this);

        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public void connect() throws ConnectException {
        try {
            socket.connect();
        } catch (Exception e) {
            logger.error("WebSocket connection could not connect: " + e.getMessage());
            throw new ConnectException("Could not connect to WebSocket.", e);
        }
    }

    public void close() {
        socket.close();
    }

    public void registerEventHandler(WSConnection.EventType event, Runnable handler) {
        List<Runnable> runnables = socket.eventHandlers.get(event);
        if (runnables != null) {
            runnables.add(handler);
        } else {
            runnables = new ArrayList<>();
            runnables.add(handler);
            socket.eventHandlers.put(event, runnables);
        }
    }

    public void deregisterEventHandler(WSConnection.EventType event) {
        socket.eventHandlers.remove(event);
    }

    /**
     * Register an INotificationHandler to receive notifications from the server.
     *
     * @param resource
     * @param method
     * @param handler
     */
    public void registerNotificationHandler(String resource, String method, INotificationHandler handler) {
        notificationHandlerHashMap.put(resource + '.' + method, handler);
    }

    /**
     * Deregister the INotificationHandler saved for the given type of notification.
     *
     * @param resource
     * @param method
     */
    public void deregisterNotificationHandler(String resource, String method) {
        notificationHandlerHashMap.remove(resource + '.' + method);
    }

    public void sendAuthenticatedRequest(Request request) throws ConnectException {
        if (userID == null || userToken == null) {
            synchronized (this.queuedAuthenticatedRequests) {
                this.queuedAuthenticatedRequests.add(request);
            }
            return;
        }
        this.sendRequest(request);
    }

    /**
     * Send a request over the WebSocket connection.
     *
     * @param request
     */
    public void sendRequest(Request request) throws ConnectException {
        sendRequest(request, 0);
    }

    public void sendRequest(Request request, int priority) throws ConnectException {
        if (socket.getState() == WSConnection.State.CREATED || socket.getState() == WSConnection.State.ERROR) {
            try {
                socket.connect();
            } catch (Exception e) {
                logger.error("WebSocket connection could not connect: " + e.getMessage());
                throw new ConnectException("Could not connect to WebSocket.", e);
            }
        }

        // Set authentication information, if available.
        if (userID != null) {
            request.setSenderId(userID);
            request.setSenderToken(userToken);
        }

        String messageText;
        try {
            messageText = mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            logger.error("Could not map request to Json string: " + request);
            return;
        }
        socket.enqueueMessage(messageText, priority);
        requestHashMap.put(request.getTag(), request);
    }

    @Override
    public void handleMessage(String message) {
        ServerMessageWrapper wrapper;
        try {
            wrapper = mapper.readValue(message, ServerMessageWrapper.class);
        } catch (IOException e) {
            logger.error("Malformed message from server: " + message);
            return;
        }
        switch (wrapper.getType()) {
            case ServerMessageWrapper.TYPE_NOTIFICATION:
                handleNotification(wrapper);
                break;
            case ServerMessageWrapper.TYPE_RESPONSE:
                handleResponse(wrapper);
                break;
        }
    }

    private void handleNotification(ServerMessageWrapper wrapper) {
        Notification notif;
        try {
            notif = mapper.convertValue(wrapper.getMessageJson(), Notification.class);
        } catch (IllegalArgumentException e) {
            String notificationMessage = wrapper.getMessageJson().toString();
            logger.error(String.format("Malformed notification from server: %s", notificationMessage));
            return;
        }

        // parse body of notification
        try {
            notif.parseData();
        } catch (JsonProcessingException e) {
            String notificationData = notif.getJsonData().toString();
            logger.error(String.format("Malformed notification data from server: %s", notificationData));
            return;
        } catch (ClassNotFoundException e) {
            logger.error("Notification data class not found");
            return;
        }

        String key = notif.getResource() + '.' + notif.getMethod();
        INotificationHandler handler = notificationHandlerHashMap.get(key);
        if (handler == null) {
            String notificationMessage = wrapper.getMessageJson().toString();
            logger.warn("No handler registered for notification: " + notificationMessage);
            return;
        }
        try {
            handler.handleNotification(notif);
        } catch (Exception e) {
            logger.error(String.format("Notification handler for %s.%s threw an exception", notif.getResource(), notif.getMethod()), e);
        }
    }

    private void handleResponse(ServerMessageWrapper wrapper) {
        Response resp;
        try {
            resp = mapper.convertValue(wrapper.getMessageJson(), Response.class);
        } catch (IllegalArgumentException e) {
            String responseMessage = wrapper.getMessageJson().toString();
            logger.error(String.format("Malformed response from server: %s", responseMessage));
            return;
        }
        long tag = resp.getTag();
        Request request = requestHashMap.get(tag);
        if (request == null) {
            String responseMessage = wrapper.getMessageJson().toString();
            logger.warn("Received extraneous response from server: " + responseMessage);
            return;
        }

        // parse body of response based on request type
        if (request.data != null) {
            try {
                resp.parseData(request.data.getClass());
            } catch (JsonProcessingException e) {
                String responseData = resp.getJsonData().toString();
                logger.error(String.format("Malformed response data from server: %s", responseData));
                return;
            } catch (ClassNotFoundException e) {
                logger.error("Response data class not found");
                return;
            }
        }

        IResponseHandler handler = request.getResponseHandler();
        if (handler == null) {
            String responseMessage = wrapper.getMessageJson().toString();
            logger.warn("No handler specified for request: " + responseMessage);
            return;
        }

        try {
            handler.handleResponse(resp);
        } catch (Exception e) {
            logger.error(String.format("Response handler for %s.%s threw an exception", request.resource, request.method), e);
        }
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

    public void setAuthInfo(String userID, String userToken) {
        this.userID = userID;
        this.userToken = userToken;
        this.sendAllAuthenticatedRequests();
    }

    private void sendAllAuthenticatedRequests() {
        synchronized (this.queuedAuthenticatedRequests) {
            List<Request> reqList = new ArrayList<>(this.queuedAuthenticatedRequests);
            this.queuedAuthenticatedRequests.clear();
            reqList.forEach(this::sendAuthenticatedRequest);
        }
    }

    public WSConnection.State getConnectionState() {
        return socket.getState();
    }

    public OutputStream getLoggingOutputStream() {
        return loggerOutputStream;
    }

    private class BatchingControl {
        final Semaphore batchingSem = new Semaphore(1);
        private final ArrayList<String> patchBatchingQueue = new ArrayList<>();
        String[] lastResponsePatches = new String[0];
        long maxVersionSeen = -1;
    }
}
