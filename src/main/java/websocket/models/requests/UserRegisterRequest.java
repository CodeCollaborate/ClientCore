package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;


public class UserRegisterRequest implements IRequestData {

    @JsonProperty("Username")
    protected String username;

    @JsonProperty("FirstName")
    protected String firstName;

    @JsonProperty("LastName")
    protected String lastName;

    @JsonProperty("Email")
    protected String email;

    @JsonProperty("Password")
    protected String password;

    @JsonIgnore
	@Override
	public Request getRequest() {
		return new Request("User", "Register", this,
				(response) -> {
					System.out.println("Received user register response: " + response);
				},
				() -> {
					System.out.println("Failed to send user register request to server.");
				});
	}

    public UserRegisterRequest(String username, String firstName, String lastName, String email, String password) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }
}
