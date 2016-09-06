package websocket.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class File {
    @JsonProperty("FileID")
    private long fileID;

    @JsonProperty("Filename")
    private String filename;

    @JsonProperty("RelativePath")
    private String relativePath;

    @JsonProperty("Version")
    private long fileVersion;

    @JsonProperty("Creator")
    private String creator;

    @JsonProperty("CreationDate")
    private String creationDate;

    public File(@JsonProperty("FileID") long fileID,
                @JsonProperty("Filename") String filename,
                @JsonProperty("RelativePath") String relativePath,
                @JsonProperty("Version") long fileVersion,
                @JsonProperty("Creator") String creator,
                @JsonProperty("CreationDate") String creationDate) {
        super();
        this.fileID = fileID;
        this.filename = filename;
        this.relativePath = relativePath;
        this.fileVersion = fileVersion;
        this.creator = creator;
        this.creationDate = creationDate;
    }

    public long getFileID() {
        return fileID;
    }

    public void setFileID(long fileID) {
        this.fileID = fileID;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public long getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(long fileVersion) {
        this.fileVersion = fileVersion;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
}
