package dataMgmt.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Paths;

/**
 * The structure for file metadata read from the project's metadata.
 * Created by fahslaj on 5/2/2016.
 */
public class FileMetadata {
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

    public String getFilePath(){
        return Paths.get(relativePath, filename).toString().replace('\\','/');
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
