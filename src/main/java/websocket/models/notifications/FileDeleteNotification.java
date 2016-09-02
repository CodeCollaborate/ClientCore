package websocket.models.notifications;

import websocket.models.Notification;

public class FileDeleteNotification extends Notification {

	public FileDeleteNotification(long fileID) {
		super.setResource("File");
		super.setMethod("Delete");
		super.setResourceID(fileID);
	}
}
