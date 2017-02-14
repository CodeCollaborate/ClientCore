package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.File;
import clientcore.websocket.models.IResponseData;

public class ProjectGetFilesResponse implements IResponseData {
    @JsonProperty("Files")
    public final File[] files;

    public ProjectGetFilesResponse(@JsonProperty("Files") File[] files) {
        this.files = files;
    }
}
