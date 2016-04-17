package websocket.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by fahslaj on 4/17/2016.
 */
public class ConnectionConfig {

    @JsonProperty("URIString")
    protected String uriString;

    @JsonProperty("Reconnect")
    protected boolean reconnect;

    @JsonProperty("MaxRetryCount")
    protected int maxRetryCount;

    public ConnectionConfig(
            @JsonProperty("URIString") String uriString,
            @JsonProperty("Reconnect")boolean reconnect,
            @JsonProperty("MaxRetryCount")int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        this.reconnect = reconnect;
        this.uriString = uriString;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public String getUriString() {
        return uriString;
    }

    public void setUriString(String uriString) {
        this.uriString = uriString;
    }
}
