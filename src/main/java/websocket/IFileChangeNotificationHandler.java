package websocket;

import websocket.models.Notification;

public interface IFileChangeNotificationHandler {
    Long handleNotification(Notification notification, long expectedModificationStamp);
}
