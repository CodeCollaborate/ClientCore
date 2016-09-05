package websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class ProjectCreateResponse implements IResponseData{
    @JsonProperty("ProjectID")
    protected int projectID;

    public int getProjectID() {
        return projectID;
    }
}
