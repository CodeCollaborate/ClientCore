package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

import java.util.Arrays;
import java.util.List;

/**
 * Created by loganga on 5/9/2016
 */
public class UserLookupRequest implements IRequestData {

    @JsonProperty("Usernames")
    protected List<String> usernames;

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

    public UserLookupRequest(List<String> usernames) {
        this.usernames = usernames;
    }

    public UserLookupRequest(String[] usernames) {
        this.usernames = Arrays.asList(usernames);
    }
}
