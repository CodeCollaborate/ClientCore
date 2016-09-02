package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectRevokePermissionsRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonProperty("RevokeUsername")
    protected long revokeUsername;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "RevokePermissions", this,
                (response) -> {
                    System.out.println("Received project revoke permissions response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project revoke permissions request to the server.");
                });
    }

    public ProjectRevokePermissionsRequest(long projectID, long revokeUsername) {
        this.projectID = projectID;
        this.revokeUsername = revokeUsername;
    }
}
