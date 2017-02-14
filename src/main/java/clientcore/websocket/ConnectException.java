package clientcore.websocket;

/**
 * Created by Benedict on 9/14/2016.
 */
public class ConnectException extends IllegalStateException {
    public ConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
