package clientcore.websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractServerMessage {
    protected final ObjectMapper mapper = new ObjectMapper();

    @JsonProperty("Data")
    protected JsonNode jsonData;

    public JsonNode getJsonData() {
        return jsonData;
    }

    public void setJsonData(JsonNode jsonData) {
        this.jsonData = jsonData;
    }

    public abstract IServerMessageData getData();
}
