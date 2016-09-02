package websocket.models.notifications;

import websocket.models.Notification;

public class ProjectDeleteNotification extends Notification {

	public ProjectDeleteNotification(long projectID) {
		super.setResource("Project");
		super.setMethod("Delete");
		super.setResourceID(projectID);
	}
}
