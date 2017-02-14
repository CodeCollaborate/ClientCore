package clientcore.websocket.models;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class File {
	@JsonProperty("FileID")
	private long fileID;

	@JsonProperty("Filename")
	private String filename;

	@JsonProperty("RelativePath")
	private Path relativePath;

	@JsonProperty("Version")
	private long fileVersion;

	@JsonProperty("Creator")
	private String creator;

	@JsonProperty("CreationDate")
	private String creationDate;

	@JsonIgnore
	private long projectID;

	/**
	 * This constructor is called when the project model is created from a
	 * server notification since the majority of its parameters are generated
	 * from the server.
	 * 
	 * @param fileID
	 * @param filename
	 * @param relativePath
	 * @param fileVersion
	 * @param creator
	 * @param creationDate
	 */
	public File(@JsonProperty("FileID") long fileID, @JsonProperty("Filename") String filename,
			@JsonProperty("RelativePath") String relativePath, @JsonProperty("Version") long fileVersion,
			@JsonProperty("Creator") String creator, @JsonProperty("CreationDate") String creationDate) {
		super();
		this.fileID = fileID;
		this.filename = filename;
		this.relativePath = Paths.get(relativePath);
		this.fileVersion = fileVersion;
		this.creator = creator;
		this.creationDate = creationDate;
	}

	/**
	 * Returns true if this file model has also been created on the server.
	 * 
	 * Currently, a server valid file is defined as a file that has a file ID
	 * that is not -1, a file version that is not -1, and a creation date that
	 * is not null.
	 * 
	 * @return
	 */
	public boolean isServerValid() {
		return fileID != -1L && fileVersion != -1L && creationDate != null;
	}

	public long getFileID() {
		return fileID;
	}

	public void setFileID(long fileID) {
		this.fileID = fileID;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public Path getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(Path relativePath) {
		this.relativePath = relativePath;
	}

	public long getFileVersion() {
		return fileVersion;
	}

	public void setFileVersion(long fileVersion) {
		this.fileVersion = fileVersion;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public long getProjectID() {
		return projectID;
	}

	public void setProjectID(long projectID) {
		this.projectID = projectID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creator == null) ? 0 : creator.hashCode());
		result = prime * result + (int) (fileID ^ (fileID >>> 32));
		result = prime * result + (int) (fileVersion ^ (fileVersion >>> 32));
		result = prime * result + ((filename == null) ? 0 : filename.hashCode());
		result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		File other = (File) obj;
		if (creator == null) {
			if (other.creator != null)
				return false;
		} else if (!creator.equals(other.creator))
			return false;
		if (fileID != other.fileID)
			return false;
		if (fileVersion != other.fileVersion)
			return false;
		if (filename == null) {
			if (other.filename != null)
				return false;
		} else if (!filename.equals(other.filename))
			return false;
		if (relativePath == null) {
			if (other.relativePath != null)
				return false;
		} else if (!relativePath.equals(other.relativePath))
			return false;
		return true;
	}
}
