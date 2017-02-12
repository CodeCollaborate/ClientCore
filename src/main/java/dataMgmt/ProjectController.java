package dataMgmt;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	public static final Logger logger = LogManager.getLogger("ProjectController");
	
	public ProjectController(SessionStorage ss) {
		this.ss = ss;
	}

	/**
	 * Stores the given project in the storage used to construct this object.
	 * 
	 * @param p
	 *            The project model to add
	 */
	public void createProject(Project p) {
		ss.setProject(p);
	}

	/**
	 * Deletes the project that matches the given project ID.
	 * 
	 * @param projectID
	 *            The ID of the project to delete
	 */
	public void deleteProject(long projectID) {
		ss.removeProject(projectID);
	}

	/**
	 * If the project exists in storage, then this method maps the given root
	 * path to the given project by its ID.
	 * 
	 * @param rootPath
	 *            The absolute path to the root of the project
	 * @param projectID
	 *            The ID of the project to map the path to
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
	 *            The ID of the project to move
	 * @param newRootPath
	 *            The new absolute path to the root of the project
	 */
	public void moveProject(long projectID, Path newRootPath) {
		ss.changeProjectPath(projectID, newRootPath);
	}

	/**
	 * Re-maps the project to the provided path and sets the project model's
	 * name to the new name.
	 * 
	 * @param projectID
	 *            The ID of the project to rename
	 * @param newName
	 *            The new name of the project
	 * @param newRootPath
	 *            The new absolute path to the root of the project
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
	 *            The ID of the project to add the permission to
	 * @param name
	 *            The name of the user to add a permission for
	 * @param p
	 *            The permission that the user is provisioned for this project
	 */
	public void addPermission(long projectID, String name, Permission p) {
		Project proj = ss.getProject(projectID);
		if (proj != null) {
			proj.getPermissions().put(name, p);
		} else {
			logger.warn("Tried to add permission to a nonexistent project");
		}
	}

	/**
	 * Removes the given user from the permission map for the project that
	 * matches the given project ID.
	 * 
	 * @param projectID
	 *            The ID of the project to remove the permission for
	 * @param name
	 *            The name of the user to remove from the permission map for
	 *            this project
	 */
	public void removePermission(long projectID, String name) {
		Project proj = ss.getProject(projectID);
		if (proj != null) {
			proj.getPermissions().remove(name);
		} else {
			logger.warn("Tried to remove permission from a nonexistent project");
		}
	}

}
