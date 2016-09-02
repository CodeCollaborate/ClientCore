package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

public class UserLoginRequest implements IRequestData {

    @JsonProperty("Username")
    protected String username;

    @JsonProperty("Password")
    protected String password;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("User", "Login", this,
                (response) -> {
                    System.out.println("Received user login response: " + response);
                },
                () -> {
                    System.out.println("Failed to send user login request to the server.");
                });
    }

    public UserLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
