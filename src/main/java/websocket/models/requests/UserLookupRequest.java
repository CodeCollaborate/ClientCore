package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

import java.util.List;

/**
 * Created by loganga on 5/9/2016
 */
public class UserLookupRequest implements IRequestData {

    @JsonProperty("Usernames")
    protected List<String> username;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("User", "Lookup", this,
                (response) -> {
                    System.out.println("Received user lookup response: " + response);
                },
                () -> {
                    System.out.println("Failed to send user lookup request to the server.");
                });
    }

    public UserLookupRequest(List<String> username) {
        this.username = username;
    }
}
