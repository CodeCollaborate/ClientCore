package websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import websocket.ExampleEchoServer.ServerRunner;
import websocket.models.ConnectionConfig;
import websocket.models.Request;
import websocket.models.ServerMessageWrapper;

import java.io.IOException;
import java.net.ConnectException;

import static org.mockito.Mockito.*;

/**
 * Created by fahslaj on 4/16/2016.
 */
public class TestWSManager {
    public static final String ECHO_WS_ADDR = "ws://localhost:10240";
    private static ObjectMapper mapper;

    @BeforeClass
    public static void setup() {
        ServerRunner.runEchoServer(10240);
        mapper = new ObjectMapper();
    }

    @Test
    public void testSendRequest() {
        WSConnection fakeConn = mock(WSConnection.class);
        WSManager manager = new WSManager(fakeConn);
        when(fakeConn.getState()).thenReturn(WSConnection.State.READY);
        try {
            manager.sendRequest(new Request());
        } catch (ConnectException e) {
            e.printStackTrace();
            Assert.fail("Request send failure");
        }
        verify(fakeConn, times(2)).getState();
        verify(fakeConn, times(1)).enqueueMessage(anyString());
    }

    @Test
    public void testSendRequestRetryConnect() {
        WSConnection fakeConn = mock(WSConnection.class);
        WSManager manager = new WSManager(fakeConn);
        when(fakeConn.getState()).thenReturn(WSConnection.State.CREATED).thenReturn(WSConnection.State.READY);
        try {
            manager.sendRequest(new Request());
        } catch (ConnectException e) {
            e.printStackTrace();
            Assert.fail("Request send failure");
        }
        verify(fakeConn, times(1)).getState();
        try {
            verify(fakeConn, times(1)).connect();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Shouldn't ever get here");
        }
        verify(fakeConn, times(1)).enqueueMessage(anyString());
    }

    @Test
    public void testSendRequestFailConnect() {
        WSConnection fakeConn = mock(WSConnection.class);
        WSManager manager = new WSManager(fakeConn);
        when(fakeConn.getState()).thenReturn(WSConnection.State.CREATED);
        try {
            doThrow(new Exception("Can't connect teehee")).when(fakeConn).connect();
        } catch (Exception e) {
            // shouldn't ever get here
            Assert.fail("How did this happen?");
        }
        try {
            manager.sendRequest(new Request());
            Assert.fail("Didn't throw expected ConnectException");
        } catch (ConnectException e) {
            // should go here
        }
        verify(fakeConn, times(1)).getState();
        try {
            verify(fakeConn, times(1)).connect();
        } catch (Exception e) {
            // should break
        }
        verify(fakeConn, times(0)).enqueueMessage(anyString());
    }

    @Test
    public void testHandleServerResponse() {
        String message = "{\n" +
                "  \"Type\":\"Response\",\n" +
                "  \"ServerMessage\": {\n" +
                "    \"Tag\":\"100\",\n" +
                "    \"Status\":\"200\",\n" +
                "    \"Data\":{\n" +
                "      \"TestParameter1\":\"value1\",\n" +
                "      \"TestParameter2\":\"value2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        WSConnection nullConn = mock(WSConnection.class);
        WSManager manager = new WSManager(nullConn);
        Request mockRequest = mock(Request.class);
        IResponseHandler mockHandler = mock(IResponseHandler.class);
        when(mockRequest.getResponseHandler()).thenReturn(mockHandler);
        manager.requestHashMap.put(Long.decode("100"), mockRequest);
        manager.handleMessage(message);
        verify(mockRequest, times(1)).getResponseHandler();
        verify(mockHandler, times(1)).handleResponse(anyObject());
    }

    @Test
    public void testHandleServerNotification() {
        String message = "{\n" +
                "  \"Type\":\"Notification\",\n" +
                "  \"ServerMessage\": {\n" +
                "    \"Resource\":\"TestResource\",\n" +
                "    \"Method\":\"TestMethod\",\n" +
                "    \"Data\":{\n" +
                "      \"TestParameter1\":\"value1\",\n" +
                "      \"TestParameter2\":\"value2\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        WSConnection nullConn = mock(WSConnection.class);
        WSManager manager = new WSManager(nullConn);
        INotificationHandler mockHandler = mock(INotificationHandler.class);
        manager.registerNotificationHandler("TestResource.TestMethod", mockHandler);
        manager.handleMessage(message);
        verify(mockHandler, times(1)).handleNotification(anyObject());
    }

    @Test
    public void testHandleServerNotificationIntegrationTest() {
        WSConnection conn = new WSConnection(new ConnectionConfig(ECHO_WS_ADDR, false, 5));
        ServerMessageWrapper smw = null;

        try {
            smw = mapper.readValue("{\n" +
                    "  \"Type\":\"Notification\",\n" +
                    "  \"ServerMessage\": {\n" +
                    "    \"Resource\":\"TestResource\",\n" +
                    "    \"Method\":\"TestMethod\",\n" +
                    "    \"Data\":{\n" +
                    "      \"TestParameter1\":\"value1\",\n" +
                    "      \"TestParameter2\":\"value2\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}", ServerMessageWrapper.class);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("could not map");
        }
        WSConnection nullConn = mock(WSConnection.class);
        WSManager manager = new WSManager(nullConn);
        conn.registerIncomingMessageHandler(manager);

        final String[] testText = new String[1];
        INotificationHandler testHandler = notification -> {
            testText[0] = notification.getData().toString();
            synchronized (this) {
                this.notifyAll();
            }
        };

        manager.registerNotificationHandler("TestResource.TestMethod", testHandler);

        try {
            conn.connect();
            conn.enqueueMessage(mapper.writeValueAsString(smw));
            synchronized (this) {
                this.wait(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String expected = "{" +
                "\"TestParameter1\":\"value1\"," +
                "\"TestParameter2\":\"value2\"}";
        Assert.assertEquals(expected, testText[0]);
    }

    @Test
    public void testHandleMessageSendError() {
        String message = "{\n" +
                "    \"Tag\":\"100\",\n" +
                "    \"Status\":\"200\",\n" +
                "    \"Data\":{\n" +
                "      \"TestParameter1\":\"value1\",\n" +
                "      \"TestParameter2\":\"value2\"\n" +
                "    }\n" +
                "  }";
        IRequestSendErrorHandler mockHandler = mock(IRequestSendErrorHandler.class);
        Request mockRequest = mock(Request.class);
        when(mockRequest.getErrorHandler()).thenReturn(mockHandler);
        WSConnection mockConn = mock(WSConnection.class);
        WSManager manager = new WSManager(mockConn);
        manager.requestHashMap.put(Long.decode("100"), mockRequest);
        manager.handleMessageSendError(message);
        verify(mockRequest, times(1)).getErrorHandler();
        verify(mockHandler, times(1)).handleRequestSendError();
    }
}
