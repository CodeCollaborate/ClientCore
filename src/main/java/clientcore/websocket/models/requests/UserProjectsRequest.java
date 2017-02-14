package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

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
