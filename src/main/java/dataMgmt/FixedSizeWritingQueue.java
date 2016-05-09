package dataMgmt;

import patching.Patch;
import patching.PatchManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by fahslaj on 5/7/2016.
 */
public class FixedSizeWritingQueue implements IFileWritingQueue {
    private String absolutePath;

    private LinkedList<Patch> queue;

    private volatile boolean writing = false;

    public FixedSizeWritingQueue() {
        queue = new LinkedList<>();
    }

    public boolean offerPatch(Patch patches, String absolutePath) {
        if (!absolutePath.equals(this.absolutePath))
            this.absolutePath = absolutePath;

        boolean success = queue.offer(patches);

        writePatchestoFileIfNeeded();

        return success;
    }

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

    protected class FileWritingTask implements Runnable {

        private String filePath;
        private List<Patch> patches;

        public FileWritingTask(String filePath, List<Patch> patches) {
            this.filePath = filePath;
            this.patches = patches;
        }

        @Override
        public void run() {
            String fileContents = "";
            File file = new File(filePath);
            try {
                fileContents = new Scanner(file).useDelimiter("\\Z").next();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                synchronized (this) {
                    writing = false;
                }
                return;
            }
            PatchManager manager = PatchManager.getInstance();
            String resultContents = manager.applyPatch(fileContents, patches);
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
