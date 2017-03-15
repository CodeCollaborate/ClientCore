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
public class Notification extends AbstractServerMessage {
    @JsonProperty("Resource")
    private String resource;

    @JsonProperty("Method")
    private String method;

    @JsonProperty("ResourceID")
    private long resourceID;

    @JsonProperty("Data")
    protected INotificationData data;

    @JsonCreator
    public Notification(@JsonProperty("Resource") String resource,
                        @JsonProperty("Method") String method,
                        @JsonProperty("ResourceID") long resourceID,
                        @JsonProperty("Data") JsonNode jsonData){
        super(jsonData);
        this.resource = resource;
        this.method = method;
        this.resourceID = resourceID;
    }

    public Notification(String resource,
                        String method,
                        long resourceID,
                        INotificationData data){
        super(null);
        this.resource = resource;
        this.method = method;
        this.resourceID = resourceID;
        this.data = data;
    }


    public void parseData() throws JsonProcessingException, ClassNotFoundException {
        Class<? extends INotificationData> type;
        String classname = this.getClass().getPackage().getName() + ".notifications." + resource + method + "Notification";

        Class c = Class.forName(classname);
        if (!INotificationData.class.isAssignableFrom(c)) {
            throw new ClassNotFoundException("Class " + classname + " does not implement INotificationData");
        }
        type = c;

        this.data = mapper.treeToValue(jsonData, type);
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

    public long getResourceID() {
        return resourceID;
    }

    public void setResourceID(long resourceID) {
        this.resourceID = resourceID;
    }

    @Override
    public INotificationData getData() {
        return data;
    }
}
