package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectDeleteRequest implements IRequestData {

    @JsonProperty("ProjectID")
    private long projectID;

    public ProjectDeleteRequest(long projectID) {
        this.projectID = projectID;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "Delete", this, responseHandler, requestSendErrorHandler);
    }

    public long getProjectID() {
        return projectID;
    }
}
