package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "GrantPermissions", this,
                (response) -> {
                    System.out.println("Received project grant permissions response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project grant permissions request to the server.");
                });
    }

    public ProjectGrantPermissionsRequest(long projectID, String grantUsername, int permissionLevel) {
        this.projectID = projectID;
        this.grantUsername = grantUsername;
        this.permissionLevel = permissionLevel;
    }
}
