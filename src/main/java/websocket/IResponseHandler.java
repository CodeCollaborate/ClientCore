package websocket;

import websocket.models.Response;

/**
 * Created by fahslaj on 4/16/2016.
 */
public interface IResponseHandler {
    public void handleResponse(Response response);
}
