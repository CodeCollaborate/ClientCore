package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class ProjectCreateResponse implements IResponseData{
    @JsonProperty("ProjectID")
    public final long projectID;

    public ProjectCreateResponse(@JsonProperty("ProjectID") long projectID) {
        this.projectID = projectID;
    }
}