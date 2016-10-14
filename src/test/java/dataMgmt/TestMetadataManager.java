package dataMgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Created by fahslaj on 5/2/2016.
 */
public class TestMetadataManager {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String configRoot = "./src/test/resources/dataMgmtTestFiles/";
    private static final String testMetadata = "testMetadata.json";
    private static final String testEmptyMetadata = "testEmptyMetadata.json";
    private static final String testMalformedMetadata = "testMalformedMetadata.json";
    private static final String testNoFilesMetadata = "testNoFilesMetadata.json";

    private static final String testWriteRoot = "./src/test/resources/dataMgmtTestFiles/";
    private static final String testWriteFile = "writtenTest.json";
    
    @Test
    public void testGetProjectMetadataNormal() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        expectedMetadata.setName("testProject");
        expectedMetadata.setPermissionLevel(10);
        expectedMetadata.setProjectID(10248523);

        int size = 3;
        FileMetadata[] fileMetadatas = new FileMetadata[size];

        long[] ids = {123, 456, 789};
        String[] paths = {"testClass.java", "otherStuff.txt", "weirdFile.dank"};
        long[] versions = {184, 1, 56};

        for (int i = 0; i < size; i++) {
            fileMetadatas[i] = new FileMetadata();
            fileMetadatas[i].setFileID(ids[i]);
            fileMetadatas[i].setRelativePath("./");
            fileMetadatas[i].setFilename(paths[i]);
            fileMetadatas[i].setVersion(versions[i]);
        }
        expectedMetadata.setFiles(fileMetadatas);

        DataManager.getInstance().getMetadataManager().readProjectMetadataFromFile(configRoot, testMetadata);
        ProjectMetadata resultMetadata = DataManager.getInstance().getMetadataManager().getProjectMetadata(configRoot);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test
    public void testGetProjectMetadataEmpty() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        DataManager.getInstance().getMetadataManager().readProjectMetadataFromFile(configRoot, testEmptyMetadata);
        ProjectMetadata resultMetadata = DataManager.getInstance().getMetadataManager().getProjectMetadata(configRoot);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test(expected=IllegalStateException.class)
    public void testGetProjectMetadataMalformed() {
        DataManager.getInstance().getMetadataManager().readProjectMetadataFromFile(configRoot, testMalformedMetadata);
    }

    @Test
    public void testNullRootPath() {
        ProjectMetadata metadata = DataManager.getInstance().getMetadataManager().getProjectMetadata(null);
        Assert.assertEquals(null, metadata);
    }

    @Test
    public void testNoFilesMetadata() {
        ProjectMetadata expectedMetadata = new ProjectMetadata();
        expectedMetadata.setProjectID(12948745);
        expectedMetadata.setName("MyProjectName");
        expectedMetadata.setPermissionLevel(10);
        expectedMetadata.setFiles(new FileMetadata[0]);

        DataManager.getInstance().getMetadataManager().readProjectMetadataFromFile(configRoot, testNoFilesMetadata);
        ProjectMetadata resultMetadata = DataManager.getInstance().getMetadataManager().getProjectMetadata(configRoot);
        Assert.assertEquals(expectedMetadata, resultMetadata);
    }

    @Test
    public void testWriteProjectMetadataNormal() {
        ProjectMetadata sampleMetadata = new ProjectMetadata();
        sampleMetadata.setProjectID(98);
        sampleMetadata.setName("sampleName");
        sampleMetadata.setPermissionLevel(10);
        FileMetadata[] files = new FileMetadata[1];
        files[0] = new FileMetadata();
        files[0].setFilename("TEST");
        files[0].setRelativePath("./");
        files[0].setVersion(1);
        sampleMetadata.setFiles(files);

        DataManager.getInstance().getMetadataManager().writeProjectMetadataToFile(sampleMetadata, testWriteRoot, testWriteFile);

        DataManager.getInstance().getMetadataManager().readProjectMetadataFromFile(testWriteRoot, testWriteFile);
        ProjectMetadata resultMetadata = DataManager.getInstance().getMetadataManager().getProjectMetadata(testWriteRoot);
        Assert.assertEquals(sampleMetadata, resultMetadata);

        File file = new File(testWriteRoot, testWriteFile);
        file.delete();
    }

    @Test
    public void testWriteProjectMetadataNoFiles() {
        ProjectMetadata sampleMetadata = new ProjectMetadata();
        sampleMetadata.setProjectID(203);
        sampleMetadata.setName("memes");
        sampleMetadata.setPermissionLevel(10);
        sampleMetadata.setFiles(new FileMetadata[0]);

        DataManager.getInstance().getMetadataManager().writeProjectMetadataToFile(sampleMetadata, testWriteRoot, testWriteFile);

        DataManager.getInstance().getMetadataManager().readProjectMetadataFromFile(testWriteRoot, testWriteFile);
        ProjectMetadata resultMetadata = DataManager.getInstance().getMetadataManager().getProjectMetadata(testWriteRoot);
        Assert.assertEquals(sampleMetadata, resultMetadata);

        File file = new File(testWriteRoot, testWriteFile);
        file.delete();
    }
}
