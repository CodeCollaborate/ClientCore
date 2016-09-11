package dataMgmt;

import org.junit.Assert;
import org.junit.Test;
import patching.PatchManager;

import static org.mockito.Mockito.*;

/**
 * Created by fahslaj on 5/9/2016.
 */
public class TestDataManager {

    @Test
    public void testGetSetPatchManager() {
        PatchManager manager = new PatchManager();
        DataManager.getInstance().setPatchManager(manager);
        Assert.assertEquals(manager, DataManager.getInstance().getPatchManager());
    }

    @Test
    public void testGetDefaultPatchManager() {
        Assert.assertNotNull(DataManager.getInstance().getPatchManager());
    }

    @Test
    public void testGetSetMetadataManager() {
        MetadataManager manager = new MetadataManager();
        DataManager.getInstance().setMetadataManager(manager);
        Assert.assertEquals(manager, DataManager.getInstance().getMetadataManager());
    }

    @Test
    public void testGetDefaultMetadataManager() {
        Assert.assertNotNull(DataManager.getInstance().getMetadataManager());
    }

    @Test
    public void testGetSetFileContentWriter() {
        FileContentWriter fcw = new FileContentWriter(mock(PatchManager.class));
        DataManager.getInstance().setFileContentWriter(fcw);
        Assert.assertEquals(fcw, DataManager.getInstance().getFileContentWriter());
    }

    @Test
    public void testGetDefaultFileContentWriter() {
        Assert.assertNotNull(DataManager.getInstance().getFileContentWriter());
    }

    @Test
    public void testGetSetSessionStorage() {
        SessionStorage storage = new SessionStorage();
        DataManager.getInstance().setSessionStorage(storage);
        Assert.assertEquals(storage, DataManager.getInstance().getSessionStorage());
    }

    @Test
    public void testGetDefaultSessionStorage() {
        Assert.assertNotNull(DataManager.getInstance().getSessionStorage());
    }
}
