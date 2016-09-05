package websocket.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class File {
	@JsonProperty("FileID")
	private long fileID;

	@JsonProperty("Name")
	private String fileName;

	@JsonProperty("RelativePath")
	private String relativePath;

	@JsonProperty("Version")
	private long fileVersion;

	public File(@JsonProperty("FileID") long fileID,
				@JsonProperty("Name") String fileName,
				@JsonProperty("RelativePath") String relativePath,
				@JsonProperty("Version") long fileVersion) {
		super();
		this.fileID = fileID;
		this.fileName = fileName;
		this.relativePath = relativePath;
		this.fileVersion = fileVersion;
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
}
