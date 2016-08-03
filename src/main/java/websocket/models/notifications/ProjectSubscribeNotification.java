package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class ProjectSubscribeNotification extends Notification {
	@JsonProperty("Username")
	public String username;

	public ProjectSubscribeNotification(String username, long projectID) {
		this.username = username;
		super.setResource("Project");
		super.setMethod("Subscribe");
		super.setResourceID(projectID);
	}
}
