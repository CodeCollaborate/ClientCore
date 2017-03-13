package clientcore.dataMgmt;

import java.nio.file.Path;
import clientcore.websocket.models.File;
import clientcore.websocket.models.Project;

public class FileController {

	private SessionStorage ss;

	public FileController(SessionStorage ss) {
		this.ss = ss;
	}

	/**
	 * Stores the given file model in the storage used to construct this object.
	 *
	 * @param projectID
	 *            The ID of the project to add the file model to
	 * @param f
	 *            The file model to add to the project that matches the give ID
	 */
	public void createFile(long projectID, File f) {
		ss.setFile(projectID, f);
	}

	/**
	 * Deletes the file that matches the given file ID.
	 *
	 * @param fileID
	 *            The ID of the file to delete
	 */
	public void deleteFile(long fileID) {
		ss.removeFile(fileID);
	}

	/**
	 * If the file exists in storage, then this method maps the given absolute
	 * path to the given file by its ID.
	 *
	 * @param absolutePath
	 *            The absolute path of the file
	 * @param projectID
	 *            The ID of the project that the file belongs to
	 * @param fileID
	 *            The ID of the file to map the path to
	 */
	public void putFileLocation(Path absolutePath, long projectID, long fileID) {
        ss.setAbsoluteFilePath(absolutePath, projectID, fileID);
	}

	/**
	 * Re-maps the project to the provided path.
	 *
	 * @param fileID
	 *            The ID of the file to re-map the path to
	 * @param projectID
	 *            The ID of the project the file belongs to
	 * @param newAbsolutePath
	 *            The absolute path the file has been moved to
	 */
	public void moveFile(long fileID, long projectID, Path newAbsolutePath, Path newRelativePath) {
		ss.setAbsoluteFilePath(newAbsolutePath, projectID, fileID);
        ss.getProject(projectID).getFile(fileID).setRelativePath(newRelativePath);
        ss.getProject(projectID).getFile(fileID).setFilename(newAbsolutePath.getFileName().toString());
	}

}
