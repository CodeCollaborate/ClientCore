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
public class ProjectRenameRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonProperty("NewName")
    protected String newName;

    public ProjectRenameRequest(long projectID, String newName) {
        this.projectID = projectID;
        this.newName = newName;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "Rename", this, responseHandler, requestSendErrorHandler);
    }
}
