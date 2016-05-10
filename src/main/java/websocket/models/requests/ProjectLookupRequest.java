package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectLookupRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "Lookup", this,
                (response) -> {
                    System.out.println("Received project lookup response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project lookup request to the server.");
                });
    }

    public ProjectLookupRequest(long projectID) {
        this.projectID = projectID;
    }
}
