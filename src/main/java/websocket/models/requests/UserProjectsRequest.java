package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class UserProjectsRequest implements IRequestData {
    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("User", "Projects", this,
                (response) -> {
                    System.out.println("Received user projects response: " + response);
                },
                () -> {
                    System.out.println("Failed to send user projects request to the server.");
                });
    }

    public UserProjectsRequest() {
    }
}