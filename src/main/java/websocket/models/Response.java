package websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response extends AbstractServerMessage {

    @JsonProperty("Tag")
    protected long tag;

    @JsonProperty("Status")
    protected int status;

    protected IResponseData data;

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
