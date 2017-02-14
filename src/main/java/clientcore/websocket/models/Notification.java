package clientcore.websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Created by fahslaj on 4/14/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification extends AbstractServerMessage {

    @JsonProperty("Resource")
    protected String resource;

    @JsonProperty("Method")
    protected String method;

    @JsonProperty("ResourceID")
    protected long resourceID;

    protected INotificationData data;

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
