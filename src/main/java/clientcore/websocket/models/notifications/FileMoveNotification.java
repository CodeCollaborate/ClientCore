package clientcore.websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.INotificationData;

public class FileMoveNotification implements INotificationData {
	@JsonProperty("NewPath")
	public final String newPath;

	public FileMoveNotification(@JsonProperty("NewPath") String newPath) {
		this.newPath = newPath;
	}
}
