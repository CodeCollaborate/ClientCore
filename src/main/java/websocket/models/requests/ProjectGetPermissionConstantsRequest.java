package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectGetPermissionConstantsRequest implements IRequestData {

    public ProjectGetPermissionConstantsRequest() {
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "GetPermissionConstants", this, responseHandler, requestSendErrorHandler);
    }
}
