package clientcore.websocket.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Project {
	@JsonProperty("ProjectID")
	private long projectID;

	@JsonProperty("Name")
	private String name;

	@JsonProperty("Permissions")
	private Map<String, Permission> permissions;

	@JsonIgnore
	private Map<Long, File> files;
	
	@JsonIgnore
	private BiMap<Path, Long> filePathToID;

	/**
	 * This constructor is called when the project model is created from a
	 * server notification since the majority of its parameters are generated
	 * from the server.
	 * 
	 * @param projectID
	 * @param name
	 * @param permissions
	 */
	public Project(@JsonProperty("ProjectID") long projectID, @JsonProperty("Name") String name,
			@JsonProperty("Permissions") Map<String, Permission> permissions) {
		super();
		this.projectID = projectID;
		this.name = name;
		this.permissions = permissions;
		this.files = new HashMap<>();
		this.filePathToID = HashBiMap.create();
	}

	public long getProjectID() {
		return projectID;
	}

	public void setProjectID(long projectID) {
		this.projectID = projectID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Map<String, Permission> permissions) {
		this.permissions = permissions;
	}

	public Collection<File> getFiles() {
		return files.values();
	}

	public void setFiles(List<File> files) {
		for (File f : files) {
			addFile(f);
		}
	}
	
	public void addFile(File file) {
		file.setProjectID(projectID);
		this.files.put(file.getFileID(), file);
	}
	
	public void setAbsoluteFilePath(Long fileID, Path absolutePath) {
		if(hasFile(fileID)){
			filePathToID.forcePut(absolutePath, fileID);
		}
	}
	
	public File removeFile(long fileID) {
		filePathToID.inverse().remove(fileID);
		return files.remove(fileID);
	}
	
	public File removeFile(Path absolutePath) {
		Long fileID = filePathToID.remove(absolutePath);
		if (fileID != null) {
			return files.get(fileID);
		}
		return null;
	}
	
	public File getFile(long fileID) {
		return files.get(fileID);
	}
	
	public File getFile(Path absolutePath) {
		Long fileID = filePathToID.get(absolutePath);
		if (fileID == null) {
			return null;
		}
		return getFile(fileID);
	}
	
	public boolean hasFile(long fileID) {
		return files.containsKey(fileID);
	}
	
	public boolean pathMappedToFile(Path absolutePath) {
		return filePathToID.containsKey(absolutePath);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (projectID ^ (projectID >>> 32));
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
		Project other = (Project) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (projectID != other.projectID)
			return false;
		return true;
	}

}
