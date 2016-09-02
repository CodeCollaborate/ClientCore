package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class FileDeleteRequest implements IRequestData {

    @JsonProperty("FileID")
    protected long fileID;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("File", "Delete", this,
                (response) -> {
                    System.out.println("Received file delete response: " + response);
                },
                () -> {
                    System.out.println("Failed to send file delete request to the server.");
                });
    }

    public FileDeleteRequest(long fileID) {
        this.fileID = fileID;
    }
}
