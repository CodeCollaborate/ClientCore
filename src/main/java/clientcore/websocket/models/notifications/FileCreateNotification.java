package clientcore.websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.File;
import clientcore.websocket.models.INotificationData;

public class FileCreateNotification implements INotificationData {
    @JsonProperty("File")
    public final File file;

    public FileCreateNotification(@JsonProperty("File") File file) {
        this.file = file;
    }
}
