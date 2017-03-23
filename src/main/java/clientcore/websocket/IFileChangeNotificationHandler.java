package clientcore.websocket;

import clientcore.websocket.models.Notification;

public interface IFileChangeNotificationHandler {
    Long handleNotification(Notification notification, Long expectedModificationStamp);
}
