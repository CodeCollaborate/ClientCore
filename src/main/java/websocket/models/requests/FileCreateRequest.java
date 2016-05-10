package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IRequestData;
import websocket.models.Request;

/**
 * Created by loganga on 5/9/2016
 */
public class FileCreateRequest implements IRequestData {

    @JsonProperty("Name")
    protected String name;

    @JsonProperty("RelativePath")
    protected String relativePath;

    @JsonProperty("ProjectID")
    protected long projectID;

    @JsonProperty("FileBytes")
    protected byte[] fileBytes;

    @JsonIgnore
    @Override
    public Request getRequest() {
        return new Request("File", "Create", this,
                (response) -> {
                    System.out.println("Received file create response: " + response);
                },
                () -> {
                    System.out.println("Failed to send file create request to the server.");
                });
    }

    public FileCreateRequest(String name, String relativePath, long projectID, byte[] fileBytes) {
        this.name = name;
        this.relativePath = relativePath;
        this.projectID = projectID;
        this.fileBytes = fileBytes;
    }
}
