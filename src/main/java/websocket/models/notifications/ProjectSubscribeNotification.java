package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.INotificationData;

public class ProjectSubscribeNotification implements INotificationData {
	@JsonProperty("Username")
	public final String username;

	public ProjectSubscribeNotification(@JsonProperty("Username") String username) {
		this.username = username;
	}
}
