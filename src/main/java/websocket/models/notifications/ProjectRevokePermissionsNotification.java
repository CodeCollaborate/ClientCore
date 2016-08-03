package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class ProjectRevokePermissionsNotification extends Notification {
	@JsonProperty("RevokeUsername")
	public String revokeUsername;

	public ProjectRevokePermissionsNotification(String revokeUsername, long projectID) {
		this.revokeUsername = revokeUsername;
		super.setResource("Project");
		super.setMethod("RevokePermissions");
		super.setResourceID(projectID);
	}
}
