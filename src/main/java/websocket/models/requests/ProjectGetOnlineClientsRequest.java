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
public class ProjectGetOnlineClientsRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    public ProjectGetOnlineClientsRequest(long projectID) {
        this.projectID = projectID;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "GetOnlineClients", this, responseHandler, requestSendErrorHandler);
    }
}
