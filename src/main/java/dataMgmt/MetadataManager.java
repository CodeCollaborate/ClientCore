package dataMgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import dataMgmt.models.ProjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by fahslaj on 5/2/2016.
 */
public class MetadataManager {

    private static final String CONFIG_FILE_NAME = "CodeCollaborateConfig.json";

    // Logger
    protected static Logger logger = LoggerFactory.getLogger("metadata");

    // Json Object Mapper
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns the metadata object for the project at the given root path.
     * @param rootPath the absolute path of the root directory of the project
     * @return the ProjectMetadata object for the given root path's project, or null
     *          if none exists
     */
    public static ProjectMetadata getProjectMetadata(String rootPath) {
        return getProjectMetadataFileSpecific(rootPath+ CONFIG_FILE_NAME);
    }

    // business logic separated for testing purposes
    protected static ProjectMetadata getProjectMetadataFileSpecific(String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            return null;
        ProjectMetadata metadata;
        try {
            metadata = mapper.readValue(file, ProjectMetadata.class);
        } catch (IOException e) {
            logger.error("IO Error on metadata read from path: "+filePath);
            return null;
        }
        return metadata;
    }

    /**
     * Writes the metadata object for the project at the given path.
     * @param metadata the ProjectMetadata object to write
     * @param rootPath the root path of the project
     */
    public static void writeProjectMetadata(ProjectMetadata metadata, String rootPath) {
        writeProjectMetadataFileSpecific(metadata, rootPath+ CONFIG_FILE_NAME);
    }

    // business logic separated for testing purposes
    protected static void writeProjectMetadataFileSpecific(ProjectMetadata metadata, String filePath) {
        File file = new File(filePath);
        if (file.exists())
            file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            logger.error("IO Error on file creation: "+filePath);
            return;
        }
        try {
            mapper.writeValue(file, metadata);
        } catch (IOException e) {
            logger.error("IO Error on metadata write to file: "+filePath);
        }
    }
}
