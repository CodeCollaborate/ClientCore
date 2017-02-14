package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

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
