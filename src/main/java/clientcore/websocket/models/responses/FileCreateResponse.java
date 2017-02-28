package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class FileCreateResponse implements IResponseData{
    @JsonProperty("FileID")
    public final int fileID;

    public FileCreateResponse(@JsonProperty("FileID") int fileID) {
        this.fileID = fileID;
    }
}
