package websocket;

/**
 * Created by fahslaj on 4/17/2016.
 */
public interface IConnectionConfig {
    public String getUriString();
    public boolean getReconnect();
    public int getMaxRetryCount();
}
