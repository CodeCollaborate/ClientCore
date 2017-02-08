package dataMgmt;

import java.nio.file.Path;
import websocket.models.File;
import websocket.models.Project;

public class FileController {
	
	private SessionStorage ss;
	
	public FileController(SessionStorage ss) {
		this.ss = ss;
	}

	/**
	 * Stores the given file model in the storage used to construct this object.
	 * 
	 * @param projectID
	 * @param f
	 */
	public void createFile(long projectID, File f) {
		ss.setFile(projectID, f);
	}

	/**
	 * Deletes the file that matches the given file ID.
	 * 
	 * @param fileID
	 */
	public void deleteFile(long fileID) {
		ss.removeFile(fileID);
	}

	/**
	 * If the file exists in storage, then this method maps the given absolute
	 * path to the given file by its ID.
	 * 
	 * @param absolutePath
	 * @param projectID
	 * @param fileID
	 */
	public void putFileLocation(Path absolutePath, long projectID, long fileID) {
		Project p = ss.getProject(projectID);
		if (p.hasFile(fileID)) {
			ss.putAbsoluteFilePath(absolutePath, projectID, fileID);
		}
	}

	/**
	 * Re-maps the project to the provided path.
	 * 
	 * @param fileID
	 * @param projectID
	 * @param newAbsolutePath
	 */
	public void moveFile(long fileID, long projectID, Path newAbsolutePath, Path newRelativePath) {
		Project p = ss.getProject(projectID);
		p.changeFilePath(fileID, newAbsolutePath);
		p.getFile(fileID).setRelativePath(newRelativePath);
	}

	/**
	 * Re-maps the project to the provided path and sets the project model's
	 * name to the new name.
	 * 
	 * @param fileID
	 * @param projectID
	 * @param newName
	 * @param newAbsolutePath
	 */
	public void renameFile(long fileID, long projectID, String newName, Path newAbsolutePath) {
		File f = ss.getProject(projectID).getFile(fileID);
		f.setFilename(newName);
		ss.changeProjectPath(projectID, newAbsolutePath);
	}
	
	
}
