package patching;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import websocket.INotificationHandler;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.WSManager;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PatchManager implements INotificationHandler {
    public static Logger logger = LogManager.getLogger("patching");
    static long PATCH_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5);

    // Threading controls
    private final HashMap<Long, BatchingControl> batchingByFile = new HashMap<>();
    private final ReadWriteLock handlingNotificationsLock = new ReentrantReadWriteLock(true); // Fair lock, to make sure no thread gets starved.
    private final LinkedBlockingQueue<Notification> notificationHandlerQueue = new LinkedBlockingQueue<>();

    // References to external modules
    private WSManager wsMgr;
    private IFileChangeNotificationHandler notifHandler;

    public PatchManager() {
        runNotificationHandlerThread();
    }

    /**
     * Sets the internal websocket manager used to send requests
     *
     * @param wsMgr the websocket manager that should be used to send requests
     */
    public void setWsMgr(WSManager wsMgr) {
        this.wsMgr = wsMgr;
    }

    /**
     * Sets the notification handler that should control writing to documents or editors
     *
     * @param notifHandler the IFileChangeNotificationHandler instance that controls writing to file or editor
     */
    public void setNotifHandler(IFileChangeNotificationHandler notifHandler) {
        this.notifHandler = notifHandler;
    }

    /**
     * Gets the batching control for the given fileID, initializing it if needed.
     *
     * @param fileID fileID to get BatchingControl for
     * @return the BatchingControl instance mapped to the provided fileID
     */
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

    /**
     * Enqueues a patch for sending. Will be batched to make sure that there is only ever one request in flight per fileID
     *
     * @param fileID         the fileID to which the patch corresponds to
     * @param patches        the list of patches to send
     * @param respHandler    the IResponseHandler that will handle the response object
     * @param sendErrHandler the IRequestSendErrHandler that will handle failures to send the request.
     */
    public void sendPatch(long fileID, Patch[] patches, IResponseHandler respHandler, IRequestSendErrorHandler sendErrHandler) {
        BatchingControl batchingCtrl = getBatchingControl(fileID);

        // Add to batching pre-queue, and then transfer to main BatchingQueue
        // This avoids deadlocking the UI, or running the transformAndSendPatch on UI threads.
        synchronized (batchingCtrl.patchBatchingPreQueue) {
            logger.debug(String.format("PatchManager: Adding %s to batching pre-queue; batching pre-queue currently %s", Arrays.toString(patches), batchingCtrl.patchBatchingPreQueue).replace("\n", "\\n") + "\n");
            Collections.addAll(batchingCtrl.patchBatchingPreQueue, patches);
        }

        // Run the transformAndSendPatch on a new thread to make sure the UI thread doesn't get blocked.
        new Thread(() -> {
            Thread.currentThread().setName("TransformAndSendPatch");
            transformAndSendPatch(batchingCtrl, fileID, respHandler, sendErrHandler);
        }).start();
    }

    /**
     * transformAndSendPatch does the necessary transformations on the patches before sending it. This updates
     * the patches to the latest version that we have, thus making sure that we stay in synchronization.
     *
     * @param batchingCtrl   the batchingCtrl for this file.
     * @param fileID         the fileID to which the patch corresponds to
     * @param respHandler    the IResponseHandler that will handle the response object
     * @param sendErrHandler the IRequestSendErrHandler that will handle failures to send the request.
     */
    private void transformAndSendPatch(BatchingControl batchingCtrl, long fileID, IResponseHandler respHandler, IRequestSendErrorHandler sendErrHandler) {
        // If could not acquire a permit, exit; there is already a thread running this.
        if (!batchingCtrl.batchingSem.tryAcquire()) {
            logger.debug("Request already in flight; returning");
            return;
        }

        // Take a read lock on the notifications; Any amount of transformAndSendPatch threads can run at the same time
        // but the notification handler must wait for any live transformAndSendPatch threads to finish before it can
        // execute.
        handlingNotificationsLock.readLock().lock();

        // Get a snapshot of the current list of patches, allowing other threads to add without blocking
        Patch[] patches;
        String[] patchStrings;
        synchronized (batchingCtrl.patchBatchingQueue) {
            // Add all in pre-queue before taking snapshot
            synchronized (batchingCtrl.patchBatchingPreQueue) {
                batchingCtrl.patchBatchingQueue.addAll(batchingCtrl.patchBatchingPreQueue);
                batchingCtrl.patchBatchingPreQueue.clear();
            }
            patches = batchingCtrl.patchBatchingQueue.toArray(new Patch[batchingCtrl.patchBatchingQueue.size()]);
            patchStrings = new String[batchingCtrl.patchBatchingQueue.size()];
            logger.debug(String.format("PatchManager: Sending patches %s", batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");
        }

        // If no patches found, exit after unlocking semaphores and locks
        if (patches.length == 0) {
            batchingCtrl.batchingSem.release();
            handlingNotificationsLock.readLock().unlock();
            return;
        }

        // Transform patches against missing patches before sending
        String[] missingPatches = batchingCtrl.lastResponsePatches.clone(); // clone to make sure that we don't overwrite if this request fails.
        for (int i = 0; i < patches.length; i++) {
            // Keep track of highest missing patch base version, in order to set it at the end.
            long maxMissingPatchBaseVersion = -1;

            for (int j = 0; j < missingPatches.length; j++) {
                Patch missingPatch = new Patch(missingPatches[j]);

                // If the new patch's base version is earlier or equal, we need to update it.
                // If the base versions are the same, the new patch takes precedence, inserting BEFORE the server patch
                // as needed.
                if (patches[i].getBaseVersion() <= missingPatch.getBaseVersion()) {
                    logger.debug(String.format("PatchManager: Transforming %s against missing patch %s", patches[i].toString(), missingPatch).replace("\n", "\\n") + "\n");

                    // Transform outgoing patch against missing patches
                    long patchBaseVersion = patches[i].getBaseVersion();
                    patches[i] = patches[i].transform(false, missingPatch);
                    patches[i].setBaseVersion(patchBaseVersion);

                    maxMissingPatchBaseVersion = Math.max(maxMissingPatchBaseVersion, missingPatch.getBaseVersion());

                    // Transform missingPatch against new patch, so blocks stay together
                    // New patch has precedence, and inserts in it's designated place, shifting the server patch back.
                    long missingPatchBaseVersion = missingPatch.getBaseVersion();
                    missingPatch = missingPatch.transform(true, patches[i]);
                    missingPatch.setBaseVersion(missingPatchBaseVersion);
                    missingPatches[j] = missingPatch.toString();
                }
            }
            // Set baseVersion as the max of the original patch version, the highest response version, or the version generated by the latest missing patch
            patches[i].setBaseVersion(Math.max(batchingCtrl.maxVersionSeen, Math.max(patches[i].getBaseVersion(), maxMissingPatchBaseVersion + 1)));

            patchStrings[i] = patches[i].toString();
        }

        // Save response data, and fire off the actual responseHandler
        Semaphore requestInFlightSem = new Semaphore(0);
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

                            // Add server-acknowledged patches to doneQueue
                            for (String change : ((FileChangeResponse) response.getData()).getChanges()) {
                                batchingCtrl.patchDoneQueue.add(new Patch(change));
                            }
                            logger.debug(String.format("PatchManager: patch queue is currently %s, patch done queue is currently %s", batchingCtrl.patchBatchingQueue, batchingCtrl.patchDoneQueue).replace("\n", "\\n"));
                        }

                        // Save missing patches & maxVersionSeen
                        if (((FileChangeResponse) response.getData()).getMissingPatches() != null) {
                            batchingCtrl.lastResponsePatches = ((FileChangeResponse) response.getData()).getMissingPatches();
                            batchingCtrl.maxVersionSeen = ((FileChangeResponse) response.getData()).getFileVersion();
                        }

                        // Fire actual response handler
                        if (respHandler != null) {
                            respHandler.handleResponse(response);
                        }
                    }

                    logger.debug(String.format("PatchManager: File Change Success; running releaser. Changes sent: %s", Arrays.toString(patches)).replace("\n", "\\n") + "\n");
                    requestInFlightSem.release();
                }, sendErrHandler
        );

        wsMgr.sendAuthenticatedRequest(req);

        // Wait for up to PATCH_TIMEOUT_MILLIS for the response. After which, assume network failure and try again.
        try {
            long startTime = System.currentTimeMillis();
            if (!requestInFlightSem.tryAcquire(PATCH_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                logger.debug("PatchManager: Request timed out, running releaser.");
            }
            logger.debug(String.format("Waited %d millis for request to complete", System.currentTimeMillis() - startTime));
        } catch (InterruptedException e) {
            // If interrupted, simply continue.
        }

        // release semaphore to allow another request to run
        batchingCtrl.batchingSem.drainPermits();
        batchingCtrl.batchingSem.release();

        // Unlock notification handler
        handlingNotificationsLock.readLock().unlock();

        // Immediately send a request if queue non-empty
        boolean hasNext;
        synchronized (batchingCtrl.patchBatchingPreQueue) {
            hasNext = !batchingCtrl.patchBatchingPreQueue.isEmpty();
        }
        synchronized (batchingCtrl.patchBatchingQueue) {
            hasNext = hasNext || !batchingCtrl.patchBatchingQueue.isEmpty();
        }

        if (hasNext) {
            logger.debug("PatchManager-Releaser: Starting next patch-batch");
            transformAndSendPatch(batchingCtrl, fileID, respHandler, sendErrHandler);
        } else {
            logger.debug("PatchManager-Releaser: No next patch-batch; batching and pre-batching queues empty");
        }
    }

    // This has to be in a separate thread so that we can have a queue to make sure
    // notifications are applied in the order they are received. Otherwise the threads waiting for the current
    // change requests could wake/acquire locks in the wrong order.
    private void runNotificationHandlerThread() {
        new Thread(() -> {
            Thread.currentThread().setName("PatchManagerNotificationHandler");
            boolean hasWriteLock = false;

            // Loop forever
            while (true) {
                Notification notification;

                synchronized (notificationHandlerQueue) {
                    try {
                        // Unlock writeLock when notification queue is drained.
                        if (notificationHandlerQueue.isEmpty()) {
                            if (hasWriteLock) {
                                handlingNotificationsLock.writeLock().unlock();
                                hasWriteLock = false;
                            }
                            // Put this thread to sleep while waiting for more notifications.
                            notificationHandlerQueue.wait();
                        }

                        notification = notificationHandlerQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                if (!hasWriteLock) {
                    // Wait until all requests in flight have returned before processing notifications.
                    handlingNotificationsLock.writeLock().lock();
                    hasWriteLock = true;
                }

                FileChangeNotification fileChangeNotif = (FileChangeNotification) notification.getData();
                long fileID = notification.getResourceID();
                BatchingControl batchingCtrl = getBatchingControl(fileID);

                while (true) {
                    long expectedModificationStamp;

                    synchronized (batchingCtrl.patchBatchingQueue) {
                        // Add all in batchingPre-Queue, to make sure we transform against current document state
                        synchronized (batchingCtrl.patchBatchingPreQueue) {
                            batchingCtrl.patchBatchingQueue.addAll(batchingCtrl.patchBatchingPreQueue);
                            batchingCtrl.patchBatchingPreQueue.clear();
                        }

                        expectedModificationStamp = batchingCtrl.expectedModificationStamp.get();

                        // Take snapshot of current changes, so that if this fails, we don't start with a dirty set.
                        // If the write to editor fails, we restore the original set of changes into the fileChangeNotification
                        String[] oldChanges = fileChangeNotif.changes.clone();
                        String[] changes = fileChangeNotif.changes;

                        // Again, take clones to make sure we do not mess up the actual queues.
                        ArrayList<Patch> transformedPatchDoneQueue = (ArrayList<Patch>) batchingCtrl.patchDoneQueue.clone();
                        ArrayList<Patch> transformedPatchBatchingQueue = (ArrayList<Patch>) batchingCtrl.patchBatchingQueue.clone();

                        // Keep track of the max base version, and remove items from doneQueue later based on it.
                        long maxBaseVersionSeen = 0;

                        for (int i = 0; i < changes.length; i++) {
                            Patch patch = new Patch(changes[i]);

                            logger.debug(String.format("PatchManager-Notification: Transforming %s against doneQueue %s", patch, transformedPatchDoneQueue).replace("\n", "\\n"));
                            logger.debug(String.format("PatchManager-Notification: Transforming doneQueue %s against %s", transformedPatchDoneQueue, patch).replace("\n", "\\n"));

                            long patchMaxVersion = patch.getBaseVersion();

                            // Transform against the done queue
                            for (int j = 0; j < transformedPatchDoneQueue.size(); j++) {
                                Patch donePatch = transformedPatchDoneQueue.get(j);
                                if (donePatch.getBaseVersion() >= patch.getBaseVersion()) {
                                    // Save patchBaseVersion, reset after transform. This prevents the case where if the doneQueue has
                                    // more than one patch based on the same version, the transformation on the first one changes the
                                    // base version of the incoming patch, and thus the subsequent donePatches are never transformed against
                                    //
                                    // The donePatch is only transformed against if it has a higher (or equal) base version,
                                    // and as such, the donePatches have precedence.
                                    long patchBaseVersion = patch.getBaseVersion();
                                    patch = patch.transform(true, donePatch);
                                    patchMaxVersion = Math.max(patchMaxVersion, patch.getBaseVersion());
                                    patch.setBaseVersion(patchBaseVersion);

                                    // Transform donePatch against new patch, to update the donePatches against
                                    // new document state
                                    long donePatchBaseVersion = donePatch.getBaseVersion();
                                    donePatch = donePatch.transform(false, patch);
                                    donePatch.setBaseVersion(donePatchBaseVersion);
                                    transformedPatchDoneQueue.set(j, donePatch);
                                }
                            }
                            maxBaseVersionSeen = Math.max(maxBaseVersionSeen, patch.getBaseVersion());

                            logger.debug(String.format("PatchManager-Notification: Transforming %s against batchingQueue %s", patch, transformedPatchBatchingQueue).replace("\n", "\\n"));

                            // All patches in batching queue here are guaranteed to be coming after the patch, since we wait for the current request to complete.
                            // Thus, we apply all indiscriminately. Maintain base version, since they are already committed to the database
                            //
                            // Because all items in batchingQueue are guaranteed to come after the notification patch, they take precedence.
                            long patchBaseVersion = patch.getBaseVersion();
                            patch = patch.transform(true, transformedPatchBatchingQueue);
                            patchMaxVersion = Math.max(patchMaxVersion, patch.getBaseVersion());
                            patch.setBaseVersion(patchBaseVersion);

                            logger.debug(String.format("PatchManager-Notification: Transforming batchingQueue %s against %s", transformedPatchBatchingQueue, patch).replace("\n", "\\n"));

                            // Transform all patches in batching queue against this one. This maintains the correctness of the items in the batchingQueue
                            // against the new document state.
                            for (int j = 0; j < transformedPatchBatchingQueue.size(); j++) {
                                Patch queuedPatch = transformedPatchBatchingQueue.get(j);

                                // Transform queuedPatch against new patch. Since the batchingQueue is guaranteed to be
                                // coming after the notification patches, they have precedence.
                                //
                                // Also, do not reset base versions, since we want to know we have transformed against
                                // this file version.
                                queuedPatch = queuedPatch.transform(false, patch);
                                transformedPatchBatchingQueue.set(j, queuedPatch);
                            }

                            // Finally, set the base version. This cannot be set earlier, to make sure the batchingQueue gets the right baseVersion in the end.
                            patch.setBaseVersion(patchMaxVersion);

                            logger.debug(String.format("PatchManager-Notification: Transformed %s against done and batching queues; result: %s", changes[i], patch).replace("\n", "\\n"));

                            // Write the changes back to the FileChangeNotification
                            changes[i] = patch.toString();
                        }

                        // Pass the transformed patches to the actual handler that will take care of writing to document or file
                        Long result = notifHandler.handleNotification(notification, expectedModificationStamp);

                        // Only if we succeeded should we break out and continue to next patch.
                        // Otherwise, release lock, and try again after new changes are added.
                        if (result != null) {
                            batchingCtrl.expectedModificationStamp.set(result);

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
                            // If we failed, copy the actual changes back, overwriting our transformed set.
                            System.arraycopy(oldChanges, 0, changes, 0, changes.length);
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
            synchronized (notificationHandlerQueue) {
                notificationHandlerQueue.put(notification);
                notificationHandlerQueue.notifyAll();
            }
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
        getBatchingControl(fileID).expectedModificationStamp.set(modificationStamp);
    }

    private class BatchingControl {
        final Semaphore batchingSem = new Semaphore(1);
        private final ArrayList<Patch> patchBatchingQueue = new ArrayList<>();
        private final ArrayList<Patch> patchBatchingPreQueue = new ArrayList<>();
        private final ArrayList<Patch> patchDoneQueue = new ArrayList<>();
        String[] lastResponsePatches = new String[0];
        long maxVersionSeen = -1;
        //        private boolean activeChangeRequest = false;
        private AtomicLong expectedModificationStamp = new AtomicLong(-1);
    }
}
