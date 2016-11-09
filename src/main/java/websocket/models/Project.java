package websocket.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;

public class Project {
    @JsonProperty("ProjectID")
    private long projectID;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Permissions")
    private HashMap<String, Permission> permissions;

    public Project(@JsonProperty("ProjectID") long projectID,
                   @JsonProperty("Name") String name,
                   @JsonProperty("Permissions") HashMap<String, Permission> permissions) {
        super();
        this.projectID = projectID;
        this.name = name;
        this.permissions = permissions;
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

    public HashMap<String, Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(HashMap<String, Permission> permissions) {
        this.permissions = permissions;
    }
}
