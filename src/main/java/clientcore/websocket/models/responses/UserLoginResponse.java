package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;

public class UserLoginResponse implements IResponseData {
    @JsonProperty("Token")
    public final String token;

    public UserLoginResponse(@JsonProperty("Token") String token) {
        this.token = token;
    }
}
