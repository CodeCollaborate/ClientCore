package dataMgmt;

import com.google.common.collect.BiMap;
import websocket.models.Project;

import java.util.*;

/**
 * The storage unit for session data.
 * Created by fahslaj on 5/3/2016.
 */
public class SessionStorage extends Observable {

    // the username for the user
    private String username;

    // the authentication token for the user
    private String authenticationToken;

    // the list of loaded projects
    private HashMap<Long, Project> projects = new HashMap<>();

    // the map of users-project keys and their online status
    private Map<String, OnlineStatus> projectUserStatus;

    // the map of permission constants
    private BiMap<String, Byte> permissionConstants;

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
        this.username = username;
        setChanged();
        notifyObservers(username);
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
     * Get the current user's loaded projects.
     * @return projects list
     */
    public List<Project> getProjects() {
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
        this.projects = new HashMap<>();
        for (Project project : projects) {
            this.projects.put(project.getProjectID(), project);
        }
        setChanged();
        notifyObservers(projects);
    }

    /**
     * Add a project to the currently loaded projects. If one project already exists with the given id,
     * it will be overwritten.
     * @param project the project to add
     */
    public void addProject(Project project) {
        this.projects.put(project.getProjectID(), project);
        setChanged();
        notifyObservers(projects);
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
        this.permissionConstants = permissionConstants;
        setChanged();
        notifyObservers(permissionConstants);
    }
}
