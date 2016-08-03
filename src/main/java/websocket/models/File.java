package websocket.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class File {
	@JsonProperty("FileID")
	public long fileID;
	
	@JsonProperty("Name")
	public String fileName;
	
	@JsonProperty("RelativePath")
	public String relativePath;
	
	@JsonProperty("Version")
	public long fileVersion;
	
	@JsonProperty("ProjectID")
	public long projectID;

	public File(long fileID, String fileName, String relativePath, long fileVersion, long projectID) {
		super();
		this.fileID = fileID;
		this.fileName = fileName;
		this.relativePath = relativePath;
		this.fileVersion = fileVersion;
		this.projectID = projectID;
	}

	public long getFileID() {
		return fileID;
	}

	public void setFileID(long fileID) {
		this.fileID = fileID;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public long getFileVersion() {
		return fileVersion;
	}

	public void setFileVersion(long fileVersion) {
		this.fileVersion = fileVersion;
	}

	public long getProjectID() {
		return projectID;
	}

	public void setProjectID(long projectID) {
		this.projectID = projectID;
	}
	
	
}
