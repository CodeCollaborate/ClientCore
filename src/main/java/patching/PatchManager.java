package patching;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import websocket.INotificationHandler;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.WSManager;
import websocket.models.Notification;
import websocket.models.Request;
import websocket.models.notifications.FileChangeNotification;
import websocket.models.requests.FileChangeRequest;
import websocket.models.responses.FileChangeResponse;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by fahslaj on 5/5/2016.
 */
public class PatchManager implements INotificationHandler {
    public static Logger logger = LogManager.getLogger("patching");
    final HashMap<Long, BatchingControl> batchingByFile = new HashMap<>();
    private WSManager wsMgr;
    private INotificationHandler notifHandler;

    public void setWsMgr(WSManager wsMgr) {
        this.wsMgr = wsMgr;
    }

    public void setNotifHandler(INotificationHandler notifHandler) {
        this.notifHandler = notifHandler;
    }

    public void sendPatch(long fileID, long baseFileVersion, Patch[] patches, IResponseHandler respHandler, IRequestSendErrorHandler sendErrHandler) {
        if (!batchingByFile.containsKey(fileID)) {
            BatchingControl batchingControl = new BatchingControl();
            batchingByFile.put(fileID, batchingControl);
            batchingControl.maxVersionSeen = baseFileVersion;
        }
        BatchingControl batchingCtrl = batchingByFile.get(fileID);

        synchronized (batchingCtrl.patchBatchingQueue) {
            logger.debug(String.format("PatchManager: Adding %s to batching queue; batching queue currently %s", Arrays.toString(patches), batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
            Collections.addAll(batchingCtrl.patchBatchingQueue, patches);
        }

        transformAndSendPatch(batchingCtrl, fileID, respHandler, sendErrHandler);
    }

    private void transformAndSendPatch(BatchingControl batchingCtrl, long fileID, IResponseHandler respHandler, IRequestSendErrorHandler sendErrHandler) {

        // If could not acquire a permit, exit; there is already a thread running this.
        if (!batchingCtrl.batchingSem.tryAcquire()) {
            return;
        }

        Timer timer = new Timer();
        // Create releaser to make sure it only is ever done once
        Runnable releaser = new Runnable() {
            final Object synchronizationObj = new Object();
            boolean released = false;

            @Override
            public void run() {
                synchronized (synchronizationObj) {
                    if (!released) {
                        batchingCtrl.batchingSem.drainPermits();
                        batchingCtrl.batchingSem.release();
                        released = true;
                    }
                }

                // Immediately send a request if queue non-empty
                boolean isEmpty;
                synchronized (batchingCtrl.patchBatchingQueue) {
                    isEmpty = batchingCtrl.patchBatchingQueue.isEmpty();
                }
                if (!isEmpty) {
                    logger.debug(String.format("PatchManager: Starting next patch-batch; current batching queue is:  %s", batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
                    transformAndSendPatch(batchingCtrl, fileID, respHandler, sendErrHandler);
                }
            }
        };

        Patch[] patches;
        String[] patchStrings;

        // Send request
        synchronized (batchingCtrl.patchBatchingQueue) {
            patches = batchingCtrl.patchBatchingQueue.toArray(new Patch[batchingCtrl.patchBatchingQueue.size()]);
            patchStrings = new String[batchingCtrl.patchBatchingQueue.size()];
            logger.debug(String.format("PatchManager: Sending patches %s", batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");

        }

        // Transform patches against missing patches before sending
        for (int i = 0; i < patches.length; i++) {
            ArrayList<Patch> toTransformAgainst = new ArrayList<>();
            for (int j = 0; j < batchingCtrl.lastResponsePatches.length; j++) {
                Patch pastPatch = new Patch(batchingCtrl.lastResponsePatches[j]);

                // If the new patch's base version is earlier or equal, we need to update it.
                // If the base versions are the same, the server patch wins.
                if (patches[i].getBaseVersion() <= pastPatch.getBaseVersion()) {
                    toTransformAgainst.add(pastPatch);
                }
            }

            // Do the transformations all at once; otherwise the base versions mess up the outcome.
            logger.debug(String.format("PatchManager: Transforming %s against missing patches %s", patches[i].toString(), toTransformAgainst).replace("\n", "\\n") + "\n");
            patches[i] = patches[i].transform(toTransformAgainst);

            // If the patch's base version is greater than the maxVersionSeen, we leave it's version as is; it has been built on a newer version than we can handle here.
            if (patches[i].getBaseVersion() < batchingCtrl.maxVersionSeen) {
                patches[i].setBaseVersion(batchingCtrl.maxVersionSeen); // Set this to the latest version we have.
            }
            patchStrings[i] = patches[i].toString();
        }

        // Save response data, and fire off the actual responseHandler
        Request req = new FileChangeRequest(fileID, patchStrings).getRequest(
                response -> {
                    if (response.getStatus() == 200) {
                        synchronized (batchingCtrl.patchBatchingQueue) {
                            logger.debug(String.format("PatchManager: Removing patches %s; patch queue is currently %s", batchingCtrl.patchBatchingQueue.subList(0, patches.length), batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
                            logger.debug(String.format("PatchManager: Removing patches %s; patch done queue is currently %s", batchingCtrl.patchBatchingQueue.subList(0, patches.length), batchingCtrl.patchDoneQueue).replace("\n", "\\n") + "\n");
                            // Remove the sent patches
                            for (int i = 0; i < patches.length; i++) {
                                batchingCtrl.patchDoneQueue.add(batchingCtrl.patchBatchingQueue.remove(0));
                            }

                        }
                        if (((FileChangeResponse) response.getData()).getMissingPatches() != null) {
                            batchingCtrl.lastResponsePatches = ((FileChangeResponse) response.getData()).getMissingPatches();
                            batchingCtrl.maxVersionSeen = ((FileChangeResponse) response.getData()).getFileVersion();
                        }
                        if (respHandler != null) {
                            respHandler.handleResponse(response);
                        }
                    }

                    logger.debug(String.format("PatchManager: File Change Success; running releaser. Changes sent: %s", Arrays.toString(patches)).replace("\n", "\\n") + "\n");
                    releaser.run();
                    timer.purge();
                    timer.cancel();
                }, sendErrHandler
        );

        wsMgr.sendAuthenticatedRequest(req);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.debug("PatchManager: Request timed out, running releaser.");
                releaser.run();
            }
        }, TimeUnit.SECONDS.toMillis(5));
    }

    @Override
    public void handleNotification(Notification notification) {
        FileChangeNotification fileChangeNotif = (FileChangeNotification) notification.getData();
        long fileID = notification.getResourceID();

        if (!batchingByFile.containsKey(fileID)) {
            BatchingControl batchingControl = new BatchingControl();
            batchingByFile.put(fileID, batchingControl);
            batchingControl.maxVersionSeen = -1;
        }
        BatchingControl batchingCtrl = batchingByFile.get(fileID);

        synchronized (batchingCtrl.patchBatchingQueue) {
            String[] changes = fileChangeNotif.changes;

            for (int i = 0; i < changes.length; i++) {
                Patch patch = new Patch(changes[i]);

                logger.debug(String.format("PatchManager-Notification: Transforming %s against doneQueue %s", patch, batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
                logger.debug(String.format("PatchManager-Notification: Transforming %s against batchingQueue %s", patch, batchingCtrl.patchDoneQueue).replace("\n", "\\n") + "\n");

                // In event that the
                Iterator<Patch> itr = batchingCtrl.patchDoneQueue.iterator();
                while(itr.hasNext()){
                    Patch donePatch = itr.next();
                    if(donePatch.getBaseVersion() >= patch.getBaseVersion()){
                        patch = patch.transform(donePatch);
                    } else {
                        // remove ones we don't care about anymore.
                        itr.remove();
                    }
                }

                patch = patch.transform(batchingCtrl.patchBatchingQueue);

                // Changes the ones in the notification as well.
                changes[i] = patch.toString();
            }
        }

        notifHandler.handleNotification(notification);
    }

    public String applyPatch(String content, List<Patch> patches) {
        boolean useCRLF = content.contains("\r\n");

        for (Patch patch : patches) {
            int startIndex = 0;
            StringBuilder sb = new StringBuilder();

            if (useCRLF) {
                patch.convertToCRLF(content);
            }
            for (Diff diff : patch.getDiffs()) {
                if (diff.getStartIndex() > 0 && diff.getStartIndex() < content.length()
                        && content.charAt(diff.getStartIndex() - 1) == '\r'
                        && content.charAt(diff.getStartIndex()) == '\n') {
                    throw new IllegalArgumentException("Tried to insert between \\r and \\n");
                }

                // Copy anything before the changes
                if (startIndex < diff.getStartIndex()) {
                    sb.append(content.substring(startIndex, diff.getStartIndex()));
                }

                if (diff.isInsertion()) {
                    // insert item
                    sb.append(diff.getChanges());

                    // If the diff's startIndex is greater, move it up.
                    // Otherwise, a previous delete may have deleted over the start index.
                    if (startIndex < diff.getStartIndex()) {
                        startIndex = diff.getStartIndex();
                    }
                } else {
                    // validate that we're deleting the right characters
                    if (!diff.getChanges().equals(content.substring(diff.getStartIndex(), diff.getStartIndex() + diff.getLength()))) {
                        throw new IllegalStateException(
                                String.format("PatchManager.ApplyText: Deleted text %s does not match changes in diff: %s",
                                        content.substring(diff.getStartIndex(), diff.getStartIndex() + diff.getLength()), diff.getChanges()));
                    }

                    // shift the start index of the next round
                    startIndex = diff.getStartIndex() + diff.getLength();
                }
            }

            sb.append(content.substring(startIndex));
            content = sb.toString();
        }

        return content;
    }

    private class BatchingControl {
        final Semaphore batchingSem = new Semaphore(1);
        private final ArrayList<Patch> patchBatchingQueue = new ArrayList<>();
        private final ArrayList<Patch> patchDoneQueue = new ArrayList<>();
        String[] lastResponsePatches = new String[0];
        long maxVersionSeen = -1;
    }
}
