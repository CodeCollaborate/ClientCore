package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectCreateRequest implements IRequestData {

    @JsonProperty("Name")
    protected String name;

    public ProjectCreateRequest(String name) {
        this.name = name;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "Create", this, responseHandler, requestSendErrorHandler);
    }

    public String getName() {
        return name;
    }
}
