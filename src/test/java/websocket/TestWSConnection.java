package websocket;

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import websocket.ExampleEchoServer.ServerRunner;
import websocket.models.ConnectionConfig;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Benedict on 4/15/2016.
 */
public class TestWSConnection {
    private static final int TEST_PORT = 10240;
    private static final ConnectionConfig TEST_CONFIG = new ConnectionConfig("ws://localhost:" + TEST_PORT, false, 5);
    private static final ConnectionConfig TEST_CONFIG_ERROR = new ConnectionConfig("ws://testhost:1", false, 5);
    private static final ConnectionConfig TEST_CONFIG_NO_RETRY = new ConnectionConfig("ws://localhost:" + TEST_PORT, false, 0);
    private static final ConnectionConfig TEST_CONFIG_RECONNECT = new ConnectionConfig("ws://localhost:" + TEST_PORT, true, 5);

    @BeforeClass
    public static void setup() {
        ServerRunner.runEchoServer(TEST_PORT);
    }

    @Test
    public void testRegisterDeregisterHandler() {
        WSConnection conn = new WSConnection(TEST_CONFIG);
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

        Assert.assertEquals(0, conn.incomingMessageHandlers.size());
        Assert.assertEquals(receivedItems.size(), 0);

        conn.registerIncomingMessageHandler(handler);
        conn.onMessage("test");
        Assert.assertEquals(1, conn.incomingMessageHandlers.size());
        Assert.assertEquals(receivedItems.size(), 1);

        conn.deregisterIncomingMessageHandler(handler);
        conn.onMessage("test");
        Assert.assertEquals(0, conn.incomingMessageHandlers.size());
        Assert.assertEquals(receivedItems.size(), 1);
    }

    @Test
    public void testMessageQueue() {
        WSConnection conn = new WSConnection(TEST_CONFIG);

        Assert.assertEquals(0, conn.messageQueue.size());

        conn.enqueueMessage("Test1");
        Assert.assertEquals(1, conn.messageQueue.size());
        conn.enqueueMessage("Test2");
        Assert.assertEquals(2, conn.messageQueue.size());
        conn.enqueueMessage("Test3");
        Assert.assertEquals(3, conn.messageQueue.size());

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
        for (String anExpected : expected) {
            Assert.assertEquals(anExpected, conn.messageQueue.poll().getMessage());
        }
    }

    @Test
    public void testSendMessageStateChecker() {
        WSConnection conn = new WSConnection(TEST_CONFIG);

        Assert.assertEquals(0, conn.messageQueue.size());

        WSConnection.WSMessage msg = new WSConnection.WSMessage("msg");

        Assert.assertEquals(WSConnection.State.CREATED, conn.getState());
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
        WSConnection conn = new WSConnection(TEST_CONFIG);

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

        conn.session = mock(Session.class);
        RemoteEndpoint testEndpoint = mock(RemoteEndpoint.class);
        Future<Void> testFuture = mock(VoidFuture.class);
        when(conn.session.getRemote()).thenReturn(testEndpoint);
        when(testEndpoint.sendStringByFuture(anyString())).thenReturn(testFuture);
        try {
            when(testFuture.get()).thenThrow(new ExecutionException(new Exception("test")));
            when(testFuture.get(anyLong(), anyObject())).thenThrow(new ExecutionException(new Exception("test")));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        conn.setState(WSConnection.State.READY);
        WSConnection.WSMessage message = new WSConnection.WSMessage("Test");

        Assert.assertEquals(0, message.getRetryCount());
        Assert.assertEquals(0, conn.messageQueue.size());

        conn.messageQueue.offer(message);

        for (int i = 1; i <= 6; i++) {
            conn.sendMessage(conn.messageQueue.poll());
            if (i <= 5) {
                Assert.assertEquals(i, message.getRetryCount());
                Assert.assertEquals(1, conn.messageQueue.size());
            } else {
                Assert.assertEquals(5, message.getRetryCount());
                Assert.assertEquals(0, conn.messageQueue.size());
            }
        }

        synchronized (erroredMessages) {
            Assert.assertEquals(erroredMessages.size(), 1);
        }
    }

    @Test
    public void testSendMessageLoop() throws InterruptedException {
        WSConnection conn = new WSConnection(TEST_CONFIG_NO_RETRY);

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
                    erroredMessages.notifyAll();
                }
            }
        };
        conn.registerIncomingMessageHandler(handler);

        conn.session = mock(Session.class);
        RemoteEndpoint testEndpoint = mock(RemoteEndpoint.class);
        Future<Void> testFuture = mock(VoidFuture.class);
        when(conn.session.getRemote()).thenReturn(testEndpoint);
        when(testEndpoint.sendStringByFuture(anyString())).thenReturn(testFuture);
        try {
            when(testFuture.get()).thenThrow(new ExecutionException(new Exception("test")));
            when(testFuture.get(anyLong(), anyObject())).thenThrow(new ExecutionException(new Exception("test")));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Assert.fail("Should not have thrown exception on mocking");
        }

        conn.setState(WSConnection.State.READY);
        WSConnection.WSMessage message = new WSConnection.WSMessage("Test");

        Assert.assertEquals(0, message.getRetryCount());
        Assert.assertEquals(0, conn.messageQueue.size());

        conn.messageQueue.offer(message);

        Thread t = new Thread(conn::messageLoop);
        t.start();

        waitForNotifies(erroredMessages, 1, 1000);

        conn.close();

        synchronized (erroredMessages) {
            Assert.assertEquals(1, erroredMessages.size());
        }
    }

    @Test
    public void testSendMessageSucceed() {
        WSConnection conn = new WSConnection(TEST_CONFIG);

        conn.session = mock(Session.class);
        RemoteEndpoint testEndpoint = mock(RemoteEndpoint.class);
        Future<Void> testFuture = mock(VoidFuture.class);
        when(conn.session.getRemote()).thenReturn(testEndpoint);
        when(testEndpoint.sendStringByFuture(anyString())).thenReturn(testFuture);
        try {
            when(testFuture.get()).thenReturn(null);
            when(testFuture.get(anyLong(), anyObject())).thenReturn(null);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }

        conn.setState(WSConnection.State.READY);
        WSConnection.WSMessage message = new WSConnection.WSMessage("Test");

        Assert.assertEquals(0, message.getRetryCount());
        Assert.assertEquals(0, conn.messageQueue.size());

        conn.messageQueue.offer(message);
        conn.sendMessage(conn.messageQueue.poll());

        Assert.assertEquals(0, message.getRetryCount());
        Assert.assertEquals(0, conn.messageQueue.size());
    }

    @Test
    public void testStateChanges() {
        WSConnection conn = new WSConnection(TEST_CONFIG);

        Assert.assertEquals(WSConnection.State.CREATED, conn.getState());

        Thread t = new Thread(() -> {
            conn.setState(WSConnection.State.CONNECT);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.CONNECT, 150);
        Assert.assertEquals(WSConnection.State.CONNECT, conn.getState());

        t = new Thread(() -> {
            conn.setState(WSConnection.State.READY);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.READY, 150);
        Assert.assertEquals(WSConnection.State.READY, conn.getState());

        t = new Thread(() -> {
            conn.setState(WSConnection.State.CLOSE);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.CLOSE, 150);
        Assert.assertEquals(WSConnection.State.CLOSE, conn.getState());

        t = new Thread(() -> {
            conn.setState(WSConnection.State.EXIT);
        });
        t.start();
        conn.waitForNextState(WSConnection.State.EXIT, 150);
        Assert.assertEquals(WSConnection.State.EXIT, conn.getState());
    }

    @Test
    public void testClose() throws Exception {

        WSConnection conn = new WSConnection(TEST_CONFIG);

        // Simulate error in connection. Should exit anyways, since reconnect flag is false
        conn.setState(WSConnection.State.READY);
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(WSConnection.State.EXIT, conn.getState());

        // Simulate normal closing of connection without client set. Should exit, and go into EXIT state.
        conn.setState(WSConnection.State.READY);
        conn.close();
        Assert.assertEquals(WSConnection.State.CLOSE, conn.getState());
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(WSConnection.State.EXIT, conn.getState());

        // Simulate normal closing of connection with client set. Should exit, and go into EXIT state.
        conn.setState(WSConnection.State.READY);
        conn.client = new WebSocketClient() {
            @Override
            protected void stop(LifeCycle l) throws Exception {
                // Just return.
            }
        };
        conn.client.start();
        conn.close();
        Assert.assertEquals(WSConnection.State.CLOSE, conn.getState());
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(WSConnection.State.EXIT, conn.getState());

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
        Assert.assertEquals(WSConnection.State.CLOSE, conn.getState());

        // Simulate errored closing of connection with client set. Should exit, and go into EXIT state.
        conn.setState(WSConnection.State.READY);
        conn.client = new WebSocketClient() {
            @Override
            protected void stop(LifeCycle l) throws Exception {
                // Just return.
            }
        };
        conn.client.start();
        conn.onClose(1001, "SHUTDOWN");
        Assert.assertEquals(WSConnection.State.EXIT, conn.getState());

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
        } catch (IllegalStateException e) {
            Assert.fail("Should not have thrown an error; should not crash a background thread.");
        }
        Assert.assertEquals(WSConnection.State.EXIT, conn.getState());
    }

    @Test
    public void testReconnect() throws Exception {
        ArrayList<String> receivedMessages = new ArrayList<>();

        WSConnection conn = new WSConnection(TEST_CONFIG_RECONNECT);
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
        Thread t = new Thread(() -> {

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

        waitForNotifies(receivedMessages, 5, 1000);

        conn.close();

        if (!conn.waitForNextState(WSConnection.State.EXIT, 5000)) {
            Assert.fail("Failed to get to Exit state");
        }

        Assert.assertEquals(receivedMessages.size(), 5);
    }

    @Test
    public void testIntegrationAgainstEcho() throws Exception {
        ArrayList<String> receivedMessages = new ArrayList<>();

        WSConnection conn = new WSConnection(TEST_CONFIG);
        conn.registerIncomingMessageHandler(new IMessageHandler() {
            @Override
            public void handleMessage(String message) {
                synchronized (receivedMessages) {
                    receivedMessages.add(message);
                    receivedMessages.notifyAll();
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

        waitForNotifies(receivedMessages, 5, 1000);

        conn.close();

        if (!conn.waitForNextState(WSConnection.State.EXIT, 5000)) {
            Assert.fail("Failed to get to Exit state");
        }

        Assert.assertEquals(receivedMessages.size(), 5);
    }

    @Test
    public void testConnectAndReconnectError() {
        WSConnection conn = new WSConnection(TEST_CONFIG_ERROR);

        try {
            conn.connect();
            Assert.fail("Should have failed on invalid host");
        } catch (Exception e) {
            // Pass.
        }

        Assert.assertEquals(WSConnection.State.ERROR, conn.getState());

        conn.setState(WSConnection.State.READY);
        conn.onClose(1001, "FORCE ERROR");

        Assert.assertEquals(WSConnection.State.ERROR, conn.getState());
    }

    private void waitForNotifies(Object obj, int num, long timeout) throws InterruptedException {
        synchronized (obj) {
            for (int i = 0; i < num; i++) {
                obj.wait(timeout);
            }
        }
    }

    private interface VoidFuture extends Future<Void> {
    }
}
