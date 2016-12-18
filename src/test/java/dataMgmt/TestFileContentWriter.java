package dataMgmt;

import org.junit.Assert;
import org.junit.Test;
import patching.Patch;
import patching.PatchManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Created by fahslaj on 5/9/2016.
 */
public class TestFileContentWriter {

//    @Test
//    public void testEnqueueAlreadyInBuffer() {
//        PatchManager manager = mock(PatchManager.class);
//        FileContentWriter writer = new FileContentWriter(manager);
//        IFileWritingQueue mocky = mock(FixedSizeWritingQueue.class);
//        writer.fileBuffers.put(Long.parseLong("100"), mocky);
//        ArrayList<Patch> patches = new ArrayList<>();
//        patches.add(mock(Patch.class));
//        writer.enqueuePatchesForWriting(Long.parseLong("100"), "path", patches);
//        verify(mocky, times(1)).offerPatch(any(Patch.class), anyString());
//    }
//
//    @Test
//    public void testEnqueueNotInBuffer() {
//        PatchManager manager = mock(PatchManager.class);
//        FileContentWriter writer = new FileContentWriter(manager);
//        ArrayList<Patch> patches = new ArrayList<>();
//        patches.add(mock(Patch.class));
//        writer.enqueuePatchesForWriting(Long.parseLong("100"), "path", patches);
//        Assert.assertNotNull(writer.fileBuffers.get(Long.parseLong("100")));
//    }
    private static final String testFilePath = "./src/test/resources/dataMgmtTestFiles/fileContentWriterTest.txt";

    @Test
    public void testEnqueueWriteToFile() throws IOException {
        String fileContents = "hello";
        String futureContents = "goodbye";
        try (FileOutputStream stream = new FileOutputStream(testFilePath)) {
            stream.write(fileContents.getBytes());
        }
        PatchManager manager = mock(PatchManager.class);
        FileContentWriter writer = new FileContentWriter(manager);
        List<Patch> patches = new ArrayList<>();
        patches.add(mock(Patch.class));
        when(manager.applyPatch(fileContents, patches)).thenReturn(futureContents);
        writer.enqueuePatchesForWriting(Long.parseLong("100"), testFilePath, patches);
        verify(manager, times(1)).applyPatch(fileContents, patches);
        String newContents;
        byte[] data = Files.readAllBytes(Paths.get(testFilePath));
        newContents = new String(data);
        Assert.assertEquals(futureContents, newContents);
        File file = new File(testFilePath);
        file.delete();
    }
}
