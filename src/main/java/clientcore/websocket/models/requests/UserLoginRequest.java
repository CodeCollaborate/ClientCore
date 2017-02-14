package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

public class UserLoginRequest implements IRequestData {

    @JsonProperty("Username")
    protected String username;

    @JsonProperty("Password")
    protected String password;

    public UserLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("User", "Login", this, responseHandler, requestSendErrorHandler);
    }
}
