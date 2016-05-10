package dataMgmt;

import org.junit.Assert;
import org.junit.Test;
import patching.Patch;
import patching.PatchManager;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

/**
 * Created by fahslaj on 5/9/2016.
 */
public class TestFileContentWriter {

    @Test
    public void testEnqueueAlreadyInBuffer() {
        PatchManager manager = mock(PatchManager.class);
        FileContentWriter writer = new FileContentWriter(manager);
        IFileWritingQueue mocky = mock(FixedSizeWritingQueue.class);
        writer.fileBuffers.put(Long.parseLong("100"), mocky);
        ArrayList<Patch> patches = new ArrayList<>();
        patches.add(mock(Patch.class));
        writer.enqueuePatchesForWriting(Long.parseLong("100"), "path", patches);
        verify(mocky, times(1)).offerPatch(any(Patch.class), anyString());
    }

    @Test
    public void testEnqueueNotInBuffer() {
        PatchManager manager = mock(PatchManager.class);
        FileContentWriter writer = new FileContentWriter(manager);
        ArrayList<Patch> patches = new ArrayList<>();
        patches.add(mock(Patch.class));
        writer.enqueuePatchesForWriting(Long.parseLong("100"), "path", patches);
        Assert.assertNotNull(writer.fileBuffers.get(Long.parseLong("100")));
    }
}
