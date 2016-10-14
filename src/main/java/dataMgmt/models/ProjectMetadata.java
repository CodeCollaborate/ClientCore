package dataMgmt.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.Permission;

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
    protected FileMetadata[] files;

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

}
