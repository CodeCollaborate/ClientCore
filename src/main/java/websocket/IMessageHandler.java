package websocket;

/**
 * Created by Benedict on 4/15/2016.
 */
public interface IMessageHandler {

    void handleMessage(String message);

    void handleMessageSendError(String message);
}
