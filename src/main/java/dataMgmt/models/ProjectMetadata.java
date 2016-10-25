package dataMgmt.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.Permission;
import websocket.models.Project;

import java.util.Arrays;
import java.util.HashMap;

/**
 * The structure for project metadata read from the project's root.
 * Created by fahslaj on 5/2/2016.
 */
public class ProjectMetadata {
    @JsonProperty("ProjectID")
    private long projectID;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Files")
    private FileMetadata[] files;

    public ProjectMetadata(Project p) {
        this.projectID = p.getProjectID();
        this.name = p.getName();
    }

    public ProjectMetadata(Project p, FileMetadata[] files) {
        this.projectID = p.getProjectID();
        this.name = p.getName();
        this.files = files;
    }

    public long getProjectID() {
        return projectID;
    }

    public void setProjectID(long projectID) {
        this.projectID = projectID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        if (!(o instanceof ProjectMetadata)) return false;

        ProjectMetadata that = (ProjectMetadata) o;

        if (projectID != that.projectID) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(files, that.files);

    }

    @Override
    public int hashCode() {
        int result = (int) (projectID ^ (projectID >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(files);
        return result;
    }
}
