package dataMgmt;

import patching.Patch;
import patching.PatchManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fahslaj on 5/5/2016.
 */
public class FileContentWriter {

    static final int WRITE_THRESHOLD = 5;

    private Map<Long, IFileWritingQueue> fileBuffers;
    private PatchManager patchManager;

    protected FileContentWriter(PatchManager patchManager) {
        fileBuffers = new HashMap<>();
        this.patchManager = patchManager;
    }

    public void enqueuePatchesForWriting(long fileId, String absolutePath, List<Patch> patches) {
        synchronized (FileContentWriter.class) {
            if (!fileBuffers.containsKey(fileId))
                fileBuffers.put(fileId, new FixedSizeWritingQueue(patchManager));

            IFileWritingQueue fsq = fileBuffers.get(fileId);
            for (Patch patch : patches){
                fsq.offerPatch(patch, absolutePath);
            }
        }
    }
}
