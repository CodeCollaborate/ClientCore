package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class FileRenameNotification extends Notification {
	@JsonProperty("NewName")
	public String newName;

	public FileRenameNotification(String newName, long fileID) {
		this.newName = newName;
		super.setResource("File");
		super.setMethod("Rename");
		super.setResourceID(fileID);
	}
}
