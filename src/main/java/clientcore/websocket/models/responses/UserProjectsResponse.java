package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;
import clientcore.websocket.models.Project;

/**
 * Created by Benedict on 9/3/2016.
 */
public class UserProjectsResponse implements IResponseData {
    @JsonProperty("Projects")
    public final Project[] projects;

    public UserProjectsResponse(@JsonProperty("Projects")Project[] projects) {
        this.projects = projects;
    }
}
