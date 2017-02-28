package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class FilePullResponse implements IResponseData {
    @JsonProperty("FileBytes")
    public final byte[] fileBytes;

    @JsonProperty("Changes")
    public final String[] changes;

    public FilePullResponse(@JsonProperty("FileBytes") byte[] fileBytes,
                            @JsonProperty("Changes") String[] changes) {
        this.fileBytes = fileBytes;
        this.changes = changes;
    }
}
