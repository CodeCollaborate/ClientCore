package clientcore.websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractServerMessage {
    final ObjectMapper mapper = new ObjectMapper();

    protected JsonNode jsonData;

    AbstractServerMessage(JsonNode jsonData){
        this.jsonData = jsonData;
    }

    @JsonIgnore
    public JsonNode getJsonData() {
        return jsonData;
    }

    public void setJsonData(JsonNode jsonData) {
        this.jsonData = jsonData;
    }

    public abstract IServerMessageData getData();
}
