package dataMgmt;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import websocket.models.Project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * The storage unit for session data.
 * Created by fahslaj on 5/3/2016.
 */
public class SessionStorage {

    public static final String USERNAME = "username";
    public static final String AUTH_TOKEN = "authtoken";
    public static final String PROJECT_LIST = "projectlist";
    public static final String PROJECT_USER_STATUS = "projectuserstatus";
    public static final String PERMISSION_CONSTANTS = "permissionconstants";

    // the username for the user
    private String username;

    // the authentication token for the user
    private String authenticationToken;

    // the list of loaded projects
    private HashMap<Long, Project> projects = new HashMap<>();

    // the map of users-project keys and their online status
    private Map<String, OnlineStatus> projectUserStatus = new HashMap<>();

    // the map of permission constants
    private BiMap<String, Byte> permissionConstants = HashBiMap.create();

    // list of listeners for this class's properties
    private final List<PropertyChangeListener> listeners = new ArrayList<>();

    /**
     * Create a new SessionStorage with an empty user status map.
     */
    protected SessionStorage() {
        projectUserStatus = new HashMap<>();
    }

    /**
     * Get the current user's username.
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the current user's username.
     * @param username
     */
    public void setUsername(String username) {
        String oldValue = this.username;
        this.username = username;
        notifyListeners(USERNAME, oldValue, this.username);
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
        String oldValue = this.authenticationToken;
        this.authenticationToken = authenticationToken;
        notifyListeners(AUTH_TOKEN, oldValue, this.authenticationToken);
    }

    /**
     * Get the current user's loaded projects.
     * @return projects list
     */
    public List<Project> getProjects() {
        return new ArrayList<>(this.projects.values());
    }

    /**
     * Get the current user's loaded projects sorted by name.
     * @return projects list
     */
    public List<Project> getSortedProjects() {
        List<Project> projects = new ArrayList<>(this.projects.values());
        Collections.sort(projects, (o1, o2) -> {
            if (o1.getName() == null) {
                return 1;
            } else if (o2.getName() == null) {
                return -1;
            } else {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return projects;
    }

    /**
     * Get a specific project by its id.
     * @param projectId the id with which to look up a project
     * @return the project with the specified projectId, or null if there is none
     */
    public Project getProjectById(long projectId) {
        return this.projects.get(projectId);
    }

    /**
     * Set the current user's loaded projects.
     * @param projects the list of projects to set
     */
    public void setProjects(List<Project> projects) {
        List<Project> oldValue = getProjects();
        this.projects = new HashMap<>();
        for (Project project : projects) {
            this.projects.put(project.getProjectID(), project);
        }
        notifyListeners(PROJECT_LIST, oldValue, getProjects());
    }

    /**
     * Add a project to the currently loaded projects. If one project already exists with the given id,
     * it will be overwritten.
     * @param project the project to add
     */
    public void setProject(Project project) {
        this.projects.put(project.getProjectID(), project);
        notifyListeners(PROJECT_LIST, null, project);
    }

    /**
     * Remove a project by its id, if it exists.
     * @param id the id of the project to remove
     */
    public void removeProjectById(long id) {
        Project old = this.projects.remove(id);
        notifyListeners(PROJECT_LIST, old, null);
    }

    /**
     * Change the status of a user-project key in the status map and notify observers of this change.
     * @param projectUserKey the user-project combo to change
     * @param status the new status
     */
    public void changeProjectUserStatus(String projectUserKey, OnlineStatus status) {
        AbstractMap.SimpleEntry<String, OnlineStatus> oldValue;
        synchronized (this) {
            oldValue = new AbstractMap.SimpleEntry<>(projectUserKey, projectUserStatus.get(projectUserKey));
            projectUserStatus.put(projectUserKey, status);
        }
        notifyListeners(PROJECT_USER_STATUS, oldValue,
                new AbstractMap.SimpleEntry<>(projectUserKey, projectUserStatus.get(projectUserKey)));
    }

    /**
     * Remove the status of a user-project key from the map and notify observers of this change.
     * @param projectUserKey the user-project key of which to remove the value
     */
    public void removeProjectUserStatus(String projectUserKey) {
        OnlineStatus status;
        synchronized (this) {
            status = projectUserStatus.remove(projectUserKey);
        }
        notifyListeners(PROJECT_USER_STATUS, projectUserKey, null);
    }

    /**
     * Get the BiMap of permission constants.
     * @return the permission constants
     */
    public BiMap<String, Byte> getPermissionConstants() {
        return permissionConstants;
    }

    /**
     * Set the permission constants map.
     * @param permissionConstants permission constants BiMap to set
     */
    public void setPermissionConstants(BiMap<String, Byte> permissionConstants) {
        BiMap<String, Byte> oldValue = this.permissionConstants;
        this.permissionConstants = permissionConstants;
        notifyListeners(PERMISSION_CONSTANTS, oldValue, this.permissionConstants);
    }

    private void notifyListeners(String identifier, Object oldValue, Object newValue) {
        synchronized(this.listeners) {
            for (PropertyChangeListener listener : this.listeners) {
                listener.propertyChange(new PropertyChangeEvent(this, identifier, oldValue, newValue));
            }
        }
    }

    /**
     * Add a property change listener
     * @param listener listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        synchronized(this.listeners) {
            this.listeners.add(listener);
        }
    }

    /**
     * Remove a property change listener
     * @param listener listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        synchronized(this.listeners) {
            this.listeners.remove(listener);
        }
    }
}
