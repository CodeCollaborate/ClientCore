package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class ProjectGrantPermissionsNotification extends Notification{
	@JsonProperty("GrantUsername")
	public String grantUsername;
	
	@JsonProperty("PermissionLevel")
	public int permissionLevel;

	public ProjectGrantPermissionsNotification(String grantUsername, int permissionLevel, long projectID) {
		this.grantUsername = grantUsername;
		this.permissionLevel = permissionLevel;
		super.setResource("Project");
		super.setMethod("GrantPermissions");
		super.setResourceID(projectID);
	}
}
