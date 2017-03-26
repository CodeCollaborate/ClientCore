package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

public class FileChangeRequest implements IRequestData {

    @JsonProperty("FileID")
    private long fileID;

    @JsonProperty("Changes")
    private String changes;

    public FileChangeRequest(long fileID, String changes) {
        this.fileID = fileID;
        this.changes = changes;
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

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }
}
