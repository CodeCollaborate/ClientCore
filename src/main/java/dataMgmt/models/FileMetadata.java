package dataMgmt.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private long version;

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

    @JsonIgnore
    public String getFilePath(){
        return Paths.get(relativePath, filename).toString().replace('\\','/');
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileMetadata)) return false;

        FileMetadata that = (FileMetadata) o;

        if (fileID != that.fileID) return false;
        if (version != that.version) return false;
        if (filename != null ? !filename.equals(that.filename) : that.filename != null) return false;
        if (relativePath != null ? !relativePath.equals(that.relativePath) : that.relativePath != null) return false;
        if (creator != null ? !creator.equals(that.creator) : that.creator != null) return false;
        return creationDate != null ? creationDate.equals(that.creationDate) : that.creationDate == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (fileID ^ (fileID >>> 32));
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + (relativePath != null ? relativePath.hashCode() : 0);
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + (creator != null ? creator.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        return result;
    }
}
