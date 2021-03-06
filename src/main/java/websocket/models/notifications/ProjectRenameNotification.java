package websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.INotificationData;

public class ProjectRenameNotification implements INotificationData {
    @JsonProperty("NewName")
    public final String newName;

    public ProjectRenameNotification(@JsonProperty("NewName") String newName) {
        this.newName = newName;
    }
}
