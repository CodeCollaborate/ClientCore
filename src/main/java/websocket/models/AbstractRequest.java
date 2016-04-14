package websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class AbstractRequest {

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

    @JsonProperty("Data")
    protected IRequestData data;

    public long getTag() {
        return tag;
    }

    public void setTag(long tag) {
        this.tag = tag;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
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
}
