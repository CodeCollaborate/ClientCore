package clientcore.dataMgmt;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import clientcore.websocket.models.File;
import clientcore.websocket.models.Project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The storage unit for session data. Created by fahslaj on 5/3/2016.
 */
public class SessionStorage {

	public static final String USERNAME = "username";
	public static final String AUTH_TOKEN = "authtoken";
	public static final String PROJECT_LIST = "projectlist";
	public static final String FILE_LIST = "filelist";
	public static final String SUBSCRIBED_PROJECTS = "subscribedlist";
	public static final String PROJECT_USER_STATUS = "projectuserstatus";
	public static final String PERMISSION_CONSTANTS = "permissionconstants";

	// the username for the user
	private String username;

	// the authentication token for the user
	private String authenticationToken;

	// the map of loaded projects
	private Map<Long, Project> projects = new HashMap<>();

	// the map of absolute project paths on disk to project IDs
	private BiMap<Path, Long> projectPathToID = HashBiMap.create();

	// the set of subscribed project IDs
	private HashSet<Long> subscribedIds = new HashSet<>();

	// the map of users-project keys and their online status
	private Map<String, OnlineStatus> projectUserStatus = new HashMap<>();

	// the map of permission clientcore.constants
	private BiMap<String, Byte> permissionConstants = HashBiMap.create();

	// list of listeners for this class's properties
	private final List<PropertyChangeListener> listeners = new ArrayList<>();

	/**
	 * Get the current user's username.
	 * 
	 * @return username
	 */
	public String getUsername() {
		synchronized (USERNAME) {
			return username;
		}
	}

	/**
	 * Set the current user's username.
	 * 
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
	 * 
	 * @return authentication token
	 */
	public String getAuthenticationToken() {
		synchronized (AUTH_TOKEN) {
			return authenticationToken;
		}
	}

	/**
	 * Set the current user's authentication token.
	 * 
	 * @param authenticationToken
	 *            the new authentication token
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
	 * 
	 * @return projects list
	 */
	public List<Project> getProjects() {
		synchronized (PROJECT_LIST) {
			return Collections.unmodifiableList(new ArrayList<>(this.projects.values()));
		}
	}

	/**
	 * Get the current user's loaded projects sorted by name.
	 * 
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
	 * 
	 * @param projectID
	 *            the id with which to look up a project
	 * @return the project with the specified projectId, or null if there is
	 *         none
	 */
	public Project getProject(long projectID) {
		synchronized (PROJECT_LIST) {
			return this.projects.get(projectID);
		}
	}

	/**
	 * Get a specific project by the absolute path to its root. Will return null
	 * if the path has not yet been mapped to the project, even if the project
	 * exists in the collection of projects.
	 * 
	 * @param absolutePath
	 *            The absolute path to the root of the project
	 * @return The project this path is mapped to, or null if this path is not
	 *         mapped to a project.
	 */
	public Project getProject(Path absolutePath) {
		synchronized (PROJECT_LIST) {
			Long projectID = projectPathToID.get(absolutePath);
			return projects.get(projectID);
		}
	}

	/**
	 * Set the current user's loaded projects. Does not set the projects' path
	 * in the path to ID map since there's no guarantee that these projects
	 * exist on disk at this point.
	 * 
	 * @param projects
	 *            the list of projects to set
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
	 * Add a project to the currently loaded projects. If one project already
	 * exists with the given id, it will be overwritten. Does not set the
	 * project's path in the path to ID map since there's no guarantee that this
	 * project exists on disk at this point.
	 * 
	 * @param project
	 *            the project to add
	 */
	public void setProject(Project project) {
		synchronized (PROJECT_LIST) {
			this.projects.put(project.getProjectID(), project);
		}
		notifyListeners(PROJECT_LIST, null, project);
	}

	/**
	 * Puts the absolute project path in a map to the project ID.
	 * 
	 * @param absPath
	 * @param projectID
	 */
	public void putAbsoluteProjectPath(Path absPath, long projectID) {
		synchronized (PROJECT_LIST) {
			projectPathToID.put(absPath, projectID);
		}
	}

	public void changeProjectPath(long projectID, Path absPath) {
		synchronized (PROJECT_LIST) {
			projectPathToID.inverse().put(projectID, absPath);
		}
	}

	public Path getProjectLocation(long projectID) {
		Path p;
		synchronized (PROJECT_LIST) {
			p = projectPathToID.inverse().get(projectID);
		}
		return p;
	}

	/**
	 * Remove a project by its id, if it exists.
	 * 
	 * @param id
	 *            the id of the project to remove
	 */
	public Project removeProject(long id) {
		Project old;
		synchronized (PROJECT_LIST) {
			projectPathToID.inverse().remove(id);
			old = this.projects.remove(id);
		}
		notifyListeners(PROJECT_LIST, old, null);
		return old;
	}

	/**
	 * Adds a file to the list of files within the project that matches the
	 * given project ID. Also sets the project ID of the file to the given
	 * project.
	 * 
	 * @param projectID
	 *            The ID of the project to add the file model to
	 * @param f
	 *            The file model to add to the project that matches the project
	 *            ID
	 */
	public void setFile(long projectID, File f) {
		synchronized (FILE_LIST) {
			f.setProjectID(projectID);
			Project p = projects.get(projectID);
			p.addFile(f);
		}
	}

	/**
	 * Puts the absolute file path in a map to the file ID.
	 * 
	 * @param absPath
	 *            The absolute path of the file
	 * @param projectID
	 *            The ID of the project the file belongs to
	 * @param fileID
	 *            The ID of the file to map the path to
	 */
	public void putAbsoluteFilePath(Path absPath, long projectID, long fileID) {
		synchronized (FILE_LIST) {
			Project p = projects.get(projectID);
			p.putAbsoluteFilePath(absPath, fileID);
		}
	}

	/**
	 * Looks through all projects for the file that matches the given file ID
	 * and returns it if found. If not found, null is returned.
	 *
	 * TODO: Evaluate this for performance; do we want to maintain an index?
	 *
	 * @return The removed file
	 */
	public File getFile(long fileID) {
		for (Project p : projects.values()) {
			if (p.hasFile(fileID)) {
				return p.getFile(fileID);
			}
		}
		return null;
	}

	/**
	 * Looks through all projects for the file that matches the given absolute
	 * path and returns it if found. If not found, this could either mean that
	 * the file does not exist or there is no path mapped to the file.
	 * 
	 * @param absolutePath
	 *            The absolute path of the file
	 * @return The file that the path is mapped to, or null if the path is not
	 *         mapped to a file
	 */
	public File getFile(Path absolutePath) {
		for (Project p : projects.values()) {
			File f = p.getFile(absolutePath);
			if (f != null) {
				return f;
			}
		}
		return null;
	}

	/**
	 * Looks through all projects for the file that matches the given file ID
	 * and removes it. If found, the file object is returned, otherwise this
	 * method returns null.
	 * 
	 * @param fileID
	 *            The ID of the file to remove
	 * @return The file that is removed or null if there is no file with this ID
	 *         in the store
	 */
	public File removeFile(long fileID) {
		synchronized (FILE_LIST) {
			for (Project p : projects.values()) {
				File old;
				if ((old = p.removeFile(fileID)) != null) {
					return old;
				}
			}
		}
		return null;
	}

	/**
	 * Set the given project id as a subscribed project, if it exists.
	 * 
	 * @param id
	 *            to set subscribed
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
	 * 
	 * @param id
	 *            to remove from subscribed set
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
	 * 
	 * @return set of subscribed ids
	 */
	public Set<Long> getSubscribedIds() {
		synchronized (SUBSCRIBED_PROJECTS) {
			return Collections.unmodifiableSet(this.subscribedIds);
		}
	}

	/**
	 * Change the status of a user-project key in the status map and notify
	 * observers of this change.
	 * 
	 * @param projectUserKey
	 *            the user-project combo to change
	 * @param status
	 *            the new status
	 */
	public void changeProjectUserStatus(String projectUserKey, OnlineStatus status) {
		OnlineStatusKeyPair oldValue;
		synchronized (this) {
			oldValue = new OnlineStatusKeyPair(projectUserKey, projectUserStatus.get(projectUserKey));
			projectUserStatus.put(projectUserKey, status);
		}
		notifyListeners(PROJECT_USER_STATUS, oldValue, new OnlineStatusKeyPair(projectUserKey, status));
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
	 * Remove the status of a user-project key from the map and notify observers
	 * of this change.
	 * 
	 * @param projectUserKey
	 *            the user-project key of which to remove the value
	 */
	public void removeProjectUserStatus(String projectUserKey) {
		OnlineStatus status;
		synchronized (this) {
			status = projectUserStatus.remove(projectUserKey);
		}
		notifyListeners(PROJECT_USER_STATUS, projectUserKey, null);
	}

	/**
	 * Get the BiMap of permission clientcore.constants.
	 * 
	 * @return the permission clientcore.constants
	 */
	public BiMap<String, Byte> getPermissionConstants() {
		BiMap<String, Byte> constants;
		synchronized (PERMISSION_CONSTANTS) {
			constants = permissionConstants;
		}
		return constants;
	}

	/**
	 * Set the permission clientcore.constants map.
	 * 
	 * @param permissionConstants
	 *            permission clientcore.constants BiMap to set
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
		synchronized (this.listeners) {
			for (PropertyChangeListener listener : this.listeners) {
				listener.propertyChange(new PropertyChangeEvent(this, identifier, oldValue, newValue));
			}
		}
	}

	/**
	 * Add a property change listener
	 * 
	 * @param listener
	 *            listener to add
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		synchronized (this.listeners) {
			this.listeners.add(listener);
		}
	}

	/**
	 * Remove a property change listener
	 * 
	 * @param listener
	 *            listener to remove
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		synchronized (this.listeners) {
			this.listeners.remove(listener);
		}
	}
}
