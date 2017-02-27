package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class FileDeleteRequest implements IRequestData {

    @JsonProperty("FileID")
    protected long fileID;

    public FileDeleteRequest(long fileID) {
        this.fileID = fileID;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("File", "Delete", this, responseHandler, requestSendErrorHandler);
    }

    public long getFileID() {
        return fileID;
    }
}
