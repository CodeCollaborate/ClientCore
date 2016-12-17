package dataMgmt;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import websocket.models.Project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The storage unit for session data.
 * Created by fahslaj on 5/3/2016.
 */
public class SessionStorage {

    public static final String USERNAME = "username";
    public static final String AUTH_TOKEN = "authtoken";
    public static final String PROJECT_LIST = "projectlist";
    public static final String SUBSCRIBED_PROJECTS = "subscribedlist";
    public static final String PROJECT_USER_STATUS = "projectuserstatus";
    public static final String PERMISSION_CONSTANTS = "permissionconstants";

    // the username for the user
    private String username;

    // the authentication token for the user
    private String authenticationToken;

    // the list of loaded projects
    private HashMap<Long, Project> projects = new HashMap<>();

    // the set of subscribed project ids
    private HashSet<Long> subscribedIds = new HashSet<>();

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
        String name;
        synchronized (USERNAME) {
            name = username;
        }
        return name;
    }

    /**
     * Set the current user's username.
     * @param username
     */
    public void setUsername(String username) {
        String oldValue;
        synchronized (USERNAME) {
            oldValue = this.username;
            this.username = username;
        }
        notifyListeners(USERNAME, oldValue, username);
    }

    /**
     * Get the current user's authentication token.
     * @return authentication token
     */
    public String getAuthenticationToken() {
        String authToken;
        synchronized (AUTH_TOKEN) {
            authToken = authenticationToken;
        }
        return authToken;
    }

    /**
     * Set the current user's authentication token.
     * @param authenticationToken the new authentication token
     */
    public void setAuthenticationToken(String authenticationToken) {
        String oldValue;
        synchronized (AUTH_TOKEN) {
            oldValue = this.authenticationToken;
            this.authenticationToken = authenticationToken;
        }
        notifyListeners(AUTH_TOKEN, oldValue, authenticationToken);
    }

    /**
     * Get the current user's loaded projects.
     * @return projects list
     */
    public List<Project> getProjects() {
        Collection<Project> collection;
        synchronized (PROJECT_LIST) {
            collection = this.projects.values();
        }
        return new ArrayList<>(collection);
    }

    /**
     * Get the current user's loaded projects sorted by name.
     * @return projects list
     */
    public List<Project> getSortedProjects() {
        List<Project> projects = getProjects();
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
        Project project;
        synchronized (PROJECT_LIST) {
            project = this.projects.get(projectId);
        }
        return project;
    }

    /**
     * Set the current user's loaded projects.
     * @param projects the list of projects to set
     */
    public void setProjects(List<Project> projects) {
        List<Project> oldValue;
        List<Project> newList;
        synchronized (PROJECT_LIST) {
            oldValue = new ArrayList<>(this.projects.values());
            this.projects = new HashMap<>();
            for (Project project : projects) {
                this.projects.put(project.getProjectID(), project);
            }
            newList = new ArrayList<>(this.projects.values());
        }
        notifyListeners(PROJECT_LIST, oldValue, newList);
    }

    /**
     * Add a project to the currently loaded projects. If one project already exists with the given id,
     * it will be overwritten.
     * @param project the project to add
     */
    public void setProject(Project project) {
        synchronized (PROJECT_LIST) {
            this.projects.put(project.getProjectID(), project);
        }
        notifyListeners(PROJECT_LIST, null, project);
    }

    /**
     * Remove a project by its id, if it exists.
     * @param id the id of the project to remove
     */
    public void removeProjectById(long id) {
        Project old;
        synchronized (PROJECT_LIST) {
            old = this.projects.remove(id);
        }
        notifyListeners(PROJECT_LIST, old, null);
    }

    /**
     * Set the given project id as a subscribed project, if it exists.
     * @param id to set subscribed
     */
    public void setSubscribed(long id) {
        boolean notify = false;
        synchronized (SUBSCRIBED_PROJECTS) {
            if (this.projects.containsKey(id)) {
                this.subscribedIds.add(id);
                notify = true;
            }
        }
        if (notify) {
            notifyListeners(SUBSCRIBED_PROJECTS, null, id);
        }
    }

    /**
     * Remove the given project id from the set of subscribed ids, if it exists.
     * @param id to remove from subscribed set
     */
    public void setUnsubscribed(long id) {
        boolean notify = false;
        synchronized (SUBSCRIBED_PROJECTS) {
            if (this.projects.containsKey(id)) {
                this.subscribedIds.remove(id);
                notify = true;
            }
        }
        if (notify) {
            notifyListeners(SUBSCRIBED_PROJECTS, id, null);
        }
    }

    /**
     * Get the set of subscribed ids
     * @return set of subscribed ids
     */
    public Set<Long> getSubscribedIds() {
        Set<Long> ids;
        synchronized (SUBSCRIBED_PROJECTS) {
            ids = this.subscribedIds;
        }
        return ids;
    }

    /**
     * Change the status of a user-project key in the status map and notify observers of this change.
     * @param projectUserKey the user-project combo to change
     * @param status the new status
     */
    public void changeProjectUserStatus(String projectUserKey, OnlineStatus status) {
        OnlineStatusKeyPair oldValue;
        synchronized (this) {
            oldValue = new OnlineStatusKeyPair(projectUserKey, projectUserStatus.get(projectUserKey));
            projectUserStatus.put(projectUserKey, status);
        }
        notifyListeners(PROJECT_USER_STATUS, oldValue,
                new OnlineStatusKeyPair(projectUserKey, status));
    }

    class OnlineStatusKeyPair {
        String projectUserKey;
        OnlineStatus onlineStatus;

        public OnlineStatusKeyPair(String projectUserKey, OnlineStatus onlineStatus) {
            this.projectUserKey = projectUserKey;
            this.onlineStatus = onlineStatus;
        }

        public String getProjectUserKey() {
            return projectUserKey;
        }

        public OnlineStatus getOnlineStatus() {
            return onlineStatus;
        }
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
        BiMap<String, Byte> constants;
        synchronized (PERMISSION_CONSTANTS) {
            constants = permissionConstants;
        }
        return constants;
    }

    /**
     * Set the permission constants map.
     * @param permissionConstants permission constants BiMap to set
     */
    public void setPermissionConstants(BiMap<String, Byte> permissionConstants) {
        BiMap<String, Byte> oldValue;
        synchronized (PERMISSION_CONSTANTS) {
            oldValue = this.permissionConstants;
            this.permissionConstants = permissionConstants;
        }
        notifyListeners(PERMISSION_CONSTANTS, oldValue, permissionConstants);
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
