package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class FileMoveNotification extends Notification {
	@JsonProperty("NewPath")
	public String newPath;

	public FileMoveNotification(String newPath, long fileID) {
		this.newPath = newPath;
		super.setResource("File");
		super.setMethod("Move");
		super.setResourceID(fileID);
	}
}
