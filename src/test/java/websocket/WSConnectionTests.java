package websocket;

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import websocket.ExampleEchoServer.ServerRunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by Benedict on 4/15/2016.
 */
public class WSConnectionTests {
    public static final String ECHO_WS_ADDR = "ws://echo.websocket.org";

    @BeforeClass
    public static void setup() {
        ServerRunner.runEchoServer(10240);
    }

    @Test
    public void testRegisterDeregisterHandler() {
        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 5);
        ArrayList<String> receivedItems = new ArrayList<>();

        IMessageHandler handler = new IMessageHandler() {
            @Override
            public void handleMessage(String message) {
                synchronized (receivedItems) {
                    receivedItems.add(message);
                }
            }

            @Override
            public void handleMessageSendError(String message) {
                // do nothing.
            }
        };

        Assert.assertEquals(conn.incomingMessageHandlers.size(), 0);
        Assert.assertEquals(receivedItems.size(), 0);

        conn.registerIncomingMessageHandler(handler);
        conn.onMessage("test");
        Assert.assertEquals(conn.incomingMessageHandlers.size(), 1);
        Assert.assertEquals(receivedItems.size(), 1);

        conn.deregisterIncomingMessageHandler(handler);
        conn.onMessage("test");
        Assert.assertEquals(conn.incomingMessageHandlers.size(), 0);
        Assert.assertEquals(receivedItems.size(), 1);
    }

    @Test
    public void testMessageQueue() {
        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 5);

        Assert.assertEquals(conn.messageQueue.size(), 0);

        conn.enqueueMessage("Test1");
        Assert.assertEquals(conn.messageQueue.size(), 1);
        conn.enqueueMessage("Test2");
        Assert.assertEquals(conn.messageQueue.size(), 2);
        conn.enqueueMessage("Test3");
        Assert.assertEquals(conn.messageQueue.size(), 3);

        WSConnection.WSMessage retriedMessage1 = new WSConnection.WSMessage("RetryTest1");
        retriedMessage1.incrementRetryCount();

        WSConnection.WSMessage retriedMessage2 = new WSConnection.WSMessage("RetryTest2");
        retriedMessage2.incrementRetryCount();

        WSConnection.WSMessage retriedMessage3 = new WSConnection.WSMessage("RetryTest3");
        retriedMessage3.incrementRetryCount();
        retriedMessage3.incrementRetryCount();

        conn.messageQueue.offer(retriedMessage1);
        conn.messageQueue.offer(retriedMessage2);
        conn.messageQueue.offer(retriedMessage3);

        // Should be sorted for max-retries, then min-creation order
        String[] expected = new String[]{"RetryTest3", "RetryTest1", "RetryTest2", "Test1", "Test2", "Test3"};
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(expected[i], conn.messageQueue.poll().getMessage());
        }
    }

    @Test
    public void testSendMessageStateChecker() {
        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 5);

        Assert.assertEquals(conn.messageQueue.size(), 0);

        WSConnection.WSMessage msg = new WSConnection.WSMessage("msg");

        Assert.assertEquals(conn.getState(), WSConnection.State.CREATED);
        try {
            conn.sendMessage(msg);
            Assert.fail("Should be in incorrect state; should have failed.");
        } catch (IllegalStateException e) {
            // Works!
        }

        conn.setState(WSConnection.State.CONNECT);
        try {
            conn.sendMessage(msg);
            Assert.fail("Should be in incorrect state; should have failed.");
        } catch (IllegalStateException e) {
            // Works!
        }

        conn.setState(WSConnection.State.CREATED);
        try {
            conn.sendMessage(msg);
            Assert.fail("Session not created; should have failed.");
        } catch (IllegalStateException e) {
            // Works!
        }

        conn.setState(WSConnection.State.CLOSE);
        try {
            conn.sendMessage(msg);
        } catch (IllegalStateException e) {
            Assert.fail("Should return without doing anything on States CLOSE and EXIT");
        }

        conn.setState(WSConnection.State.EXIT);
        try {
            conn.sendMessage(msg);
        } catch (IllegalStateException e) {
            Assert.fail("Should return without doing anything on States CLOSE and EXIT");
        }
    }

    @Test
    public void testSendMessageRequeue() {
        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 5);

        ArrayList<String> erroredMessages = new ArrayList<>();
        IMessageHandler handler = new IMessageHandler() {
            @Override
            public void handleMessage(String message) {
                // do nothing
            }

            @Override
            public void handleMessageSendError(String message) {
                synchronized (erroredMessages) {
                    erroredMessages.add(message);
                }
            }
        };
        conn.registerIncomingMessageHandler(handler);

        conn.session = new testSession() {
            @Override
            public RemoteEndpoint getRemote() {
                return new testRemote() {
                    @Override
                    public Future<Void> sendStringByFuture(String text) {
                        return new testFuture<Void>() {
                            @Override
                            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                                throw new ExecutionException(new Exception("test"));
                            }

                            @Override
                            public Void get() throws InterruptedException, ExecutionException {
                                throw new ExecutionException(new Exception("test"));
                            }
                        };
                    }
                };
            }
        };
        conn.setState(WSConnection.State.READY);
        WSConnection.WSMessage message = new WSConnection.WSMessage("Test");

        Assert.assertEquals(message.getRetryCount(), 0);
        Assert.assertEquals(conn.messageQueue.size(), 0);

        conn.messageQueue.offer(message);

        for (int i = 1; i <= 6; i++) {
            conn.sendMessage(conn.messageQueue.poll());
            if (i <= 5) {
                Assert.assertEquals(message.getRetryCount(), i);
                Assert.assertEquals(conn.messageQueue.size(), 1);
            } else {
                Assert.assertEquals(message.getRetryCount(), 5);
                Assert.assertEquals(conn.messageQueue.size(), 0);
            }
        }

        synchronized (erroredMessages) {
            Assert.assertEquals(erroredMessages.size(), 1);
        }
    }

    @Test
    public void testSendMessageLoop() throws InterruptedException {
        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 0);

        ArrayList<String> erroredMessages = new ArrayList<>();
        IMessageHandler handler = new IMessageHandler() {
            @Override
            public void handleMessage(String message) {
                // do nothing
            }

            @Override
            public void handleMessageSendError(String message) {
                synchronized (erroredMessages) {
                    erroredMessages.add(message);
                }
            }
        };
        conn.registerIncomingMessageHandler(handler);

        conn.session = new testSession() {
            @Override
            public RemoteEndpoint getRemote() {
                return new testRemote() {
                    @Override
                    public Future<Void> sendStringByFuture(String text) {
                        return new testFuture<Void>() {
                            @Override
                            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                                throw new ExecutionException(new Exception("test"));
                            }

                            @Override
                            public Void get() throws InterruptedException, ExecutionException {
                                throw new ExecutionException(new Exception("test"));
                            }
                        };
                    }
                };
            }
        };
        conn.setState(WSConnection.State.READY);
        WSConnection.WSMessage message = new WSConnection.WSMessage("Test");

        Assert.assertEquals(message.getRetryCount(), 0);
        Assert.assertEquals(conn.messageQueue.size(), 0);

        conn.messageQueue.offer(message);

        Thread t = new Thread(conn::messageLoop);
        t.start();

        Thread.currentThread();
        Thread.sleep(500);

        conn.close();

        synchronized (erroredMessages) {
            Assert.assertEquals(erroredMessages.size(), 1);
        }
    }

    @Test
    public void testSendMessageSucceed() {
        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 5);

        conn.session = new testSession() {
            @Override
            public RemoteEndpoint getRemote() {
                return new testRemote() {
                    @Override
                    public Future<Void> sendStringByFuture(String text) {
                        return new testFuture<Void>() {
                            @Override
                            public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                                return null;
                            }

                            @Override
                            public Void get() throws InterruptedException, ExecutionException {
                                return null;
                            }
                        };
                    }
                };
            }
        };
        conn.setState(WSConnection.State.READY);
        WSConnection.WSMessage message = new WSConnection.WSMessage("Test");

        Assert.assertEquals(message.getRetryCount(), 0);
        Assert.assertEquals(conn.messageQueue.size(), 0);

        conn.messageQueue.offer(message);
        conn.sendMessage(conn.messageQueue.poll());

        Assert.assertEquals(message.getRetryCount(), 0);
        Assert.assertEquals(conn.messageQueue.size(), 0);
    }

    @Test
    public void testStateChanges() {
        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 5);

        Assert.assertEquals(conn.getState(), WSConnection.State.CREATED);

        Thread t = new Thread(() -> {
            try {
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            conn.setState(WSConnection.State.CONNECT);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.CONNECT, 150);
        Assert.assertEquals(conn.getState(), WSConnection.State.CONNECT);

        t = new Thread(() -> {
            try {
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            conn.setState(WSConnection.State.READY);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.READY, 150);
        Assert.assertEquals(conn.getState(), WSConnection.State.READY);

        t = new Thread(() -> {
            try {
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            conn.setState(WSConnection.State.CLOSE);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.CLOSE, 150);
        Assert.assertEquals(conn.getState(), WSConnection.State.CLOSE);

        t = new Thread(() -> {
            try {
                Thread.currentThread();
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            conn.setState(WSConnection.State.EXIT);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.EXIT, 150);
        Assert.assertEquals(conn.getState(), WSConnection.State.EXIT);
    }

    @Test
    public void testClose() throws Exception {

        WSConnection conn = new WSConnection(ECHO_WS_ADDR, false, 5);

        // Simulate error in connection. Should exit anyways, since reconnect flag is false
        conn.setState(WSConnection.State.READY);
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(conn.getState(), WSConnection.State.EXIT);

        // Simulate normal closing of connection without client set. Should exit, and go into EXIT state.
        conn.setState(WSConnection.State.READY);
        conn.close();
        Assert.assertEquals(conn.getState(), WSConnection.State.CLOSE);
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(conn.getState(), WSConnection.State.EXIT);

        // Simulate normal closing of connection with client set. Should exit, and go into EXIT state.
        conn.setState(WSConnection.State.READY);
        conn.client = new WebSocketClient() {
            @Override
            protected void stop(LifeCycle l) throws Exception {
                return;
            }
        };
        conn.client.start();
        conn.close();
        Assert.assertEquals(conn.getState(), WSConnection.State.CLOSE);
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(conn.getState(), WSConnection.State.EXIT);

        // Simulate normal closing of connection with client set, which will fail to stop. Should cause an exception.
        conn.setState(WSConnection.State.READY);
        conn.client = new WebSocketClient() {

            @Override
            protected void stop(LifeCycle l) throws Exception {
                throw new Exception("test");
            }
        };
        conn.client.start();
        try {
            conn.close();
            Assert.fail("Should have thrown an error; failed to close");
        } catch (IllegalStateException e) {
            // Should have thrown error. Continue.
        }
        Assert.assertEquals(conn.getState(), WSConnection.State.ERROR);

        // Simulate errored closing of connection with client set. Should exit, and go into EXIT state.
        conn.setState(WSConnection.State.READY);
        conn.client = new WebSocketClient() {
            @Override
            protected void stop(LifeCycle l) throws Exception {
                return;
            }
        };
        conn.client.start();
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(conn.getState(), WSConnection.State.EXIT);

        // Simulate errored closing of connection with client set, which will fail to stop. Should cause an exception.
        conn.setState(WSConnection.State.READY);
        conn.client = new WebSocketClient() {
            @Override
            protected void stop(LifeCycle l) throws Exception {
                throw new Exception("test");
            }
        };
        conn.client.start();
        try {
            conn.onClose(1001, "SHUTDOWN");
            Assert.fail("Should have thrown an error; failed to close");
        } catch (IllegalStateException e) {
            // Should have thrown error. Continue.
        }
        Assert.assertEquals(conn.getState(), WSConnection.State.ERROR);
    }

    @Test
    public void testReconnect() throws Exception {
        ArrayList<String> receivedMessages = new ArrayList<>();

        WSConnection conn = new WSConnection("ws://localhost:10240", true, 3);
        conn.registerIncomingMessageHandler(new IMessageHandler() {
            @Override
            public void handleMessage(String message) {
                synchronized (receivedMessages) {
                    receivedMessages.add(message);
                }
            }

            @Override
            public void handleMessageSendError(String message) {
                throw new IllegalStateException("Should not have any message send errors");
            }
        });

        conn.connect();

        if (!conn.waitForNextState(WSConnection.State.READY, 5000)) {
            Assert.fail("Failed to get to Ready state");
        }

        conn.setState(WSConnection.State.CLOSE);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                synchronized (conn.messageQueue) {
                    conn.messageQueue.notifyAll();
                }
                conn.setState(WSConnection.State.CREATED);
                if (conn.client != null) {
                    WebSocketClient client = conn.client;
                    conn.client = null;
                    try {
                        client.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();

        if (!conn.waitForNextState(WSConnection.State.CREATED, 5000)) {
            Assert.fail("Failed to get to Created state");
        }
        if (!conn.waitForNextState(WSConnection.State.CONNECT, 5000)) {
            Assert.fail("Failed to get to Connect state");
        }
        if (!conn.waitForNextState(WSConnection.State.READY, 5000)) {
            Assert.fail("Failed to get to Ready state");
        }

        // Ensure that connection is actually open, and messages can be sent.
        for (int i = 0; i < 5; i++) {
            conn.enqueueMessage("HELLO" + i);
        }

        // wait for messages to be received.
        Thread.sleep(1000);

        conn.close();

        if (!conn.waitForNextState(WSConnection.State.EXIT, 5000)) {
            Assert.fail("Failed to get to Exit state");
        }

        Assert.assertEquals(receivedMessages.size(), 5);
    }

    @Test
    public void testIntegrationAgainstEcho() throws Exception {
        ArrayList<String> receivedMessages = new ArrayList<>();

        WSConnection conn = new WSConnection("ws://localhost:10240", false, 3);
        conn.registerIncomingMessageHandler(new IMessageHandler() {
            @Override
            public void handleMessage(String message) {
                synchronized (receivedMessages) {
                    receivedMessages.add(message);
                }
            }

            @Override
            public void handleMessageSendError(String message) {
                throw new IllegalStateException("Should not have any message send errors");
            }
        });
        conn.connect();

        if (!conn.waitForNextState(WSConnection.State.READY, 5000)) {
            Assert.fail("Failed to get to Exit state");
        }

        for (int i = 0; i < 5; i++) {
            conn.enqueueMessage("HELLO" + i);
        }

        Thread.sleep(1000);

        conn.close();

        if (!conn.waitForNextState(WSConnection.State.EXIT, 5000)) {
            Assert.fail("Failed to get to Exit state");
        }

        Assert.assertEquals(receivedMessages.size(), 5);
    }

    private abstract class testSession implements Session {

        @Override
        public void close() {

        }

        @Override
        public void close(CloseStatus closeStatus) {

        }

        @Override
        public void close(int statusCode, String reason) {

        }

        @Override
        public void disconnect() throws IOException {

        }

        @Override
        public long getIdleTimeout() {
            return 0;
        }

        @Override
        public void setIdleTimeout(long ms) {

        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public WebSocketPolicy getPolicy() {
            return null;
        }

        @Override
        public String getProtocolVersion() {
            return null;
        }

        @Override
        public RemoteEndpoint getRemote() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public UpgradeRequest getUpgradeRequest() {
            return null;
        }

        @Override
        public UpgradeResponse getUpgradeResponse() {
            return null;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public SuspendToken suspend() {
            return null;
        }
    }

    private abstract class testRemote implements RemoteEndpoint {

        @Override
        public void sendBytes(ByteBuffer data) throws IOException {

        }

        @Override
        public Future<Void> sendBytesByFuture(ByteBuffer data) {
            return null;
        }

        @Override
        public void sendBytes(ByteBuffer data, WriteCallback callback) {

        }

        @Override
        public void sendPartialBytes(ByteBuffer fragment, boolean isLast) throws IOException {

        }

        @Override
        public void sendPartialString(String fragment, boolean isLast) throws IOException {

        }

        @Override
        public void sendPing(ByteBuffer applicationData) throws IOException {

        }

        @Override
        public void sendPong(ByteBuffer applicationData) throws IOException {

        }

        @Override
        public void sendString(String text) throws IOException {

        }

        @Override
        public Future<Void> sendStringByFuture(String text) {
            return null;
        }

        @Override
        public void sendString(String text, WriteCallback callback) {

        }

        @Override
        public BatchMode getBatchMode() {
            return null;
        }

        @Override
        public void setBatchMode(BatchMode mode) {

        }

        @Override
        public void flush() throws IOException {

        }
    }

    private abstract class testFuture<V> implements Future<V> {

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return null;
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return null;
        }
    }
}
