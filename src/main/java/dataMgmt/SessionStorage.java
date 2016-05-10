package dataMgmt;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

/**
 * The storage unit for session data.
 * Created by fahslaj on 5/3/2016.
 */
public class SessionStorage extends Observable {

    // the authentication token for the user
    private String authenticationToken;

    // the map of users-project keys and their online status
    private Map<String, OnlineStatus> projectUserStatus;

    /**
     * Create a new SessionStorage with an empty user status map.
     */
    protected SessionStorage() {
        projectUserStatus = new HashMap<>();
    }

    /**
     * Get the current user's authentication token.
     * @return authentication token
     */
    public String getAuthenticationToken() {
        return authenticationToken;
    }

    /**
     * Set the current user's authentication token.
     * @param authenticationToken the new authentication token
     */
    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    /**
     * Change the status of a user-project key in the status map and notify observers of this change.
     * @param projectUserKey the user-project combo to change
     * @param status the new status
     */
    // notifyObservers() is called to prompt the gui to change online displays
    public void changeProjectUserStatus(String projectUserKey, OnlineStatus status) {
        synchronized (this) {
            projectUserStatus.put(projectUserKey, status);
        }
        setChanged();
        notifyObservers(projectUserStatus);
    }

    /**
     * Remove the status of a user-project key from the map and notify observers of this change.
     * @param projectUserKey the user-project key of which to remove the value
     */
    // notifyObservers() is called to prompt the gui to change online displays
    public void removeProjectUserStatus(String projectUserKey) {
        synchronized (this) {
            projectUserStatus.remove(projectUserKey);
        }
        setChanged();
        notifyObservers(projectUserStatus);
    }
}
