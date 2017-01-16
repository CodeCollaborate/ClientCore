package patching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websocket.*;
import websocket.models.Notification;
import websocket.models.Request;
import websocket.models.notifications.FileChangeNotification;
import websocket.models.requests.FileChangeRequest;
import websocket.models.responses.FileChangeResponse;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by fahslaj on 5/5/2016.
 */
public class PatchManager implements INotificationHandler {
    static long PATCH_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private static final Logger logger = LoggerFactory.getLogger("patching");
    private final HashMap<Long, BatchingControl> batchingByFile = new HashMap<>();
    private WSManager wsMgr;
    private IFileChangeNotificationHandler notifHandler;
    private LinkedBlockingQueue<Notification> notificationHandlerQueue = new LinkedBlockingQueue<>();

    public PatchManager() {
        runNotificationHandlerThread();
    }

    public void setWsMgr(WSManager wsMgr) {
        this.wsMgr = wsMgr;
    }

    public void setNotifHandler(IFileChangeNotificationHandler notifHandler) {
        this.notifHandler = notifHandler;
    }

    private BatchingControl getBatchingControl(long fileID) {
        if (!batchingByFile.containsKey(fileID)) {
            synchronized (batchingByFile) {
                if (!batchingByFile.containsKey(fileID)) {
                    BatchingControl batchingControl = new BatchingControl();
                    batchingByFile.put(fileID, batchingControl);
                    batchingControl.maxVersionSeen = -1;
                }
            }
        }
        return batchingByFile.get(fileID);
    }

    public void sendPatch(long fileID, long baseFileVersion, Patch[] patches, IResponseHandler respHandler, IRequestSendErrorHandler sendErrHandler) {

        BatchingControl batchingCtrl = getBatchingControl(fileID);

        synchronized (batchingCtrl.patchBatchingPreQueue) {
            logger.debug(String.format("PatchManager: Adding %s to batching pre-queue; batching queue currently %s, batching pre-queue currently %s", Arrays.toString(patches), batchingCtrl.patchBatchingQueue, batchingCtrl.patchBatchingPreQueue).replace("\n", "\\n") + "\n");
            Collections.addAll(batchingCtrl.patchBatchingPreQueue, patches);
        }

        new Thread(() -> {
            Thread.currentThread().setName("TransformAndSendPatch");
            transformAndSendPatch(batchingCtrl, fileID, respHandler, sendErrHandler);
        }).start();
    }

    private void transformAndSendPatch(BatchingControl batchingCtrl, long fileID, IResponseHandler respHandler, IRequestSendErrorHandler sendErrHandler) {
        // If could not acquire a permit, exit; there is already a thread running this.
        if (!batchingCtrl.batchingSem.tryAcquire()) {
            logger.debug("Request already in flight; returning");
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
                    if (released) {
                        return;
                    }

                    batchingCtrl.batchingSem.drainPermits();
                    batchingCtrl.batchingSem.release();

                    synchronized (batchingCtrl) {
                        batchingCtrl.activeChangeRequest = false;
                        batchingCtrl.notifyAll();
                    }

                    released = true;
                }

                // Immediately send a request if queue non-empty
                boolean hasNext;
                synchronized (batchingCtrl.patchBatchingPreQueue) {
                    hasNext = !batchingCtrl.patchBatchingPreQueue.isEmpty();
                }
                synchronized (batchingCtrl.patchBatchingQueue) {
                    hasNext = hasNext || !batchingCtrl.patchBatchingQueue.isEmpty();
                }

                if (hasNext) {
                    logger.debug(String.format("PatchManager-Releaser: Starting next patch-batch; current batching queue is: %s, batching pre-queue currently %s", batchingCtrl.patchBatchingQueue, batchingCtrl.patchBatchingPreQueue).replace("\n", "\\n") + "\n");
                    transformAndSendPatch(batchingCtrl, fileID, respHandler, sendErrHandler);
                } else {
                    logger.debug("PatchManager-Releaser: No next patch-batch; batching and pre-batching queues empty");
                }

            }
        };

        Patch[] patches;
        String[] patchStrings;

        // Send request
        synchronized (batchingCtrl.patchBatchingQueue) {
            synchronized (batchingCtrl.patchBatchingPreQueue) {
                batchingCtrl.patchBatchingQueue.addAll(batchingCtrl.patchBatchingPreQueue);
                batchingCtrl.patchBatchingPreQueue.clear();
            }
            patches = batchingCtrl.patchBatchingQueue.toArray(new Patch[batchingCtrl.patchBatchingQueue.size()]);
            patchStrings = new String[batchingCtrl.patchBatchingQueue.size()];
            logger.debug(String.format("PatchManager: Sending patches %s", batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
        }

        if (patches.length == 0) {
            batchingCtrl.batchingSem.release();
            return;
        }

        // Transform patches against missing patches before sending
        String[] missingPatches = batchingCtrl.lastResponsePatches.clone();
        for (int i = 0; i < patches.length; i++) {
            long maxMissingPatchBaseVersion = patches[i].getBaseVersion() - 1;
            for (int j = 0; j < missingPatches.length; j++) {
                Patch missingPatch = new Patch(missingPatches[j]);

                // If the new patch's base version is earlier or equal, we need to update it.
                // If the base versions are the same, the server patch wins.
                if (patches[i].getBaseVersion() <= missingPatch.getBaseVersion()) {
                    logger.debug(String.format("PatchManager: Transforming %s against missing patch %s", patches[i].toString(), missingPatch).replace("\n", "\\n") + "\n");

                    long patchBaseVersion = patches[i].getBaseVersion();
                    patches[i] = patches[i].transform(false, missingPatch);
                    patches[i].setBaseVersion(patchBaseVersion);

                    maxMissingPatchBaseVersion = Math.max(maxMissingPatchBaseVersion, missingPatch.getBaseVersion());

                    // Transform missingPatch against new patch, so blocks stay together
                    // New patch has precedence, since it inserts in it's actual place
                    long missingPatchBaseVersion = missingPatch.getBaseVersion();
                    missingPatch = missingPatch.transform(true, patches[i]);
                    missingPatch.setBaseVersion(missingPatchBaseVersion);
                    missingPatches[j] = missingPatch.toString();
                }
            }
            patches[i].setBaseVersion(Math.max(patches[i].getBaseVersion(), maxMissingPatchBaseVersion + 1));

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
                            logger.debug(String.format("PatchManager: Removing patches %s; patch queue is currently %s", batchingCtrl.patchBatchingQueue.subList(0, patches.length), batchingCtrl.patchBatchingQueue).replace("\n", "\\n"));
                            logger.debug(String.format("PatchManager: Removing patches %s; patch done queue is currently %s", batchingCtrl.patchBatchingQueue.subList(0, patches.length), batchingCtrl.patchDoneQueue).replace("\n", "\\n"));
                            // Remove the sent patches
                            for (int i = 0; i < patches.length; i++) {
                                batchingCtrl.patchBatchingQueue.remove(0);
                            }
                            for (String change : ((FileChangeResponse) response.getData()).getChanges()) {
                                batchingCtrl.patchDoneQueue.add(new Patch(change));
                            }
                            logger.debug(String.format("PatchManager: patch queue is currently %s, patch done queue is currently %s", batchingCtrl.patchBatchingQueue, batchingCtrl.patchDoneQueue).replace("\n", "\\n"));
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


        synchronized (batchingCtrl) {
            batchingCtrl.activeChangeRequest = true;
        }

        wsMgr.sendAuthenticatedRequest(req);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.debug("PatchManager: Request timed out, running releaser.");
                releaser.run();
            }
        }, PATCH_TIMEOUT_MILLIS);
    }

    // This has to be in a separate thread so that we can have a queue to make sure
    // notifications are applied in the order they are received. Otherwise the threads waiting for the current
    // change requests could wake in the wrong order.
    private void runNotificationHandlerThread() {
        new Thread(() -> {
            Thread.currentThread().setName("PatchManagerNotificationHandler");

            while (true) {
                Notification notification = null;
                try {
                    notification = notificationHandlerQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    continue;
                }

                FileChangeNotification fileChangeNotif = (FileChangeNotification) notification.getData();
                long fileID = notification.getResourceID();

                BatchingControl batchingCtrl = getBatchingControl(fileID);
                if (batchingCtrl.activeChangeRequest) {
                    synchronized (batchingCtrl) {
                        while (batchingCtrl.activeChangeRequest) {
                            try {
                                batchingCtrl.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                // If we can't get control, and we are interrupted, something is majorly broken
                                // The best we can do to try and recover is apply the patch anyways.
                                break;
                            }
                        }
                    }
                }

                while (true) {
                    long expectedModificationStamp;

                    synchronized (batchingCtrl.patchBatchingQueue) {
                        synchronized (batchingCtrl.patchBatchingPreQueue) {
                            batchingCtrl.patchBatchingQueue.addAll(batchingCtrl.patchBatchingPreQueue);
                            batchingCtrl.patchBatchingPreQueue.clear();

                            synchronized (batchingCtrl) {
                                expectedModificationStamp = batchingCtrl.expectedModificationStamp;
                            }
                        }

                        String[] oldchanges = fileChangeNotif.changes.clone();
                        String[] changes = fileChangeNotif.changes;

                        long maxBaseVersionSeen = 0;
                        ArrayList<Patch> transformedPatchDoneQueue = (ArrayList<Patch>) batchingCtrl.patchDoneQueue.clone();
                        ArrayList<Patch> transformedPatchBatchingQueue = (ArrayList<Patch>) batchingCtrl.patchBatchingQueue.clone();

                        for (int i = 0; i < changes.length; i++) {
                            Patch patch = new Patch(changes[i]);

                            logger.debug(String.format("PatchManager-Notification: Transforming %s against doneQueue %s", patch, transformedPatchDoneQueue).replace("\n", "\\n"));
                            logger.debug(String.format("PatchManager-Notification: Transforming doneQueue %s against %s", transformedPatchDoneQueue, patch).replace("\n", "\\n"));

                            long patchMaxVersion = patch.getBaseVersion();

                            for (int j = 0; j < transformedPatchDoneQueue.size(); j++) {
                                Patch donePatch = transformedPatchDoneQueue.get(j);
                                if (donePatch.getBaseVersion() >= patch.getBaseVersion()) {
                                    // Save patchBaseVersion, reset after transform. This prevents the bug where if the doneQueue has
                                    // more than one patch based on the same version, the transformation on the first one changes the
                                    // base version of the incoming patch, and thus the subsequent donePatches are never transformed against
                                    long patchBaseVersion = patch.getBaseVersion();
                                    patch = patch.transform(true, donePatch);
                                    patchMaxVersion = Math.max(patchMaxVersion, patch.getBaseVersion());
                                    patch.setBaseVersion(patchBaseVersion);

                                    // Transform donePatch against new patch, so blocks stay together
                                    // New patch has precedence, since it inserts in it's actual place
                                    long donePatchBaseVersion = donePatch.getBaseVersion();
                                    donePatch = donePatch.transform(false, patch);
                                    donePatch.setBaseVersion(donePatchBaseVersion);
                                    transformedPatchDoneQueue.set(j, donePatch);
                                }
                            }
                            maxBaseVersionSeen = Math.max(maxBaseVersionSeen, patch.getBaseVersion());

                            logger.debug(String.format("PatchManager-Notification: Transforming %s against batchingQueue %s", patch, transformedPatchBatchingQueue).replace("\n", "\\n"));

                            // All patches in batching queue here are guaranteed to be coming after the patch, since we wait for the current request to complete.
                            long patchBaseVersion = patch.getBaseVersion();
                            patch = patch.transform(true, transformedPatchBatchingQueue);
                            patchMaxVersion = Math.max(patchMaxVersion, patch.getBaseVersion());
                            patch.setBaseVersion(patchBaseVersion);

                            logger.debug(String.format("PatchManager-Notification: Transforming batchingQueue %s against %s", transformedPatchBatchingQueue, patch).replace("\n", "\\n"));

                            // Transform all patches in batching queue against this one; also saves work on the sending end.
                            for (int j = 0; j < transformedPatchBatchingQueue.size(); j++) {
                                Patch queuedPatch = transformedPatchBatchingQueue.get(j);

                                // Transform queuedPatch against new patch, so blocks stay together
                                // New patch has precedence, since it inserts in it's actual place
                                queuedPatch = queuedPatch.transform(false, patch);
                                transformedPatchBatchingQueue.set(j, queuedPatch);
                            }

                            patch.setBaseVersion(patchMaxVersion);

                            logger.debug(String.format("PatchManager-Notification: Transformed %s against done and batching queues; result: %s", changes[i], patch).replace("\n", "\\n"));

                            // Changes the ones in the notification as well.
                            changes[i] = patch.toString();
                        }

                        Long result = notifHandler.handleNotification(notification, expectedModificationStamp);

                        // Only if we succeeded should we break out and continue to next patch.
                        // Otherwise, release lock, and try again after new changes are added.
                        if (result != null) {
                            batchingCtrl.expectedModificationStamp = result;

                            // Update all the patches in the done and batching queues
                            for (int i = 0; i < transformedPatchDoneQueue.size(); i++) {
                                batchingCtrl.patchDoneQueue.set(i, transformedPatchDoneQueue.get(i));
                            }
                            for (int i = 0; i < transformedPatchBatchingQueue.size(); i++) {
                                batchingCtrl.patchBatchingQueue.set(i, transformedPatchBatchingQueue.get(i));
                            }

                            // Remove all patches in doneQueue that we no longer need.
                            Iterator<Patch> itr = batchingCtrl.patchDoneQueue.iterator();
                            while (itr.hasNext()) {
                                Patch donePatch = itr.next();
                                if (donePatch.getBaseVersion() < maxBaseVersionSeen) {
                                    itr.remove();
                                }
                            }
                            break;
                        } else {
                            System.arraycopy(oldchanges, 0, changes, 0, changes.length);
                            logger.debug(String.format("PatchManager - Document changed between notification arrival and attempt to append. Retrying changes: %s", Arrays.asList(changes)).replace("\n", "\\n"));
                            continue;
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public void handleNotification(Notification notification) {
        try {
            notificationHandlerQueue.put(notification);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

    public void setModificationStamp(long fileID, long modificationStamp) {
        BatchingControl batchingCtrl = getBatchingControl(fileID);
        synchronized (batchingCtrl) {
            batchingCtrl.expectedModificationStamp = modificationStamp;
        }
    }

    private class BatchingControl {
        final Semaphore batchingSem = new Semaphore(1);
        private final ArrayList<Patch> patchBatchingQueue = new ArrayList<>();
        private final ArrayList<Patch> patchBatchingPreQueue = new ArrayList<>();
        private final ArrayList<Patch> patchDoneQueue = new ArrayList<>();
        String[] lastResponsePatches = new String[0];
        long maxVersionSeen = -1;
        private boolean activeChangeRequest = false;
        private long expectedModificationStamp = -1;
    }
}
