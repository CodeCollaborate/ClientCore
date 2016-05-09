package dataMgmt;

import patching.Patch;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fahslaj on 5/5/2016.
 */
public class FileContentWriter {

    public static final int WRITE_THRESHOLD = 5;

    private static FileContentWriter instance;

    /**
     * Get the active instance of the FileContentWriter class.
     * @return the instance of the FileContentWriter class
     */
    public static FileContentWriter getInstance() {
        if (instance == null) {
            synchronized (FileContentWriter.class) {
                if (instance == null) {
                    instance = new FileContentWriter();
                }
            }
        }
        return instance;
    }

    private Map<Long, IFileWritingQueue> fileBuffers;

    private FileContentWriter() {
        fileBuffers = new HashMap<>();
    }

    public void enqueuePatchForWriting(long fileId, String absolutePath, Patch patch) {
        synchronized (FileContentWriter.class) {
            if (!fileBuffers.containsKey(fileId))
                fileBuffers.put(fileId, new FixedSizeWritingQueue());

            IFileWritingQueue fsq = fileBuffers.get(fileId);
            fsq.offerPatch(patch, absolutePath);
        }
    }
}
