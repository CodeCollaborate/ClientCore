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

    private Object lock;
    private boolean writing = false;

    public FixedSizeWritingQueue() {
        queue = new LinkedList<>();
    }

    public boolean offerPatch(Patch e, String absolutePath) {
        if (!absolutePath.equals(this.absolutePath))
            this.absolutePath = absolutePath;

        if (queue.size() + 1 < FileContentWriter.WRITE_THRESHOLD)
            return queue.offer(e);

        synchronized (lock) {
            if (writing)
                return queue.offer(e);

            List<Patch> patchesToWrite = new ArrayList<>();
            for (int i = 0; i < queue.size(); i++) {
                patchesToWrite.add(queue.get(i));
            }

            writing = true;
            new Thread(new FileWritingTask(absolutePath, patchesToWrite)).start();
        }
        return queue.offer(e);
    }

    private class FileWritingTask implements Runnable {

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
                synchronized (lock) {
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
                synchronized (lock) {
                    writing = false;
                }
                return;
            }

            synchronized (lock) {
                patches.forEach(s -> queue.remove(s));
                writing = false;
            }
        }
    }
}
