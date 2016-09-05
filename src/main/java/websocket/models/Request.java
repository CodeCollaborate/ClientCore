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
    protected long tag;

    @JsonProperty("Resource")
    protected String resource;

    @JsonProperty("Method")
    protected String method;

    @JsonProperty("SenderID")
    protected String senderId;

    @JsonProperty("SenderToken")
    protected String senderToken;

    @JsonProperty("Timestamp")
    protected long timestamp;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    protected IRequestData data;

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
        // TODO: get these from the client core based on user info
        this.senderId = "12345";
        this.senderToken = "12345";
        this.timestamp = System.currentTimeMillis();
    }
    
    public Request(String resource, String method,
                      IRequestData data, IResponseHandler responseHandler, IRequestSendErrorHandler errorHandler) {
        this.tag = tagGenerator.getAndIncrement();
        this.resource = resource;
        this.method = method;
        // TODO: get these from the client core based on user info
        this.senderId = null;
        this.senderToken = null;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
        this.responseHandler = responseHandler;
        this.errorHandler = errorHandler;
    }

    public long getTag() {
        return tag;
    }

    public String getResource() {
        return resource;
    }

    public String getMethod() {
        return method;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderToken() {
        return senderToken;
    }

    public void setSenderToken(String senderToken) {
        this.senderToken = senderToken;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public IRequestData getData() {
        return data;
    }

    public void setData(IRequestData data) {
        this.data = data;
    }

    public IResponseHandler getResponseHandler() {
        return responseHandler;
    }

    public void setResponseHandler(IResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
    }

    public IRequestSendErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(IRequestSendErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
}
