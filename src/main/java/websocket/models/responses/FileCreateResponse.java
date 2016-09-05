package websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class FileCreateResponse implements IResponseData{
    @JsonProperty("FileID")
    protected int fileID;

    public int getFileID() {
        return fileID;
    }
}
