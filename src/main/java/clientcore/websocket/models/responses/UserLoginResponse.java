package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;

public class UserLoginResponse implements IResponseData {
    @JsonProperty("Token")
    protected String token;

    public String getToken() {
        return token;
    }
}
