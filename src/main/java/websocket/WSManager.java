package websocket;

/**
 * Created by fahslaj on 4/14/2016.
 */
public class WSManager implements IMessageHandler {
    public static void main(String[] args) throws InterruptedException {

        WSConnection conn = new WSConnection("ws://echo.websocket.org", false, 3);
        conn.registerIncomingMessageHandler(new WSManager());
        try {
            conn.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        conn.connect("ws://echo.websocket.org");
        conn.enqueueMessage("HELLO1");
        conn.enqueueMessage("HELLO2");
        conn.enqueueMessage("HELLO3");
        conn.enqueueMessage("HELLO4");
        conn.enqueueMessage("HELLO5");
        Thread.sleep(1000);
        conn.enqueueMessage("HELLO6");
        conn.enqueueMessage("HELLO7");
        conn.close();
        conn.enqueueMessage("HELLO8");
        conn.enqueueMessage("HELLO9");
    }

    @Override
    public void handleMessage(String message) {

        System.out.println("Received message: " + message);
    }

    @Override
    public void handleMessageSendError(String message) {

        System.out.println("Failed to send message: " + message);
    }
}
