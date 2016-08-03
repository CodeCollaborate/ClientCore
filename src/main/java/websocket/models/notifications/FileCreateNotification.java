package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.File;
import websocket.models.Notification;

public class FileCreateNotification extends Notification {
	@JsonProperty("File")
	public File file;

	public FileCreateNotification(File file) {
		this.file = file;
		super.setResource("File");
		super.setMethod("Create");
		super.setResourceID(file.fileID);
	}
}
