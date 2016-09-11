package websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IResponseData;

public class UserLoginResponse implements IResponseData {
    @JsonProperty("Token")
    protected String token;

    public String getToken() {
        return token;
    }
}
