package websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.File;
import websocket.models.IResponseData;

public class ProjectGetFilesResponse implements IResponseData {
    @JsonProperty("Files")
    public final File[] files;

    public ProjectGetFilesResponse(@JsonProperty("Files") File[] files) {
        this.files = files;
    }
}
