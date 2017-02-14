package clientcore.dataMgmt;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import clientcore.patching.Patch;
import clientcore.patching.PatchManager;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Created by fahslaj on 5/9/2016.
 */
public class TestFixedSizeWritingQueue {

    private static final String testFile = "./src/test/resources/dataMgmtTestFiles/testWriting.json";

    @Test
    public void testOfferPatchSingle() {
        PatchManager manager = mock(PatchManager.class);
        FixedSizeWritingQueue queue= new FixedSizeWritingQueue(manager);
        Patch patch = mock(Patch.class);
        queue.offerPatch(patch, testFile);
        verify(manager, times(0)).applyPatch(anyString(), anyListOf(Patch.class));
    }

    @Test
    public void testOfferPatchMany() throws IOException, InterruptedException {
        File f = new File(testFile);
        f.createNewFile();
        PatchManager manager = mock(PatchManager.class);
        final int[] count = {0};
        when(manager.applyPatch(anyString(), anyList())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                count[0]++;
                return null;
            }
        });
        FixedSizeWritingQueue queue= new FixedSizeWritingQueue(manager);
        Patch patch = mock(Patch.class);
        for (int i = 0; i < 7; i++)
            queue.offerPatch(patch, testFile);
        Thread.sleep(50);
        Assert.assertEquals(count[0], 1);
        f.delete();
    }

    @Test
    public void testOfferPatchManyMany() throws IOException, InterruptedException {
        File f = new File(testFile);
        f.createNewFile();
        PatchManager manager = mock(PatchManager.class);
        final int[] count = {0};
        when(manager.applyPatch(anyString(), anyList())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                count[0]++;
                return null;
            }
        });
        FixedSizeWritingQueue queue= new FixedSizeWritingQueue(manager);
        Patch patch = mock(Patch.class);
        Patch[] array = {patch, patch, patch, patch, patch, patch, patch, patch, patch};
        queue.offerPatch(array, testFile);
        Thread.sleep(50);
        Assert.assertEquals(count[0], 1);
        f.delete();
    }
}
