package clientcore.websocket;

import clientcore.patching.Patch;
import clientcore.websocket.models.Notification;

public interface IFileChangeNotificationHandler {
    Long handleNotification(Notification notification, Patch[] originalChanges, Long expectedModificationStamp);
}
