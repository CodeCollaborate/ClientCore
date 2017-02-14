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
public class ProjectRevokePermissionsRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonProperty("RevokeUsername")
    protected String revokeUsername;

    public ProjectRevokePermissionsRequest(long projectID, String revokeUsername) {
        this.projectID = projectID;
        this.revokeUsername = revokeUsername;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "RevokePermissions", this, responseHandler, requestSendErrorHandler);
    }
}
