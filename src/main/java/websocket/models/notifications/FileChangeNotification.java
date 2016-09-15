package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.INotificationData;

public class FileChangeNotification implements INotificationData {
	@JsonProperty("Changes")
	public final String[] changes;

	@JsonProperty("FileVersion")
	public final long fileVersion;

	@JsonProperty("BaseFileVersion")
	public final long baseFileVersion;

	public FileChangeNotification(@JsonProperty("Changes") String[] changes,
								  @JsonProperty("FileVersion") long fileVersion,
								  @JsonProperty("BaseFileVersion") long baseFileVersion) {
		this.changes = changes;
		this.fileVersion = fileVersion;
		this.baseFileVersion = baseFileVersion;
	}
}
