package websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IResponseData;

/**
 * Created by Benedict on 9/3/2016.
 */
public class FileChangeResponse implements IResponseData{
    @JsonProperty("FileVersion")
    protected int fileVersion;

    @JsonProperty("MissingPatches")
    protected String[] missingPatches;

    public int getFileVersion() {
        return fileVersion;
    }

    public String[] getMissingPatches() {
        return missingPatches;
    }
}
