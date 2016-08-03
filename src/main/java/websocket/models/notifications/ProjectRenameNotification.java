package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class ProjectRenameNotification extends Notification {
	@JsonProperty("NewName")
	public String newName;

	public ProjectRenameNotification(String newName, long projectID) {
		this.newName = newName;
		super.setResource("Project");
		super.setMethod("Rename");
		super.setResourceID(projectID);
	}
	
	
}
