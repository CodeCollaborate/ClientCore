package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("File", "Move", this,
                (response) -> {
                    System.out.println("Received file move response: " + response);
                },
                () -> {
                    System.out.println("Failed to send file move request to the server.");
                });
    }

    public FileMoveRequest(long fileID, String newPath) {
        this.fileID = fileID;
        this.newPath = newPath;
    }
}
