package clientcore.websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.INotificationData;

public class FileRenameNotification implements INotificationData {
	@JsonProperty("NewName")
	public final String newName;

	public FileRenameNotification(@JsonProperty("NewName") String newName) {
		this.newName = newName;
	}
}
