package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectCreateRequest implements IRequestData {

    @JsonProperty("Name")
    protected String name;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "Create", this,
                (response) -> {
                    System.out.println("Received project create response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project create request to the server.");
                });
    }

    public ProjectCreateRequest(String name) {
        this.name = name;
    }
}
