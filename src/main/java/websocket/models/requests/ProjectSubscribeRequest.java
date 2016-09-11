package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectSubscribeRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "Subscribe", this,
                (response) -> {
                    System.out.println("Received project subscribe response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project subscribe request to the server.");
                });
    }

    public ProjectSubscribeRequest(long projectID) {
        this.projectID = projectID;
    }
}
