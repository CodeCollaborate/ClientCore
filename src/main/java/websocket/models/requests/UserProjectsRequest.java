package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class UserProjectsRequest implements IRequestData {
    public UserProjectsRequest() {
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("User", "Projects", this, responseHandler, requestSendErrorHandler);
    }
}
