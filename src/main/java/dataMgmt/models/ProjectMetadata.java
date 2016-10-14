package dataMgmt.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.Permission;

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

    @JsonProperty("Permissions")
    private HashMap<String, Permission> permissions;

    // Used in User.Projects - needs to use permissions eventually.
    @JsonProperty("PermissionLevel")
    private int permissionLevel;

    @JsonProperty("Files")
    private FileMetadata[] files;

    @JsonProperty("FileVersion")
    private long fileVersion;

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

    public HashMap<String, Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(HashMap<String, Permission> permissions) {
        this.permissions = permissions;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public FileMetadata[] getFiles() {
        return files;
    }

    public void setFiles(FileMetadata[] files) {
        this.files = files;
    }

    public long getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(long fileVersion) {
        this.fileVersion = fileVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMetadata)) return false;

        ProjectMetadata that = (ProjectMetadata) o;

        if (projectID != that.projectID) return false;
        if (permissionLevel != that.permissionLevel) return false;
        if (fileVersion != that.fileVersion) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (permissions != null ? !permissions.equals(that.permissions) : that.permissions != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(files, that.files);

    }

    @Override
    public int hashCode() {
        int result = (int) (projectID ^ (projectID >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (permissions != null ? permissions.hashCode() : 0);
        result = 31 * result + permissionLevel;
        result = 31 * result + Arrays.hashCode(files);
        result = 31 * result + (int) (fileVersion ^ (fileVersion >>> 32));
        return result;
    }
}
