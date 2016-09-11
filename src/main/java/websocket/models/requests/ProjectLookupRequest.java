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
public class ProjectLookupRequest implements IRequestData {

    @JsonProperty("ProjectIDs")
    protected List<Long> projectIDs;

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

    public ProjectLookupRequest(List<Long> projectIDs) {
        this.projectIDs = projectIDs;
    }

    public ProjectLookupRequest(Long[] projectIDs) {
        this.projectIDs = Arrays.asList(projectIDs);
    }
}
