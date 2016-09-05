package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.INotificationData;

public class ProjectRevokePermissionsNotification implements INotificationData {
	@JsonProperty("RevokeUsername")
	public final String revokeUsername;

	public ProjectRevokePermissionsNotification(@JsonProperty("RevokeUsername") String revokeUsername) {
		this.revokeUsername = revokeUsername;
	}
}
