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
public class ProjectGrantPermissionsRequest implements IRequestData {

    @JsonProperty("ProjectID")
    private long projectID;

    @JsonProperty("GrantUsername")
    private String grantUsername;

    @JsonProperty("PermissionLevel")
    private int permissionLevel;

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

    public long getProjectID() {
        return projectID;
    }

    public String getGrantUsername() {
        return grantUsername;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }
}
