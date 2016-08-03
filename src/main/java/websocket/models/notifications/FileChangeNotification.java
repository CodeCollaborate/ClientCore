package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.Notification;

public class FileChangeNotification extends Notification {
	@JsonProperty("Changes")
	public String[] changes;

	@JsonProperty("FileVersion")
	public long fileVersion;

	@JsonProperty("BaseFileVersion")
	public long baseFileVersion;

	public FileChangeNotification(String[] changes, long fileVersion, long baseFileVersion, long fileID) {
		this.changes = changes;
		super.setResource("File");
		super.setMethod("Move");
		super.setResourceID(fileID);
	}
}
