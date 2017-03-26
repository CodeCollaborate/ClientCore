package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class FileChangeResponse implements IResponseData {
    @JsonProperty("FileVersion")
    public final long fileVersion;

    @JsonProperty("Changes")
    public final String changes;

    @JsonProperty("MissingPatches")
    public final String[] missingPatches;

    public FileChangeResponse(@JsonProperty("FileVersion") int fileVersion,
                              @JsonProperty("Changes") String changes,
                              @JsonProperty("MissingPatches") String[] missingPatches) {
        this.fileVersion = fileVersion;
        this.changes = changes;
        this.missingPatches = missingPatches;
    }
}
