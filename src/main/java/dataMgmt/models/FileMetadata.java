package dataMgmt.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by fahslaj on 5/2/2016.
 */
public class FileMetadata {

    @JsonProperty("FileID")
    protected long fileId;

    @JsonProperty("FilePath")
    protected String filePath;

    @JsonProperty("Version")
    protected long version;

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileMetadata that = (FileMetadata) o;

        if (getFileId() != that.getFileId()) return false;
        if (getVersion() != that.getVersion()) return false;
        return getFilePath() != null ? getFilePath().equals(that.getFilePath()) : that.getFilePath() == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (getFileId() ^ (getFileId() >>> 32));
        result = 31 * result + (getFilePath() != null ? getFilePath().hashCode() : 0);
        result = 31 * result + (int) (getVersion() ^ (getVersion() >>> 32));
        return result;
    }
}
