package websocket.ExampleEchoServer;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

@WebSocket
public class EchoSocket {
    private static final Logger LOG = Log.getLogger(EchoSocket.class);
    private Session session;
    private RemoteEndpoint remote;

    @OnWebSocketClose
    public void onWebSocketClose(int statusCode, String reason) {
        this.session = null;
        this.remote = null;
    }

    @OnWebSocketConnect
    public void onWebSocketConnect(Session session) {
        this.session = session;
        this.remote = this.session.getRemote();
    }

    @OnWebSocketError
    public void onWebSocketError(Throwable cause) {
    }

    @OnWebSocketMessage
    public void onWebSocketText(String message) {
        if (this.session != null && this.session.isOpen() && this.remote != null) {
            this.remote.sendStringByFuture(message);
        }
    }
}