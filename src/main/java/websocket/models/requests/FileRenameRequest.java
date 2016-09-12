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
public class FileRenameRequest implements IRequestData {

    @JsonProperty("FileID")
    protected long fileID;

    @JsonProperty("NewName")
    protected String newName;

    public FileRenameRequest(long fileID, String newName) {
        this.fileID = fileID;
        this.newName = newName;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("File", "Rename", this, responseHandler, requestSendErrorHandler);
    }
}
