package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class FilePullResponse implements IResponseData {
    @JsonProperty("FileBytes")
    protected byte[] fileBytes;

    @JsonProperty("Changes")
    protected String[] changes;

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public String[] getChanges() {
        return changes;
    }
}
