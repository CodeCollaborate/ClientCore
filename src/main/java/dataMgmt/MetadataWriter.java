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
public class MetadataWriter {

    // Logger
    protected static Logger logger = LoggerFactory.getLogger("metadata");

    // Json Object Mapper
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns the metadata object for the project at the given root path.
     * @param rootPath the absolute path of the root directory of the project
     * @return the ProjectMetadata object for the given root path's project, or null
     *          if none exists.
     */
    public static ProjectMetadata getProjectMetadata(String rootPath) {
        File file = new File(rootPath);
        if (!file.exists())
            return null;
        ProjectMetadata metadata;
        try {
            metadata = mapper.readValue(file, ProjectMetadata.class);
        } catch (IOException e) {
            logger.error("IO Error on metadata read from path: "+rootPath);
            return null;
        }
        return metadata;
    }
}
