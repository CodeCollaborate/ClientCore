package dataMgmt;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

/**
 * Created by fahslaj on 5/3/2016.
 */
public class SessionStorage extends Observable {

    private static SessionStorage instance;

    /**
     * Get the active instance of the SessionStorage class.
     * @return the instance of the SessionStorage class
     */
    public static SessionStorage getInstance() {
        if (instance == null) {
            synchronized (SessionStorage.class) {
                if (instance == null) {
                    instance = new SessionStorage();
                }
            }
        }
        return instance;
    }

    private String authenticationToken;
    private Map<String, OnlineStatus> projectUserStatus;

    public SessionStorage() {
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
        projectUserStatus.put(projectUserKey, status);
        notifyObservers();
    }

    // notifyObservers() is called to prompt the gui to change online displays
    public void removeProjectUserStatus(String projectUserKey) {
        projectUserStatus.remove(projectUserKey);
        notifyObservers();
    }
}
