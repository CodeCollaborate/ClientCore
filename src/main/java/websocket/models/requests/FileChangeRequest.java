package websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import websocket.models.IRequestData;
import websocket.models.Request;

public class FileChangeRequest implements IRequestData {

	@JsonProperty("FileID")
	protected long fileID;
	
	@JsonProperty("Changes")
	protected String[] changes;
	
	@JsonProperty("BaseFileVersion")
	protected long baseFileVersion;
	
	@JsonIgnore
	@Override
	public Request getRequest() {
		// TODO Auto-generated method stub
		return new Request("File", "Change", this, 
				(response) -> {
                    System.out.println("Received file change response: " + response);
                },
                () -> {
                    System.out.println("Failed to send file change request to the server.");
                }) {
			
		};
	}
	
	public FileChangeRequest(long fileID, String[] changes, long baseFileVersion) {
        this.fileID = fileID;
        this.changes = changes;
        this.baseFileVersion = baseFileVersion;
    }
}
