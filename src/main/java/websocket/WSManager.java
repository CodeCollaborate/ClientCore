package websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import patching.Patch;
import websocket.models.ConnectionConfig;
import websocket.models.Notification;
import websocket.models.Request;
import websocket.models.Response;
import websocket.models.ServerMessageWrapper;
import websocket.models.requests.FileChangeRequest;
import websocket.models.responses.FileChangeResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by fahslaj on 4/14/2016.
 */
public class WSManager implements IMessageHandler {
    static Logger logger = LoggerFactory.getLogger("websocket");
    final HashMap<Long, BatchingControl> batchingByFile = new HashMap<>();
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

        // Batch FileChangeRequests
        if (request.data instanceof FileChangeRequest) {
            // Add to batching queue
            FileChangeRequest data = (FileChangeRequest) request.data;

            if (!batchingByFile.containsKey(data.getFileID())) {
                BatchingControl batchingControl = new BatchingControl();
                batchingByFile.put(data.getFileID(), batchingControl);
                batchingControl.maxVersionSeen = data.getBaseFileVersion();
            }
            BatchingControl batchingCtrl = batchingByFile.get(data.getFileID());

            synchronized (batchingCtrl.patchBatchingQueue) {
                logger.debug(String.format("Adding %s to batching queue; batching queue currently %s", Arrays.toString(((FileChangeRequest) request.data).getChanges()), batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
                batchingCtrl.patchBatchingQueue.addAll(Arrays.asList(((FileChangeRequest) request.data).getChanges()));
            }

            // Create releaser to make sure it only is ever done once
            Runnable releaser = new Runnable() {
                boolean released = false;
                final Object synchronizationObj = new Object();
                IResponseHandler respHandler = request.getResponseHandler();

                @Override
                public void run() {
                    synchronized (synchronizationObj) {
                        if (!released) {
                            batchingCtrl.batchingSem.drainPermits();
                            batchingCtrl.batchingSem.release();
                            released = true;
                        }
                    }

                    // Immediately send a request if queue non-empty
                    boolean isEmpty;
                    synchronized (batchingCtrl.patchBatchingQueue) {
                        isEmpty = batchingCtrl.patchBatchingQueue.isEmpty();
                    }
                    if (!isEmpty) {
                        logger.debug(String.format("Triggering new fileChangeRequest; current batching queue is:  %s", batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
                        sendRequest(new FileChangeRequest(data.getFileID(), new String[]{}, batchingCtrl.maxVersionSeen).getRequest(
                                respHandler, request.getErrorHandler()
                        ));
                    }
                }
            };

            Timer timer = new Timer();

            if (batchingCtrl.batchingSem.tryAcquire()) {
                String[] patches;

                // Send request
                synchronized (batchingCtrl.patchBatchingQueue) {
                    patches = batchingCtrl.patchBatchingQueue.toArray(new String[batchingCtrl.patchBatchingQueue.size()]);
                    logger.debug(String.format("Sending patches %s", batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");

                }

                // Transform patches against missing patches before sending
                for (int i = 0; i < patches.length; i++) {
                    Patch patch = new Patch(patches[i]);

                    for (int j = 0; j < batchingCtrl.lastResponsePatches.length; j++) {
                        Patch pastPatch = new Patch(batchingCtrl.lastResponsePatches[j]);

                        // If the new patch's base version is earlier or equal, we need to update it.
                        // If the base versions are the same, the server patch wins.
                        if (patch.getBaseVersion() <= pastPatch.getBaseVersion()) {
                            logger.debug(String.format("FileChange: Transforming %s against missing patch %s", patch.toString(), pastPatch.toString()).replace("\n", "\\n") + "\n");
                            patch = patch.transform(pastPatch);
                        }
                    }

                    // If the patch's base version is greater than the maxVersionSeen, we leave it's version as is; it has been built on a newer version than we can handle here.
                    if (patch.getBaseVersion() < batchingCtrl.maxVersionSeen) {
                        patch.setBaseVersion(batchingCtrl.maxVersionSeen); // Set this to the latest version we have.
                    }
                    patches[i] = patch.toString(); // Write-back
                }

                ((FileChangeRequest) request.data).setChanges(patches);
                ((FileChangeRequest) request.data).setBaseFileVersion(batchingCtrl.maxVersionSeen);

                // Save response data, and fire off the actual responseHandler
                IResponseHandler respHandler = request.getResponseHandler();
                request.setResponseHandler(response -> {
                    if (response.getStatus() == 200) {
                        synchronized (batchingCtrl.patchBatchingQueue) {
                            logger.debug(String.format("Removing patches %s; patch queue", batchingCtrl.patchBatchingQueue.subList(0, patches.length)).replace("\n", "\\n") + "\n");
                            // Remove the sent patches
                            for (int i = 0; i < patches.length; i++) {
                                batchingCtrl.patchBatchingQueue.remove(0);
                            }
                        }
                        if (((FileChangeResponse) response.getData()).getMissingPatches() != null) {
                            batchingCtrl.lastResponsePatches = ((FileChangeResponse) response.getData()).getMissingPatches();
                            batchingCtrl.maxVersionSeen = ((FileChangeResponse) response.getData()).getFileVersion();
                        }
                        if (respHandler != null) {
                            respHandler.handleResponse(response);
                        }
                    }

                    logger.debug(String.format("File Change Success; running releaser. Changes sent: %s", Arrays.toString(patches)).replace("\n", "\\n") + "\n");
                    releaser.run();
                    timer.purge();
                    timer.cancel();
                });
            } else {
                return;
            }
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    logger.debug("Request timed out, running releaser.");
                    releaser.run();
                }
            }, TimeUnit.SECONDS.toMillis(5));
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
        Notification an;
        try {
            an = mapper.convertValue(wrapper.getMessageJson(), Notification.class);
        } catch (IllegalArgumentException e) {
            String notificationMessage = wrapper.getMessageJson().toString();
            logger.error(String.format("Malformed notification from server: %s", notificationMessage));
            return;
        }

        // parse body of notification
        try {
            an.parseData();
        } catch (JsonProcessingException e) {
            String notificationData = an.getJsonData().toString();
            logger.error(String.format("Malformed notification data from server: %s", notificationData));
            return;
        } catch (ClassNotFoundException e) {
            logger.error("Notification data class not found");
            return;
        }

        String key = an.getResource() + '.' + an.getMethod();
        INotificationHandler handler = notificationHandlerHashMap.get(key);
        if (handler == null) {
            String notificationMessage = wrapper.getMessageJson().toString();
            logger.warn("No handler registered for notification: " + notificationMessage);
            return;
        }
        handler.handleNotification(an);
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

    private class BatchingControl {
        final Semaphore batchingSem = new Semaphore(1);
        private final ArrayList<String> patchBatchingQueue = new ArrayList<>();
        String[] lastResponsePatches = new String[0];
        long maxVersionSeen = -1;
    }
}
