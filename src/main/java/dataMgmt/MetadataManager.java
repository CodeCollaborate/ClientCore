package dataMgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the metadata of projects and files.
 * Created by fahslaj on 5/2/2016.
 */
public class MetadataManager {

    // The default name of the configuration file located at a project's root
    private static final String CONFIG_FILE_NAME = "CodeCollaborateConfig.json";
    // Json Object Mapper
    private static final ObjectMapper mapper = new ObjectMapper();
    // Logger
    protected static Logger logger = LoggerFactory.getLogger("metadata");


    // stores all project metadata, based on project root path
    private Map<String, ProjectMetadata> projectMetadataMap = new HashMap<>();
    // stores all file metadata, based on file's path
    private Map<String, FileMetadata> fileMetadataMap = new HashMap<>();
    // stores conversions between projectID and filePath
    private Map<Long, String> projectIDtoRootPath = new HashMap<>();
    // stores conversions between fileID and filePath
    private Map<Long, String> fileIDtoFilePath = new HashMap<>();
    // stores conversions between fileID and projectID
    private Map<Long, Long> fileIDtoProjectID = new HashMap<>();

    public Collection<FileMetadata> getAllFiles(){
        return fileMetadataMap.values();
    }
    public Collection<ProjectMetadata> getAllProjects(){
        return projectMetadataMap.values();
    }

    /**
     * Retrieves the project metadata
     *
     * @param projectID project's unique ID
     * @return ProjectMetadata object representing the project's metadata
     */
    public ProjectMetadata getProjectMetadata(long projectID) {
        String projectRootPath = getProjectLocation(projectID);
        if (projectRootPath == null) {
            return null;
        }

        return projectMetadataMap.get(projectRootPath);
    }

    /**
     * Gets root path for project
     *
     * @param projectID project's unique ID
     * @return String object representing the project's root path. Null if no such projectID found.
     */
    public String getProjectLocation(long projectID) {
        return projectIDtoRootPath.get(projectID);
    }

    /**
     * Retrieves the project metadata
     *
     * @param rootPath project's root path
     * @return ProjectMetadata object representing the project's metadata
     */
    public ProjectMetadata getProjectMetadata(String rootPath) {
        return projectMetadataMap.get(rootPath);
    }

    /**
     * Gets projectID that the file for the provided FileID belongs to
     * @param fileID the fileID to lookup the project for
     * @return the ProjectID that the file belongs to, or null if no such entry exists.
     */
    public Long getProjectIDForFileID(long fileID){
        return fileIDtoProjectID.get(fileID);
    }

    /**
     * Retrieves the file metadata
     *
     * @param fileID file's unique ID
     * @return FileMetadata object representing the file's metadata
     */
    public FileMetadata getFileMetadata(long fileID) {
        String filepath = fileIDtoFilePath.get(fileID);
        if (filepath == null) {
            return null;
        }

        return fileMetadataMap.get(filepath);
    }

    /**
     * Retrieves the file metadata
     *
     * @param filePath filepath
     * @return FileMetadata object representing the file's metadata
     */
    public FileMetadata getFileMetadata(String filePath) {
        return fileMetadataMap.get(filePath);
    }

    public void readProjectMetadataFromFile(String projectRoot, String configFileName) {
        // Check if file exists
        File file = new File(projectRoot, configFileName);
        if (!file.exists()) {
            throw new IllegalArgumentException("No such file found");
        }

        // Attempt to read file
        ProjectMetadata metadata;
        try {
            metadata = mapper.readValue(file, ProjectMetadata.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse ProjectMetadata from file: " + Paths.get(projectRoot, configFileName).toString().replace('\\', '/') + " - " + e.getMessage());
        }

        if (metadata != null){
            putProjectMetadata(projectRoot, metadata);
        }
    }

    public void putProjectMetadata(String projectRoot, ProjectMetadata metadata) {
        projectMetadataMap.put(projectRoot, metadata);
        projectIDtoRootPath.put(metadata.getProjectID(), projectRoot);

        if (metadata.getFiles() != null) {
            for (FileMetadata f : metadata.getFiles()) {
                // Only add it if filepath is valid (non-null)
                if (f.getFilePath() != null) {
                    String filePath = Paths.get(projectRoot, f.getFilePath()).normalize().toAbsolutePath().toString().replace('\\', '/');
                    putFileMetadata(filePath, metadata.getProjectID(), f);
                }
            }
        }
    }

    public void putFileMetadata(String filePath, long projectID, FileMetadata metadata) {
        fileMetadataMap.put(filePath, metadata);
        fileIDtoFilePath.put(metadata.getFileID(), filePath);
        fileIDtoProjectID.put(metadata.getFileID(), projectID);
        ProjectMetadata meta = projectMetadataMap.get(projectIDtoRootPath.get(projectID));
        List<FileMetadata> files;
        if (meta.getFiles() == null) {
            files = new ArrayList<>();
            files.add(metadata);
            meta.setFiles(files);
        } else {
            files = meta.getFiles();
            if (!files.contains(metadata)) {
                files.add(metadata);
            }
        }
    }

    public void projectMoved(long projectID, String newRootPath) {
        projectIDtoRootPath.put(projectID, newRootPath);
    }

    public void projectDeleted(long projectID) {
        String rootPath = getProjectLocation(projectID);
        if (rootPath != null){
            projectMetadataMap.remove(rootPath);
        }
        projectIDtoRootPath.remove(projectID);
    }

    public void fileMoved(long fileID, String newFilePath) {
        Path p = Paths.get(newFilePath);
        String filePath = p.getParent().toString().replace("\\", "/");
        String fileName = p.getFileName().toString().replace("\\", "/");

        getFileMetadata(fileID).setRelativePath(filePath);
        getFileMetadata(fileID).setFilename(fileName);
        fileIDtoFilePath.put(fileID, newFilePath);
    }

    public void fileDeleted(Long fileID) {
        String filePath = fileIDtoFilePath.get(fileID);
        if (filePath != null){
            fileIDtoFilePath.remove(fileID);
            fileMetadataMap.remove(filePath);
        }

        Long id = fileIDtoProjectID.get(fileID);
        fileIDtoProjectID.remove(fileID);
        String rootPath = projectIDtoRootPath.get(id);
        if (rootPath != null) {
            ProjectMetadata projectMetadata = projectMetadataMap.get(rootPath);
            List<FileMetadata> metas = projectMetadata.getFiles();
            if (metas != null) {
                FileMetadata toRemove = null;
                for (FileMetadata meta : metas) {
                    if (meta.getFileID() == fileID) {
                        toRemove = meta;
                    }
                }
                if (toRemove != null) {
                    metas.remove(toRemove);
                }
            }
        }
    }

    /**
     * Writes the metadata object for the project at the given path.
     *
     * @param metadata    the ProjectMetadata object to write
     * @param projectRoot the root path of the project
     * @param configFileName the name of the config file
     */
    public void writeProjectMetadataToFile(ProjectMetadata metadata, String projectRoot, String configFileName) {
        File file = new File(projectRoot, configFileName);

        // Remove file, and rewrite
        if (file.exists()) {
            file.delete();
        }

        // Create file
        try {
            file.createNewFile();
        } catch (IOException e) {
            logger.error("IO Error on file creation: " + projectRoot + " - " + e.getMessage());
            return;
        }

        // Write to file
        try {
            mapper.writeValue(file, metadata);
        } catch (IOException e) {
            logger.error("IO Error on metadata write to file: " + projectRoot + " - " + e.getMessage());
        }
    }
}
