package websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerMessageWrapper {

    // constants for the Type variable for ServerMessageWrapper
    @JsonIgnore
    public static final String TYPE_NOTIFICATION = "Notification";
    @JsonIgnore
    public static final String TYPE_RESPONSE = "Response";

    @JsonProperty("Type")
    protected String type;

    @JsonProperty("Timestamp")
    protected long timestamp;

    @JsonProperty("ServerMessage")
    protected JsonNode messageJson;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public JsonNode getMessageJson() {
        return messageJson;
    }

    public void setMessageJson(JsonNode messageJson) {
        this.messageJson = messageJson;
    }

}
