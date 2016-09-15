package websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IResponseData;
import websocket.models.Project;

/**
 * Created by Benedict on 9/3/2016.
 */
public class ProjectLookupResponse implements IResponseData {
    @JsonProperty("Projects")
    protected Project[] projects;

    public Project[] getProjects() {
        return projects;
    }
}
