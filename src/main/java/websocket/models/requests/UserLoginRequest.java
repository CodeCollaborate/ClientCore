package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Request;

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
