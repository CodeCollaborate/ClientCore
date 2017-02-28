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
public class FileCreateRequest implements IRequestData {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("RelativePath")
    private String relativePath;

    @JsonProperty("ProjectID")
    private long projectID;

    @JsonProperty("FileBytes")
    private byte[] fileBytes;

    public FileCreateRequest(String name, String relativePath, long projectID, byte[] fileBytes) {
        this.name = name;
        this.relativePath = relativePath;
        this.projectID = projectID;
        this.fileBytes = fileBytes;
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("File", "Create", this, responseHandler, requestSendErrorHandler);
    }

    public String getName() {
        return name;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getProjectID() {
        return projectID;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }
}
