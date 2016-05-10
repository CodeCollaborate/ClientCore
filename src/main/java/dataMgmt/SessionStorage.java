package dataMgmt;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

/**
 * Created by fahslaj on 5/3/2016.
 */
public class SessionStorage extends Observable {

    private String authenticationToken;
    private Map<String, OnlineStatus> projectUserStatus;

    protected SessionStorage() {
        projectUserStatus = new HashMap<>();
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    // notifyObservers() is called to prompt the gui to change online displays
    public void changeProjectUserStatus(String projectUserKey, OnlineStatus status) {
        synchronized (this) {
            projectUserStatus.put(projectUserKey, status);
        }
        setChanged();
        notifyObservers(projectUserStatus);
    }

    // notifyObservers() is called to prompt the gui to change online displays
    public void removeProjectUserStatus(String projectUserKey) {
        synchronized (this) {
            projectUserStatus.remove(projectUserKey);
        }
        setChanged();
        notifyObservers(projectUserStatus);
    }
}
