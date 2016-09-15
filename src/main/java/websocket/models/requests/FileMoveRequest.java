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
public class FileMoveRequest implements IRequestData {

    @JsonProperty("FileID")
    protected long fileID;

    @JsonProperty("NewPath")
    protected String newPath;

    public FileMoveRequest(long fileID, String newPath) {
        this.fileID = fileID;
        this.newPath = newPath;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("File", "Move", this, responseHandler, requestSendErrorHandler);
    }
}
