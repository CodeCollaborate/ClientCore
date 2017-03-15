package clientcore.websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.INotificationData;

public class ProjectGrantPermissionsNotification implements INotificationData {
    @JsonProperty("GrantUsername")
    public final String grantUsername;

    @JsonProperty("PermissionLevel")
    public final int permissionLevel;

    public ProjectGrantPermissionsNotification(@JsonProperty("GrantUsername") String grantUsername,
                                               @JsonProperty("PermissionLevel") int permissionLevel) {
        this.grantUsername = grantUsername;
        this.permissionLevel = permissionLevel;
    }
}
