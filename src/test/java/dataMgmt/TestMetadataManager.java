package dataMgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by fahslaj on 5/2/2016.
 */
public class TestMetadataManager {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String testMetadata = "./src/test/resources/dataMgmtTestFiles/testMetadata.json";
    private static final String testEmptyMetadata = "./src/test/resources/dataMgmtTestFiles/testEmptyMetadata.json";
    private static final String testMalformedMetadata = "./src/test/resources/dataMgmtTestFiles/testMalformedMetadata.json";
    private static final String testNoFilesMetadata = "./src/test/resources/dataMgmtTestFiles/testNoFilesMetadata.json";

    private static final String testWriteDestination = "./src/test/resources/dataMgmtTestFiles/writtenTest.json";
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

        ProjectMetadata resultMetadata = MetadataManager.getInstance().getProjectMetadataFileSpecific(testMetadata);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test
    public void testGetProjectMetadataEmpty() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        ProjectMetadata resultMetadata = MetadataManager.getInstance().getProjectMetadataFileSpecific(testEmptyMetadata);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test
    public void testGetProjectMetadataMalformed() {
        MetadataManager.logger = mock(Logger.class);
        MetadataManager.getInstance().getProjectMetadataFileSpecific(testMalformedMetadata);
        verify(MetadataManager.logger, times(1)).error("IO Error on metadata read from path: "+testMalformedMetadata);
        MetadataManager.logger = LoggerFactory.getLogger("metadata");
    }

    @Test
    public void testNoFilesMetadata() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        expectedMetadata.setProjectId(12948745);
        expectedMetadata.setName("MyProjectName");
        expectedMetadata.setOwner("Me");
        expectedMetadata.setFiles(new FileMetadata[0]);

        ProjectMetadata resultMetadata = MetadataManager.getInstance().getProjectMetadataFileSpecific(testNoFilesMetadata);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test
    public void testWriteProjectMetadataNormal() {
        ProjectMetadata sampleMetadata = new ProjectMetadata();
        sampleMetadata.setProjectId(98);
        sampleMetadata.setName("sampleName");
        sampleMetadata.setOwner("Gene");
        FileMetadata[] files = new FileMetadata[1];
        files[0] = new FileMetadata();
        sampleMetadata.setFiles(files);

        MetadataManager.getInstance().writeProjectMetadataFileSpecific(sampleMetadata, testWriteDestination);
        File file = new File(testWriteDestination);
        ProjectMetadata result = null;
        if (!file.exists())
            Assert.fail("File was not written");
        try {
            result = mapper.readValue(file, ProjectMetadata.class);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Invalid json");
        }
        Assert.assertEquals(sampleMetadata, result);
        file.delete();
    }

    @Test
    public void testWriteProjectMetadataNoFiles() {
        ProjectMetadata sampleMetadata = new ProjectMetadata();
        sampleMetadata.setProjectId(203);
        sampleMetadata.setName("memes");
        sampleMetadata.setOwner("Greg");
        sampleMetadata.setFiles(new FileMetadata[0]);

        MetadataManager.getInstance().writeProjectMetadataFileSpecific(sampleMetadata, testWriteDestination);
        File file = new File(testWriteDestination);
        ProjectMetadata result = null;
        if (!file.exists())
            Assert.fail("File was not written");
        try {
            result = mapper.readValue(file, ProjectMetadata.class);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Invalid json");
        }
        Assert.assertEquals(sampleMetadata, result);
        file.delete();
    }
}
