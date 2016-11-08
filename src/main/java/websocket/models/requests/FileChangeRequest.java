package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Request;

public class FileChangeRequest implements IRequestData {

    @JsonProperty("FileID")
    protected long fileID;

    @JsonProperty("Changes")
    protected String[] changes;

    @JsonProperty("BaseFileVersion")
    protected long baseFileVersion;

    public FileChangeRequest(long fileID, String[] changes, long baseFileVersion) {
        this.fileID = fileID;
        this.changes = changes;
        this.baseFileVersion = baseFileVersion;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        // TODO Auto-generated method stub
        return new Request("File", "Change", this, responseHandler, requestSendErrorHandler);
    }

    public long getFileID() {
        return fileID;
    }

    public String[] getChanges() {
        return changes;
    }

    public void setChanges(String[] changes) {
        this.changes = changes;
    }

    public long getBaseFileVersion() {
        return baseFileVersion;
    }

    public void setBaseFileVersion(long baseFileVersion) {
        this.baseFileVersion = baseFileVersion;
    }
}
