package dataMgmt.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;

/**
 * Created by fahslaj on 5/2/2016.
 */
public class ProjectMetadata {

    @JsonProperty("ProjectID")
    protected long projectId;

    @JsonProperty("Name")
    protected String name;

    @JsonProperty("Owner")
    protected String owner;

    @JsonProperty("Files")
    protected FileMetadata[] files;

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public FileMetadata[] getFiles() {
        return files;
    }

    public void setFiles(FileMetadata[] files) {
        this.files = files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectMetadata metadata = (ProjectMetadata) o;

        if (getProjectId() != metadata.getProjectId()) return false;
        if (getName() != null ? !getName().equals(metadata.getName()) : metadata.getName() != null) return false;
        if (getOwner() != null ? !getOwner().equals(metadata.getOwner()) : metadata.getOwner() != null) return false;
        return Arrays.equals(getFiles(), metadata.getFiles());

    }

    @Override
    public int hashCode() {
        int result = (int) (getProjectId() ^ (getProjectId() >>> 32));
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        result = 31 * result + (getOwner() != null ? getOwner().hashCode() : 0);
        result = 31 * result + Arrays.hashCode(getFiles());
        return result;
    }
}
