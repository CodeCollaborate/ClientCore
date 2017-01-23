package websocket;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import websocket.models.ConnectionConfig;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of a WSConnection that sends and receives strings.
 * <p>
 * Utilizes a queue to make sure messages enqueued while waiting for connection will be sent as soon as possible.
 * Queue further configured to prioritize previously-failed messages.
 * <p>
 * CAUTION: Has no max retry count set up. Messages that fail to send can potentially cause infinite loops.
 */

@WebSocket(maxTextMessageSize = 512 * 1024 * 1024)
public class WSConnection {
    public static final Logger logger = LogManager.getLogger("websocket");
    private static final int IDLE_TIMEOUT = 5;
    private static final long PING_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    // Queue of messages. Priority given to messages that need to be retried.
    final Queue<WSMessage> messageQueue = new PriorityQueue<>();
    // List of handlers that incoming messages should be sent to.
    final List<IMessageHandler> incomingMessageHandlers = new ArrayList<>();
    final HashMap<EventType, List<Runnable>> eventHandlers;
    private final Timer pingTimer;
    // Jetty objects
    WebSocketClient client;
    Session session;
    // Configuration
    ConnectionConfig config;
    // State of program
    private volatile State state;

    /**
     * Creates a new WSConnection, but does not initialize the connection
     */
    public WSConnection(ConnectionConfig config) {
        setState(State.CREATED);
        this.config = config;
        this.eventHandlers = new HashMap<>();
        this.pingTimer = new Timer();
    }

    public void handleEvent(EventType event) {
        List<Runnable> runnables = eventHandlers.get(event);
        if (runnables == null) {
            return;
        }
        runnables.stream().filter(Objects::nonNull).forEach(Runnable::run);
    }

    /**
     * Helper method to open connection to URI given during instantiation. Note that the response does not mean that the connection
     * has been successfully established. To check the connection status, check the status flag.
     * <p>
     * This method puts the WSConnection into State.CONNECT
     *
     * @throws Exception throws an exception for websocket connections
     */
    public void connect() throws Exception {
        try {
            if (this.client == null) {
                SslContextFactory ssl = new SslContextFactory();
                this.client = new WebSocketClient(ssl);
            }
            this.client.start();
            URI uri = new URI(config.getUriString());

            setState(State.CONNECT);
            Future<Session> fut = this.client.connect(this, uri, new ClientUpgradeRequest());
            fut.get();
        } catch (Exception e) {
            setState(State.ERROR);
            throw e;
        }
    }

    /**
     * Called once connection is established. Will log that connection has been achieved, set the state to State.READY
     * and run the sendingThread. All items in the message queue (previously added before connection achieved) will be
     * sent, and new messages will be sent as they are enqueued.
     *
     * @param session Jetty session object
     */
    @SuppressWarnings("WeakerAccess")
    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info(String.format("Got connection to %s", session.getRemoteAddress().getHostName()));
        this.session = session;
        setState(State.READY);

        logger.info(String.format("Setting websocket idle timeout to %d minutes", IDLE_TIMEOUT));
        session.setIdleTimeout(TimeUnit.MINUTES.toMillis(IDLE_TIMEOUT));

        //TODO(wongb): D

        handleEvent(EventType.ON_CONNECT);

        Thread sendingThread = new Thread(this::messageLoop);
        sendingThread.start();

        pingTimer.schedule(new PingTask(session), PING_TIMEOUT);
    }

    /**
     * Terminates the websocket connection, and sets the system to the CLOSE state.
     */
    public void close() {
        setState(State.CLOSE);
        synchronized (this.messageQueue) {
            this.messageQueue.notifyAll();
        }
        try {
            if (this.client != null) {
                this.client.stop();
                this.client = null;
            }
        } catch (Exception e) {
            logger.warn(String.format("Failed to shut down client: %s", e.getMessage()));
            throw new IllegalStateException("Could not shutdown properly");
        }
    }

    /**
     * Called when websocket has been closed. Will attempt to reconnect if previous state is not CLOSE.
     * This means that it will attempt to reconnect unless close() is explicitly called.
     * <p>
     * If previous state is CLOSE, will exit without attempting to reconnect.
     *
     * @param statusCode Status code of closed websocket
     * @param reason     Reason that websocket was closed.
     */
    @SuppressWarnings("WeakerAccess")
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.info(String.format("Connection closed - Reason: %s", reason));
        this.session = null;

        handleEvent(EventType.ON_CLOSE);

        if (getState() != State.CLOSE && config.isReconnect()) {
            for (int i = 0; i < config.getMaxRetryCount(); i++) {
                try {
                    connect();
                    return;
                } catch (Exception e) {
                    logger.error(String.format("Error reconnecting - Exception: %s", e.getCause().getMessage()));
                }
            }
            setState(State.ERROR);
            return;
        }
        setState(State.EXIT);
        try {
            if (client != null) {
                client.stop();
            }
        } catch (Exception e) {
            logger.warn(String.format("Failed to shut down client cleanly: %s", e.getMessage()));
        }
    }

    /**
     * Continually loop through message queue, sending messages as they are inserted. If no messages are in the queue,
     * wait on the message queue; enqueueMessage will notifyAll when a new item is added.
     * <p>
     * This MUST be run on a separate thread to prevent blocking of main thread.
     * <p>
     * Will exit if state is not ready at any point.
     */
    void messageLoop() {
        WSMessage msg;
        while (true) {
            synchronized (this) {
                if (getState() != State.READY) {
                    return;
                }
            }
            synchronized (messageQueue) {
                msg = messageQueue.poll();
                if (msg == null) {
                    try {
                        messageQueue.wait();
                    } catch (InterruptedException e) {
                        // Do nothing; try again next time.
                    }
                    continue;
                }
            }
            sendMessage(msg);
        }
    }

    /**
     * Sends the message through the websocket. Will exit without doing anything if state is CLOSE or EXIT.
     *
     * @param msg Message to be sent
     * @return true if message was sent successfully, false otherwise.
     * @throws IllegalStateException if no connection found.
     */
    boolean sendMessage(WSMessage msg) {
        synchronized (this) {
            if (state == State.CLOSE || state == State.EXIT) {
                return false;
            } else if (state != State.READY) {
                throw new IllegalStateException("Cannot send message if not in ready state");
            }
        }
        Future<Void> fut = session.getRemote().sendStringByFuture(msg.getMessage());
        try {
            fut.get(5, TimeUnit.SECONDS);
            String printableMsg = msg.getMessage().replaceAll("\"Password\":\"(.*?)\"", "\"Password\":\"***\"");
            logger.debug(String.format("Sent message: %s", printableMsg));
        } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.warn(String.format("Error sending message \"%s\" - Exception: %s", msg.getMessage(), e.getCause().getMessage()));

            // Insert into retry queue.
            if (msg.getRetryCount() < config.getMaxRetryCount()) {
                msg.incrementRetryCount();
                synchronized (messageQueue) {
                    this.messageQueue.offer(msg);
                    this.messageQueue.notifyAll();
                }
            } else {
                for (IMessageHandler handler : incomingMessageHandlers) {
                    handler.handleMessageSendError(msg.getMessage());
                }
            }

            return false;
        }
        return true;
    }

    void enqueueMessage(String msg, int priority) {
        if (getState() == State.CLOSE || state == State.EXIT) {
            return;
        }

        synchronized (messageQueue) {
            this.messageQueue.offer(new WSMessage(msg, priority));
            this.messageQueue.notifyAll();
            String printableMsg = msg.replaceAll("\"Password\":\"(.*?)\"", "\"Password\":\"***\"");
            logger.debug(String.format("Enqueued message: %s", printableMsg));
        }

    }

    @SuppressWarnings("WeakerAccess")
    @OnWebSocketMessage
    public void onMessage(String msg) {
        logger.debug(String.format("Received message: %s", msg));

        for (IMessageHandler handler : incomingMessageHandlers) {
            handler.handleMessage(msg);
        }
    }

    State getState() {
        synchronized (this) {
            return this.state;
        }
    }

    void setState(State state) {
        synchronized (this) {
            this.state = state;
            this.notifyAll();
        }
    }

    boolean waitForNextState(State state, long timeout) {
        if (getState() != state) {
            synchronized (this) {
                if (getState() != state) {
                    try {
                        this.wait(timeout);
                    } catch (InterruptedException e) {
                        return false;
                    }
                    return getState() == state;
                }
            }
        }
        return true;
    }

    /**
     * Registers the given handler as an incoming message handler
     *
     * @param handler the hander to be registered
     */
    public void registerIncomingMessageHandler(IMessageHandler handler) {
        this.incomingMessageHandlers.add(handler);
    }

    /**
     * Deregisters the given handler as an incoming message handler
     *
     * @param handler the hander to be deregistered
     */
    public void deregisterIncomingMessageHandler(IMessageHandler handler) {
        this.incomingMessageHandlers.remove(handler);
    }

    public enum EventType {
        ON_CONNECT,
        ON_ERROR,
        ON_CLOSE,
        ON_RECEIVE_MESSAGE,
        ON_SEND_MESSAGE
    }

    public enum State {
        CREATED,
        CONNECT,
        READY,
        CLOSE,
        EXIT,
        ERROR,
    }

    /**
     * WSMessage class allows older messages to always have higher priority, even after failure and re-insertion.
     */
    static class WSMessage implements Comparable<WSMessage> {
        private static AtomicLong counter = new AtomicLong(0);
        private final int priority;
        private final String message;
        private final long id = counter.getAndIncrement();
        private int retryCount = 0;

        WSMessage(String message, int priority) {
            this.message = message;
            this.priority = priority;
        }

        String getMessage() {
            return message;
        }

        int getRetryCount() {
            return retryCount;
        }

        void incrementRetryCount() {
            retryCount++;
        }

        public int getPriority() {
            return priority;
        }

        @Override
        public int compareTo(WSMessage o) {
            int result = Integer.compare(priority, o.priority);
            if (result != 0) {
                return result;
            }

            result = -1 * Integer.compare(retryCount, o.retryCount);
            if (result == 0) {
                result = Long.compare(id, o.id);
            }

            return result;
        }
    }

    private class PingTask extends TimerTask {
        private Session session;

        PingTask(Session session){
            this.session = session;
        }

        @Override
        public void run() {
            if (!this.session.isOpen()){
                return; // Looks like we have a closed connection...
            }
            try {
                this.session.getRemote().sendPing(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pingTimer.schedule(new PingTask(session), PING_TIMEOUT);
        }
    }
}
