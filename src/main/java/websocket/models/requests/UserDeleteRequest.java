package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * @author Joel Shapiro on 10/10/16
 */
public class UserDeleteRequest implements IRequestData {

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("User", "Delete", this, responseHandler, requestSendErrorHandler);
    }
}
