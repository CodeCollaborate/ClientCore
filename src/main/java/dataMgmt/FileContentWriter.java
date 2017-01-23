package dataMgmt;

import patching.Patch;
import patching.PatchManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileContentWriter allows for the enqueueing of patches to be applied to a closed file on disk.
 * Created by fahslaj on 5/5/2016.
 */
public class FileContentWriter {
    public static Logger logger = LogManager.getLogger("datamgmt");

    // The number of patches to flush to disc at a time
    static final int WRITE_THRESHOLD = 5;

    // The mapping of fileIds to IFileWritingQueues for keeping file queues separate.
    protected Map<Long, IFileWritingQueue> fileBuffers;

    // The PatchManager to use for patching the files on the disc
    private PatchManager patchManager;

    /**
     * Creates a FileContentWriter with the given PatchManager to handle patching.
     * @param patchManager the PatchManager to use to patch files on the disc
     */
    protected FileContentWriter(PatchManager patchManager) {
        fileBuffers = new HashMap<>();
        this.patchManager = patchManager;
    }

    /**
     * Enqueues a list of patches to be applied to a closed file on the disc
     * with the given fileId and absolutePath.
     * @param fileId the fileId of the file to be patched
     * @param absolutePath the absolute path on the disc of the file to be patched
     * @param patches the patches to apply to the file
     */
    public void enqueuePatchesForWriting(long fileId, String absolutePath, List<Patch> patches) {
        synchronized (FileContentWriter.class) {
            File file = new File(absolutePath);
            if (!file.exists()) {
                logger.error("Cannot apply patches to non-existent file: " + absolutePath);
                return;
            }
            String contents = null;
            try {
                byte[] data = Files.readAllBytes(Paths.get(file.getPath()));
                contents = new String(data);
            } catch (IOException e) {
                logger.error("Error reading file: " + absolutePath);
            }

            String newContents = patchManager.applyPatch(contents, patches);
            FileWriter writer = null;
            try {
                writer = new FileWriter(file);
                writer.write(newContents);
                writer.close();
            } catch (IOException e) {
                logger.error("Error writing to file: " + absolutePath);
            }

//            if (!fileBuffers.containsKey(fileId))
//                fileBuffers.put(fileId, new FixedSizeWritingQueue(patchManager));
//
//            IFileWritingQueue fsq = fileBuffers.get(fileId);
//            for (Patch patch : patches){
//                fsq.offerPatch(patch, absolutePath);
//            }
        }
    }
}
