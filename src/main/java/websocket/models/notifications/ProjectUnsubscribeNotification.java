package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class ProjectUnsubscribeNotification extends Notification {
	@JsonProperty("Username")
	public String username;

	public ProjectUnsubscribeNotification(String username, long projectID) {
		this.username = username;
		super.setResource("Project");
		super.setMethod("Unsubscribe");
		super.setResourceID(projectID);
	}
}
