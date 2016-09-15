package websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request {
    private static AtomicLong tagGen = new AtomicLong();

    private static AtomicLong tagGenerator = new AtomicLong(0);
    private Request instance;

    @JsonProperty("Tag")
    public final long tag;

    @JsonProperty("Resource")
    public final String resource;

    @JsonProperty("Method")
    public final String method;

    @JsonProperty("SenderID")
    private String senderId;

    @JsonProperty("SenderToken")
    private String senderToken;

    @JsonProperty("Timestamp")
    public final long timestamp;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public final IRequestData data;

    @JsonIgnore
    private IResponseHandler responseHandler;

    @JsonIgnore
    private IRequestSendErrorHandler errorHandler;

    /**
     * Default Request constructor that should only be used for testing requests.
     */
    public Request() {
    	this.instance = this;
    	this.tag = tagGenerator.getAndIncrement();
        this.resource = "Default";
        this.method = "Default";
        this.data = null;
        this.senderId = "12345";
        this.senderToken = "12345";
        this.timestamp = System.currentTimeMillis();
    }
    
    public Request(String resource, String method,
                      IRequestData data, IResponseHandler responseHandler, IRequestSendErrorHandler errorHandler) {
        this.tag = tagGenerator.getAndIncrement();
        this.resource = resource;
        this.method = method;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
        this.responseHandler = responseHandler;
        this.errorHandler = errorHandler;
    }

    public long getTag() {
        return tag;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderToken(String senderToken) {
        this.senderToken = senderToken;
    }

    public IResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public IRequestSendErrorHandler getErrorHandler() {
        return errorHandler;
    }
}
