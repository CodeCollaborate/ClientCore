package dataMgmt;

import java.nio.file.Path;

import websocket.models.Permission;
import websocket.models.Project;

/**
 * A controller that performs high level operations on the storage class
 * provided in the constructor.
 * 
 * @author loganga
 *
 */
public class ProjectController {
	private SessionStorage ss;

	public ProjectController(SessionStorage ss) {
		this.ss = ss;
	}

	/**
	 * Stores the given project in the storage used to construct this object.
	 * 
	 * @param p
	 */
	public void createProject(Project p) {
		ss.setProject(p);
	}

	/**
	 * Deletes the project that matches the given project ID.
	 * 
	 * @param projectID
	 */
	public void deleteProject(long projectID) {
		ss.removeProject(projectID);
	}

	/**
	 * If the project exists in storage, then this method maps the given root
	 * path to the given project by its ID.
	 * 
	 * @param rootPath
	 * @param projectID
	 */
	public void putProjectLocation(Path rootPath, long projectID) {
		if (ss.getProject(projectID) != null) {
			ss.putAbsoluteProjectPath(rootPath, projectID);
		}
	}

	/**
	 * Re-maps the project to the provided path.
	 * 
	 * @param projectID
	 * @param newRootPath
	 */
	public void moveProject(long projectID, Path newRootPath) {
		ss.changeProjectPath(projectID, newRootPath);
	}

	/**
	 * Re-maps the project to the provided path and sets the project model's
	 * name to the new name.
	 * 
	 * @param projectID
	 * @param newName
	 * @param newRootPath
	 */
	public void renameProject(long projectID, String newName, Path newRootPath) {
		ss.getProject(projectID).setName(newName);
		ss.changeProjectPath(projectID, newRootPath);
	}

	/**
	 * Puts the given permission parameters into the permission map for the
	 * project that matches the given project ID.
	 * 
	 * @param projectID
	 * @param name
	 * @param p
	 */
	public void addPermission(long projectID, String name, Permission p) {
		ss.getProject(projectID).getPermissions().put(name, p);
	}

	/**
	 * Removes the given user from the permission map for the project that
	 * matches the given project ID.
	 * 
	 * @param projectID
	 * @param name
	 */
	public void removePermission(long projectID, String name) {
		ss.getProject(projectID).getPermissions().remove(name);
	}

}
