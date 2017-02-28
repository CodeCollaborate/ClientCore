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
public class FilePullRequest implements IRequestData {

    @JsonProperty("FileID")
    private long fileID;

    public FilePullRequest(long fileID) {
        this.fileID = fileID;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("File", "Pull", this, responseHandler, requestSendErrorHandler);
    }

    public long getFileID() {
        return fileID;
    }
}
