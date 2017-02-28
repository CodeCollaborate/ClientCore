package clientcore.websocket.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response extends AbstractServerMessage {
    @JsonProperty("Tag")
    private long tag;

    @JsonProperty("Status")
    private int status;

    @JsonProperty("Data")
    private IResponseData data;

    @JsonCreator
    public Response(@JsonProperty("Tag") long tag,
                    @JsonProperty("Status") int status,
                    @JsonProperty("Data") JsonNode jsonData){
        super(jsonData);
        this.tag = tag;
        this.status = status;
    }

    public Response(long tag, int status, IResponseData data){
        super(null);
        this.tag = tag;
        this.status = status;
        this.data = data;
    }

    public void parseData(Class requestType) throws JsonProcessingException, ClassNotFoundException {
        Class<? extends IResponseData> type;
        String classname = requestType.getName().replace("requests", "responses").replace("Request", "Response");

        Class c = Class.forName(classname);
        if (!IResponseData.class.isAssignableFrom(c)) {
            throw new ClassNotFoundException("Class " + classname + " does not implement IResponseData");
        }
        type = c;

        this.data = mapper.treeToValue(jsonData, type);
    }


    public long getTag() {
        return tag;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public IResponseData getData() {
        return data;
    }
}
