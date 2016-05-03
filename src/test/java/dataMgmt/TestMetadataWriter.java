package dataMgmt;

import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by fahslaj on 5/2/2016.
 */
public class TestMetadataWriter {

    private static final String testMetadata = "./src/test/resources/dataMgmtTestFiles/testMetadata.json";
    private static final String testEmptyMetadata = "./src/test/resources/dataMgmtTestFiles/testEmptyMetadata.json";
    private static final String testMalformedMetadata = "./src/test/resources/dataMgmtTestFiles/testMalformedMetadata.json";
    private static final String testNoFilesMetadata = "./src/test/resources/dataMgmtTestFiles/testNoFilesMetadata.json";

    @Test
    public void testGetProjectMetadataNormal() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        expectedMetadata.setName("testProject");
        expectedMetadata.setOwner("Austin");
        expectedMetadata.setProjectId(10248523);

        int size = 3;
        FileMetadata[] fileMetadatas = new FileMetadata[size];

        long[] ids = {123, 456, 789};
        String[] paths = {"./testClass.java", "./otherStuff.txt", "./weirdFile.dank"};
        long[] versions = {184, 1, 56};

        for (int i = 0; i < fileMetadatas.length; i++) {
            fileMetadatas[i] = new FileMetadata();
            fileMetadatas[i].setFileId(ids[i]);
            fileMetadatas[i].setFilePath(paths[i]);
            fileMetadatas[i].setVersion(versions[i]);
        }
        expectedMetadata.setFiles(fileMetadatas);

        ProjectMetadata resultMetadata = MetadataWriter.getProjectMetadata(testMetadata);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test
    public void testGetProjectMetadataEmpty() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        ProjectMetadata resultMetadata = MetadataWriter.getProjectMetadata(testEmptyMetadata);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test
    public void testGetProjectMetadataMalformed() {
        MetadataWriter.logger = mock(Logger.class);
        MetadataWriter.getProjectMetadata(testMalformedMetadata);
        verify(MetadataWriter.logger, times(1)).error("IO Error on metadata read from path: "+testMalformedMetadata);
        MetadataWriter.logger = LoggerFactory.getLogger("metadata");
    }

    @Test
    public void testNoFilesMetadata() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        expectedMetadata.setProjectId(12948745);
        expectedMetadata.setName("MyProjectName");
        expectedMetadata.setOwner("Me");
        expectedMetadata.setFiles(new FileMetadata[0]);

        ProjectMetadata resultMetadata = MetadataWriter.getProjectMetadata(testNoFilesMetadata);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }
}
