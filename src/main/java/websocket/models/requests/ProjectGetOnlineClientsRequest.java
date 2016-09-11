package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectGetOnlineClientsRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    public ProjectGetOnlineClientsRequest(long projectID) {
        this.projectID = projectID;
    }

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "GetOnlineClients", this,
                (response) -> {
                    System.out.println("Received project get online clients response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project get online clients request to the server.");
                });
    }
}
