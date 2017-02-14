package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;
import clientcore.websocket.models.Project;

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
