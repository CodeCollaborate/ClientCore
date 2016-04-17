package websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class AbstractServerMessage {

    @JsonProperty("Data")
    protected JsonNode data;

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}
