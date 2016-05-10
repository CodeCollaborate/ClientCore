package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectRenameRequest implements IRequestData {

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonProperty("NewName")
    protected String newName;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "Rename", this,
                (response) -> {
                    System.out.println("Received project rename response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project rename request to the server.");
                });
    }

    public ProjectRenameRequest(long projectID, String newName) {
        this.projectID = projectID;
        this.newName = newName;
    }
}
