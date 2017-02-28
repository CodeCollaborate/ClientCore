package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class FileMoveRequest implements IRequestData {

    @JsonProperty("FileID")
    private long fileID;

    @JsonProperty("NewPath")
    private String newPath;

    public FileMoveRequest(long fileID, String newPath) {
        this.fileID = fileID;
        this.newPath = newPath;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("File", "Move", this, responseHandler, requestSendErrorHandler);
    }

    public long getFileID() {
        return fileID;
    }

    public String getNewPath() {
        return newPath;
    }
}
