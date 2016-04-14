package websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by fahslaj on 4/14/2016.
 */

@WebSocket(maxTextMessageSize = 64*1024)
public class CCWebSocket {
    private final CountDownLatch closeLatch;
    private Connector connector;

    @SuppressWarnings("unused")
    private Session session;

    public CCWebSocket(Connector connector) {
        this.closeLatch = new CountDownLatch(1);
        this.connector = connector;
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("CC: Connection closed: %d - %s%n \n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.printf("CC: Got connect: %s%n \n", session);
        this.session = session;
    }

    public boolean sendMessage(String msg) throws Exception {
        if (session == null)
            throw new Exception("Session is null");
        System.out.println("Sending: "+msg);
        Future<Void> fut;
        fut = session.getRemote().sendStringByFuture(msg);
        boolean done = false;
        try {
            fut.get(2, TimeUnit.SECONDS);
            done = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return done;
    }

    public void closeSession() {
        session.close(StatusCode.NORMAL, "Done");
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        System.out.println("Got Stuff: "+msg);
        connector.receiveMessage(msg);
    }

    public boolean sessionNull() {
        return session == null;
    }
}
