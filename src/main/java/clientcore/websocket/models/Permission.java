package clientcore.websocket.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Permission {
    @JsonProperty("Username")
    private String username;

    @JsonProperty("PermissionLevel")
    private int permissionLevel;

    @JsonProperty("GrantedBy")
    private String grantedBy;

    @JsonProperty("GrantedDate")
    private String grantedDate;

    public Permission(@JsonProperty("Username") String username,
                      @JsonProperty("PermissionLevel") int permissionLevel,
                      @JsonProperty("GrantedBy") String grantedBy,
                      @JsonProperty("GrantedDate") String grantedDate) {
        super();
        this.username = username;
        this.permissionLevel = permissionLevel;
        this.grantedBy = grantedBy;
        this.grantedDate = grantedDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public String getGrantedDate() {
        return grantedDate;
    }

    public void setGrantedDate(String grantedDate) {
        this.grantedDate = grantedDate;
    }
}
