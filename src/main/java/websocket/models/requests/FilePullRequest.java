package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class FilePullRequest implements IRequestData {

    @JsonProperty("FileID")
    protected long fileID;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("File", "Pull", this,
                (response) -> {
                    System.out.println("Received file pull response: " + response);
                },
                () -> {
                    System.out.println("Failed to send file pull request to the server.");
                });
    }

    public FilePullRequest(long fileID) {
        this.fileID = fileID;
    }
}
