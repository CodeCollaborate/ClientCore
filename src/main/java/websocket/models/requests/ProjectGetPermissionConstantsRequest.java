package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectGetPermissionConstantsRequest implements IRequestData {

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("Project", "GetPermissionConstants", this,
                (response) -> {
                    System.out.println("Received project get permission constants response: " + response);
                },
                () -> {
                    System.out.println("Failed to send project get permission constants request to the server.");
                });
    }

    public ProjectGetPermissionConstantsRequest() {
    }
}
