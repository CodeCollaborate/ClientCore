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
public class ProjectGrantPermissionsRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonProperty("GrantUsername")
    protected String grantUsername;

    @JsonProperty("PermissionLevel")
    protected int permissionLevel;

    public ProjectGrantPermissionsRequest(long projectID, String grantUsername, int permissionLevel) {
        this.projectID = projectID;
        this.grantUsername = grantUsername;
        this.permissionLevel = permissionLevel;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "GrantPermissions", this, responseHandler, requestSendErrorHandler);
    }
}
