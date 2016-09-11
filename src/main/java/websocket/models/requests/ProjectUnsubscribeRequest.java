package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectUnsubscribeRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "Unsubscribe", this,
                (response) -> {
                    System.out.println("Received project unsubscribe response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project unsubscribe request to the server.");
                });
    }

    public ProjectUnsubscribeRequest(long projectID) {
        this.projectID = projectID;
    }
}
