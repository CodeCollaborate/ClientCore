package clientcore.patching;

import clientcore.websocket.*;
import clientcore.websocket.models.File;
import clientcore.websocket.models.Notification;
import clientcore.websocket.models.Request;
import clientcore.websocket.models.notifications.FileChangeNotification;
import clientcore.websocket.models.requests.FileChangeRequest;
import clientcore.websocket.models.responses.FileChangeResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PatchManager implements INotificationHandler {
    public static final Logger logger = LogManager.getLogger("clientcore/patching");
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
     * Sets the internal clientcore.websocket manager used to send requests
     *
     * @param wsMgr the clientcore.websocket manager that should be used to send requests
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
                    batchingControl.currDocumentVersion = -1;
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
            Thread.currentThread().setName("TransformAndSendPatch-" + System.currentTimeMillis());
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
        // Add all in pre-queue before taking snapshot
        synchronized (batchingCtrl.patchBatchingPreQueue) {
            batchingCtrl.patchBatchingQueue.addAll(batchingCtrl.patchBatchingPreQueue);
            batchingCtrl.patchBatchingPreQueue.clear();
        }
        patches = batchingCtrl.patchBatchingQueue.toArray(new Patch[batchingCtrl.patchBatchingQueue.size()]);
        logger.debug(String.format("PatchManager: Sending patches %s", batchingCtrl.patchBatchingQueue).replace("\n", "\\n") + "\n");

        // If no patches found, exit after unlocking semaphores and locks
        if (patches.length == 0) {
            batchingCtrl.batchingSem.release();
            handlingNotificationsLock.readLock().unlock();
            return;
        }

        // TODO: Remove this once debugging is done
        long baseVersion = patches[0].getBaseVersion();
        for(Patch patch : patches){
            if (patch.getBaseVersion() != baseVersion){
                logger.fatal(String.format("PatchManager-Sending: Patch base versions were not all the same: %s", Arrays.toString(patches)));
                break;
            }
        }

        // Send patches
        // > Consolidate patches
        Patch consolidatedPatch = Consolidator.consolidatePatches(patches);

        // > Send (No need to transform, since we assume it has been done by either the response handler, or the notification handler
        Semaphore requestInFlightSem = new Semaphore(0);
        Request req = new FileChangeRequest(fileID, new String[]{consolidatedPatch.toString()}).getRequest(
                response -> {
                    if (response.getStatus() == 200) {
                        synchronized (batchingCtrl.patchBatchingQueue) {
                            logger.debug(String.format("PatchManager: Removing patches %s; patch queue is currently %s", batchingCtrl.patchBatchingQueue.subList(0, patches.length), batchingCtrl.patchBatchingQueue).replace("\n", "\\n"));

                            if (((FileChangeResponse) response.getData()).changes.length > 1){
                                logger.error(String.format("PatchManager: Patch had more than 1 change: %s", Arrays.toString(((FileChangeResponse) response.getData()).changes)));
                            }

                            // Remove the sent patches (Should only have 1, if consolidation works)
                            for(int i = 0; i < patches.length; i++){
                                batchingCtrl.patchBatchingQueue.remove(0);
                            }

                            // TODO: Evaluate if this is still necessary - might be able to be deleted.
//                            // Add server-acknowledged patches to doneQueue (Should only have 1, if consolidation works)
//                            batchingCtrl.patchDoneQueue.add(new Patch(((FileChangeResponse) response.getData()).changes[0]));
//                            logger.debug(String.format("PatchManager: patch queue is currently %s, patch done queue is currently %s", batchingCtrl.patchBatchingQueue, batchingCtrl.patchDoneQueue).replace("\n", "\\n"));
                        }

                        // BELOW HANDLES CASE WHERE WE GET THE RESPONSE OF A HIGHER VERSION BEFORE A NOTIFICATION OF A PRIOR VERSION:
                        // N1 -> R3 -> N2, BY APPLYING N2 BEFORE UPDATING TO VERSION 3.
                        //

                        Patch[] missingPatches = Patch.getPatches(((FileChangeResponse) response.getData()).missingPatches);
                        while(true) {
                            // Apply missing patches to document
                            // > Consolidate missing patches that are based on versions above current document version
                            int firstNewPatchIndex = 0;
                            for (; firstNewPatchIndex < missingPatches.length; firstNewPatchIndex++) {
                                if (batchingCtrl.currDocumentVersion >= missingPatches[firstNewPatchIndex].getBaseVersion()) {
                                    break;
                                }
                            }
                            Patch consolidatedMissingPatches = Consolidator.consolidatePatches(
                                    Arrays.copyOfRange(missingPatches, firstNewPatchIndex, missingPatches.length)
                            );

                            // > If no patches, return.
                            if(consolidatedMissingPatches == null){
                                break;
                            }

                            // > Get modification stamp of document
                            long expectedModificationStamp = batchingCtrl.expectedModificationStamp.get();

                            // > Drain batching pre-queue into batching queue
                            synchronized (batchingCtrl.patchBatchingPreQueue) {
                                batchingCtrl.patchBatchingQueue.addAll(batchingCtrl.patchBatchingPreQueue);
                                batchingCtrl.patchBatchingPreQueue.clear();
                            }

                            // > TODO: Validate that transforming against accepted patches instead of sent patches is correct
                            // > Transform missing patches against doneQueue (Others have priority, since they were newer)
                            Patch consolidatedDonePatches = Consolidator.consolidatePatches(
                                    Patch.getPatches(((FileChangeResponse) response.getData()).changes)
                            );
                            consolidatedMissingPatches = consolidatedMissingPatches.transform(true, consolidatedDonePatches);
                            // NOTE: There is no need to do reverse transformations here, since these done patches are never used again.

                            // > Transform against batching queue (Others have priority, since they have not been sent to server)
                            Patch consolidatedBatchedPatches = Consolidator.consolidatePatches(batchingCtrl.patchBatchingQueue);
                            consolidatedMissingPatches = consolidatedMissingPatches.transform(true, consolidatedBatchedPatches);

                            // > Reverse-transform batching queue against missing consolidated patch, save in temp
                            consolidatedBatchedPatches = consolidatedBatchedPatches.transform(false, consolidatedMissingPatches);

                            // > Apply missing consolidated patch to document, update document baseVersion to be this response's version
                            //     by generating "notification" of a new file change
                            Notification notification = new Notification("File", "Change", fileID,
                                    new FileChangeNotification(new String[]{consolidatedMissingPatches.toString()},
                                            ((FileChangeResponse) response.getData()).fileVersion,
                                            batchingCtrl.currDocumentVersion
                                    )
                            );

                            // > Attempt to optimistically change the file
                            //     Pass the transformed patches to the actual handler that will take care of writing to document or file
                            Long result = notifHandler.handleNotification(notification, expectedModificationStamp);

                            // > If our optimistic write was successful
                            if (result != null) {
                                batchingCtrl.expectedModificationStamp.set(result);

                                // >> Write reverse-transformed batching queue back.
                                consolidatedBatchedPatches.setBaseVersion(((FileChangeResponse) response.getData()).fileVersion);
                                batchingCtrl.patchBatchingQueue.clear();
                                batchingCtrl.patchBatchingQueue.add(consolidatedBatchedPatches);

                                break;
                            }
                            // Otherwise try again after new changes are added.
                            else {
                                logger.debug(String.format("PatchManager-ResponseHandler: Document changed between notification arrival and attempt to append. Retrying changes: %s", Arrays.asList(missingPatches)).replace("\n", "\\n"));
                                continue;
                            }
                        }

                        // Save missing patches & maxVersionSeen
                        batchingCtrl.lastMissingPatches = missingPatches;
                        batchingCtrl.currDocumentVersion = ((FileChangeResponse) response.getData()).fileVersion;

                        // TODO: Does this actually do the right thing anymore?
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

                        // TODO: Remove this once debugging is done
                        // At this point, all patches in the batching queues should be the same version.
                        if(!batchingCtrl.patchBatchingQueue.isEmpty()){
                            long patchVersion = batchingCtrl.patchBatchingQueue.get(0).getBaseVersion();
                            for(int i = 1; i < batchingCtrl.patchBatchingQueue.size(); i++){
                                if(patchVersion != batchingCtrl.patchBatchingQueue.get(i).getBaseVersion()){
                                    logger.fatal("Batching queue had patches of different versions: %s",
                                            Arrays.asList(batchingCtrl.patchBatchingQueue));
                                }
                            }
                        }

                        expectedModificationStamp = batchingCtrl.expectedModificationStamp.get();

                        // If file is of a greater version than this notification, continue to next notification
                        if(batchingCtrl.currDocumentVersion > fileChangeNotif.baseFileVersion){
                            continue;
                        }

                        // Else, patch the document
                        // > Consolidate fileChangeNotif, and batchingQueue
                        Patch consolidatedFileChangeNotification = Consolidator.consolidatePatches(Patch.getPatches(fileChangeNotif.changes));
                        Patch consolidatedBatchedPatches = Consolidator.consolidatePatches(batchingCtrl.patchBatchingQueue);

                        if (consolidatedBatchedPatches != null) {
                            // > Transform fileChangeNotif against batchingQueue
                            //     Others have priority, since they are still unversioned
                            consolidatedFileChangeNotification = consolidatedFileChangeNotification.transform(true, consolidatedBatchedPatches);

                            // > Reverse-Transform batching queue against fileChangeNotif
                            consolidatedBatchedPatches = consolidatedBatchedPatches.transform(false, consolidatedFileChangeNotification);
                        }

                        // Update the actual file
                        // Pass the transformed patches to the actual handler that will take care of writing to document or file
                        Long result = notifHandler.handleNotification(
                                new Notification(notification.getResource(), notification.getMethod(), notification.getResourceID(),
                                        new FileChangeNotification(consolidatedFileChangeNotification == null ? new String[]{} : new String[]{consolidatedFileChangeNotification.toString()},
                                                fileChangeNotif.fileVersion, fileChangeNotif.baseFileVersion
                                        )
                                )
                                , expectedModificationStamp);

                        // > If our optimistic write was successful
                        if (result != null) {
                            batchingCtrl.expectedModificationStamp.set(result);

                            // >> Write reverse-transformed batching queue back.
                            consolidatedBatchedPatches.setBaseVersion(fileChangeNotif.fileVersion);
                            batchingCtrl.patchBatchingQueue.clear();
                            batchingCtrl.patchBatchingQueue.add(consolidatedBatchedPatches);

                            batchingCtrl.currDocumentVersion = fileChangeNotif.fileVersion;

                            break;
                        }
                        // > Otherwise try again after new changes are added.
                        else {
                            logger.debug(String.format("PatchManager-NotificationHandler: Document changed between notification arrival and attempt to append. Retrying changes: %s", Arrays.asList(fileChangeNotif.changes)).replace("\n", "\\n"));
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
        Patch[] lastMissingPatches = {};
        long currDocumentVersion = -1;
        //        private boolean activeChangeRequest = false;
        private AtomicLong expectedModificationStamp = new AtomicLong(-1);
    }
}
