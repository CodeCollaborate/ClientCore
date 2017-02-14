package clientcore.dataMgmt;

import clientcore.patching.Patch;
import clientcore.patching.PatchManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * An implementation of an IFileWritingQueue that flushes patches to the file
 * after a fixed number of patches are received.
 * Created by fahslaj on 5/7/2016.
 */
public class FixedSizeWritingQueue implements IFileWritingQueue {

    // The absolute path of the file for which this queue is tracking patches
    private String absolutePath;

    // The implementation of the queue of patches
    private LinkedList<Patch> queue;

    // The PatchManager to use to patch the file
    private PatchManager patchManager;

    // Tracks whether a process is currently applying patches to the file
    private volatile boolean writing = false;

    /**
     * Creates a FixedSizeWritingQueue that patches with the given PatchManager.
     * @param patchManager the PatchManager to use to patch the file
     */
    public FixedSizeWritingQueue(PatchManager patchManager) {
        queue = new LinkedList<>();
        this.patchManager = patchManager;
    }

    /**
     * Offer a patch for the queue to track and specify the updated absolute path
     * of the file for if the patches need to be flushed to disc.
     * @param patch the patch to offer to the queue
     * @param absolutePath the new absolute path of the file tracked by this queue
     * @return true if successfully added the patch, false if adding the patch failed
     */
    public boolean offerPatch(Patch patch, String absolutePath) {
        if (!absolutePath.equals(this.absolutePath) && absolutePath != null)
            this.absolutePath = absolutePath;

        boolean success = queue.offer(patch);

        writePatchestoFileIfNeeded();

        return success;
    }

    /**
     * Offer a patch for the queue to track and specify the updated absolute path
     * of the file for if the patches need to be flushed to disc.
     * @param patches the patches to offer to the queue
     * @param absolutePath the new absolute path of the file tracked by this queue
     * @return true if successfully added the patches, false if adding the patch failed
     */
    public boolean offerPatch(Patch[] patches, String absolutePath) {
        if (!absolutePath.equals(this.absolutePath))
            this.absolutePath = absolutePath;

        boolean success = true;

        for (Patch patch : patches) {
            success = success && queue.offer(patch);
        }

        writePatchestoFileIfNeeded();

        return success;
    }

    // applies the patches to the file if the queue length exceeds the WRITE_THRESHOLD
    private void writePatchestoFileIfNeeded() {
        if (queue.size() >= FileContentWriter.WRITE_THRESHOLD) {
            synchronized (this) {
                if (!writing) {
                    List<Patch> patchesToWrite = new ArrayList<>();
                    for (int i = 0; i < queue.size(); i++) {
                        patchesToWrite.add(queue.get(i));
                    }

                    writing = true;
                    new Thread(new FileWritingTask(absolutePath, patchesToWrite)).start();
                }
            }
        }
    }

    /**
     * A task for reading a file, applying a list of patches, and writing the file
     */
    protected class FileWritingTask implements Runnable {

        // the file path of the file to patch
        private String filePath;

        // the list of patches to apply
        private List<Patch> patches;

        /**
         * Creates a FileWritingTask that will apply the list of patches "patches" to the file at the location
         * "filePath"
         * @param filePath the path of the file to patch
         * @param patches the patches to apply to the file
         */
        public FileWritingTask(String filePath, List<Patch> patches) {
            this.filePath = filePath;
            this.patches = patches;
        }

        @Override
        public void run() {
            String fileContents = "";
            File file = new File(filePath);
            try {
                Scanner scanny = new Scanner(file).useDelimiter("\\Z");
                if (scanny.hasNext())
                    fileContents = scanny.next();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                synchronized (this) {
                    writing = false;
                }
                return;
            }
            String resultContents = patchManager.applyPatch(fileContents, patches);
            try {
                FileWriter writer = new FileWriter(file);
                writer.write(resultContents);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                synchronized (this) {
                    writing = false;
                }
                return;
            }

            synchronized (this) {
                patches.forEach(s -> queue.remove(s));
                writing = false;
            }
        }
    }
}
