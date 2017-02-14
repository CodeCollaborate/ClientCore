package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

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
