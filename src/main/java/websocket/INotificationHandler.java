package websocket;

import websocket.models.Notification;

/**
 * Created by fahslaj on 4/16/2016.
 */
public interface INotificationHandler {
    public void handleNotification(Notification notification);
}
