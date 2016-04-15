package websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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

@WebSocket(maxTextMessageSize = 64 * 1024)
public class WSConnection {

    private static final Logger logger = LoggerFactory.getLogger("websocket");

    // Queue of messages. Priority given to messages that need to be retried.
    final Queue<WSMessage> messageQueue = new PriorityQueue<>();

    // List of handlers that incoming messages should be sent to.
    final List<IMessageHandler> incomingMessageHandlers = new ArrayList<>();

    // State of program
    volatile State state;

    // Jetty objects
    WebSocketClient client;
    Session session;

    // Configuration
    private String uriString;
    private boolean reconnect;
    private int maxRetryCount;

    /**
     * Creates a new WSConnection, but does not initialize the connection
     */
    public WSConnection(String uriString, boolean reconnect, int maxRetryCount) {
        setState(State.CREATED);
        this.uriString = uriString;
        this.reconnect = reconnect;
        this.maxRetryCount = maxRetryCount;
    }

    /**
     * Helper method to open connection to URI given during instantiation. Note that the response does not mean that the connection
     * has been successfully established. To check the connection status, check the status flag.
     * <p>
     * This method puts the WSConnection into State.CONNECT
     *
     * @return true if connection request sent successfully, false otherwise.
     */
    public void connect() throws Exception {
        if (this.client == null) {
            this.client = new WebSocketClient();
        }
        this.client.start();
        URI uri = new URI(uriString);

        setState(State.CONNECT);
        Future<Session> fut = this.client.connect(this, uri, new ClientUpgradeRequest());
        fut.get();
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

        Thread sendingThread = new Thread(this::messageLoop);
        sendingThread.start();
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
            setState(State.ERROR);
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
        if (this.state != State.CLOSE && reconnect) {
            try {
                connect();
                return;
            } catch (Exception e) {
                setState(State.ERROR);
                logger.error(String.format("Error reconnecting - Exception: %s", e.getCause().getMessage()));
                throw new IllegalStateException(e);
            }
        }
        setState(State.EXIT);
        try {
            if (client != null) {
                client.stop();
            }
        } catch (Exception e) {
            setState(State.ERROR);
            logger.warn(String.format("Failed to shut down client: %s", e.getMessage()));
            throw new IllegalStateException(e);
        }
    }

    /**
     * Continually loop through message queue, sending messages as they are inserted. If no messages are in the queue,
     * wait on the message queue; enqueueMessage will notifyAll when a new item is added.
     *
     * This MUST be run on a separate thread to prevent blocking of main thread.
     *
     * Will exit if state is not ready at any point.
     */
    void messageLoop() {
        WSMessage msg;
        while (true) {
            synchronized (this) {
                if (this.state != State.READY) {
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
            logger.debug(String.format("Sent message: %s", msg.getMessage()));
        } catch (CancellationException | ExecutionException | InterruptedException | TimeoutException e) {
            logger.warn(String.format("Error sending message \"%s\" - Exception: %s", msg.getMessage(), e.getCause().getMessage()));

            // Insert into retry queue.
            if (msg.getRetryCount() < maxRetryCount) {
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

    void enqueueMessage(String msg) {
        if (this.state == State.CLOSE || state == State.EXIT) {
            return;
        }

        synchronized (messageQueue) {
            this.messageQueue.offer(new WSMessage(msg));
            this.messageQueue.notifyAll();
            logger.debug(String.format("Enqueued message: %s", msg));
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
        return state;
    }

    void setState(State state) {
        synchronized (this) {
            this.state = state;
            this.notifyAll();
        }
    }

    boolean waitForNextState(State state, long timeout) {
        if (this.state != state) {
            synchronized (this) {
                if (this.state != state) {
                    try {
                        this.wait(timeout);
                    } catch (InterruptedException e) {
                        return false;
                    }
                    return this.state == state;
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

    enum State {
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

        private final String message;
        private final long id = counter.getAndIncrement();
        private int retryCount = 0;

        WSMessage(String message) {
            this.message = message;
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

        @Override
        public int compareTo(WSMessage o) {
            int val = -1 * Integer.compare(retryCount, o.retryCount);
            if (val == 0) {
                val = Long.compare(id, o.id);
            }
            return val;
        }
    }
}
