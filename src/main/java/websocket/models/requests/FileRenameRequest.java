package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("File", "Rename", this,
                (response) -> {
                    System.out.println("Received file rename response: " + response);
                },
                () -> {
                    System.out.println("Failed to send file rename request to the server.");
                });
    }

    public FileRenameRequest(long fileID, String newName) {
        this.fileID = fileID;
        this.newName = newName;
    }
}
