package websocket;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;

/**
 * Created by fahslaj on 4/14/2016.
 */
public class Connector {
    public static void main(String[] args) {
        Connector connector = new Connector();
        connector.connect("ws://echo.websocket.org");
        try {
            Thread.sleep(5000); // We need to block to wait for connect
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        connector.sendMessage("Hello");
    }

    WebSocketClient client;
    CCWebSocket socket;
    URI uri;

    public void sendMessage(String message) {
        try {
            socket.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean connect(String uriString) {
        if (client != null || socket != null) {
            return false;
        }
        client = new WebSocketClient();
        socket = new CCWebSocket(this);
        try {
            client.start();
            uri = new URI(uriString);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, uri, request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void receiveMessage(String text) {
        System.out.println("Received message: "+text);
    }
}
