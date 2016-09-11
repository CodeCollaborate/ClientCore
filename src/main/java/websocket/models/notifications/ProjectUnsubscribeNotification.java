package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.INotificationData;

public class ProjectUnsubscribeNotification implements INotificationData {
    @JsonProperty("Username")
    public final String username;

    public ProjectUnsubscribeNotification(@JsonProperty("Username") String username) {
        this.username = username;
    }
}
