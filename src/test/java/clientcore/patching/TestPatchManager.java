package clientcore.patching;

import clientcore.websocket.WSManager;
import clientcore.websocket.models.Notification;
import clientcore.websocket.models.Request;
import clientcore.websocket.models.Response;
import clientcore.websocket.models.notifications.FileChangeNotification;
import clientcore.websocket.models.requests.FileChangeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static clientcore.patching.PatchManager.PATCH_TIMEOUT_MILLIS;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by Benedict on 5/9/2016.
 */
public class TestPatchManager {
    ObjectMapper mapper = new ObjectMapper();

    private Patch[] getPatches(String[] patchStrs) {
        Patch[] result = new Patch[patchStrs.length];

        for (int i = 0; i < patchStrs.length; i++) {
            result[i] = new Patch(patchStrs[i]);
        }

        return result;
    }

    @Test
    public void testApplyPatch() {

        // The quick brown fox jumped over the lazy dog
        // The brown fox jumped over the lazy dog.
        // The quick brown fox jumped the lazy dog
        String baseString = "The quick brown fox jumped over the lazy dog";
        String expectedString1 = "The brown fox jumped over the lazy dog.";
        String expectedString2 = "The quick brown fox jumped the lazy dog";
        String expectedString3 = "The brown fox jumped the lazy dog.";

        Patch patch1 = new Patch("v1:\n4:-6:quick+,\n44:+1:.:\n44");
        Patch patch2 = new Patch("v1:\n27:-5:over+:\n44");
        Patch patch3 = patch2.transform(true, patch1);

        PatchManager mgr = new PatchManager();

        Assert.assertEquals(expectedString1, mgr.applyPatch(baseString, Collections.singletonList(patch1)));
        Assert.assertEquals(expectedString2, mgr.applyPatch(baseString, Collections.singletonList(patch2)));
        Assert.assertEquals(expectedString3, mgr.applyPatch(baseString, Arrays.asList(patch1, patch3)));
    }


    @Test
    public void testApplyPatchInvalidLocation() {

        // The quick brown fox jumped over the lazy dog
        // The brown fox jumped over the lazy dog.
        // The quick brown fox jumped the lazy dog
        String baseString = "The quick brown fox jumped over the lazy dog";

        Patch patch1 = new Patch("v1:\n99:-6:quick+:\n44");

        PatchManager mgr = new PatchManager();

        try {
            mgr.applyPatch(baseString, Arrays.asList(patch1));
            Assert.fail("Should have failed; invalid location");
        } catch (Exception e) {
            // Succeed
        }
    }

    @Test
    public void testNotificationHandler() throws IOException, ClassNotFoundException, InterruptedException {
        WSManager fakeWSMgr = mock(WSManager.class);
        PatchManager patchMgr = new PatchManager();
        PatchManager.notifyOnSend = true;
        PatchManager.PATCH_TIMEOUT_MILLIS = 1000;
        patchMgr.setWsMgr(fakeWSMgr);
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        final Semaphore sem = new Semaphore(0);


        String[] patches = new String[]{
                "v0:\n0:+5:test0:\n10",
                "v0:\n1:+5:test1:\n10",
                "v1:\n2:+5:test2:\n15",
                "v2:\n3:+5:test3:\n20",
        };

        String patchStrFormat = "{\"Resource\": \"File\", \"Method\": \"Change\", \"ResourceID\": 1, \"Data\": {\"FileVersion\": %d, \"Changes\": %s}}";

        // Add first patch to batching queue
        patchMgr.sendPatch(1, getPatches(Arrays.copyOfRange(patches, 0, 1)), null, null);

        // Wait for patchMgr to have sent the request
        synchronized (patchMgr) {
            patchMgr.wait();
        }

        // Expect that patch 1 will be transformed against 0, since 0 is in batching queue
        patchMgr.setNotifHandler((notification, expectedModificationStamp) -> {
            Patch transformedPatch = new Patch(patches[1]);
            transformedPatch = transformedPatch.transform(true, new Patch(patches[0]));

            Assert.assertEquals(1, ((FileChangeNotification) notification.getData()).changes.length);
            Assert.assertEquals(transformedPatch.toString(), ((FileChangeNotification) notification.getData()).changes[0]);

            sem.release();

            return 1L;
        });

        // Generate and send notification
        Notification notif = mapper.readValue(
                String.format(patchStrFormat, 1, mapper.writeValueAsString(Arrays.copyOfRange(patches, 1, 2))),
                Notification.class);
        notif.parseData();
        patchMgr.handleNotification(notif);

        // Wait for timeout of patch send, and start of notificaton handling
        if (!sem.tryAcquire(2, TimeUnit.SECONDS)) {
            Assert.fail("Failed to acquire sem");
        }

        // Expect that patch 2 will be transformed against 0, since 0 is still in batching queue
        patchMgr.setNotifHandler((notification, expectedModificationStamp) -> {
            Patch transformedPatch = new Patch(patches[2]);
            Patch queuedPatch = new Patch(patches[0]).transform(false, new Patch(patches[1]));
            transformedPatch = transformedPatch.transform(true, queuedPatch);

            Assert.assertEquals(1, ((FileChangeNotification) notification.getData()).changes.length);
            Assert.assertEquals(transformedPatch.toString(), ((FileChangeNotification) notification.getData()).changes[0]);

            sem.release();

            return 1L;
        });

        // Generate and send notification
        notif = mapper.readValue(
                String.format(patchStrFormat, 2, mapper.writeValueAsString(Arrays.copyOfRange(patches, 2, 3))),
                Notification.class);
        notif.parseData();
        patchMgr.handleNotification(notif);

        // Wait for timeout of patch send, and start of notificaton handling
        if (!sem.tryAcquire(2, TimeUnit.SECONDS)) {
            Assert.fail("Failed to acquire sem");
        }
    }

    @Test
    public void testResponseHandler() throws InterruptedException, IOException, ClassNotFoundException {
        WSManager fakeWSMgr = mock(WSManager.class);
        PatchManager patchMgr = new PatchManager();
        PatchManager.notifyOnSend = true;
        PatchManager.PATCH_TIMEOUT_MILLIS = 1000;
        patchMgr.setWsMgr(fakeWSMgr);
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

        String[] patches = new String[]{
                "v0:\n0:+5:test0:\n10",
                "v0:\n1:+5:test1:\n15",
                "v0:\n2:+5:test2:\n20"
        };

        Request[] req = new Request[1];
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[0])}, null, null);

        // Wait for patchMgr to have sent the request
        synchronized (patchMgr) {
            patchMgr.wait();
        }

        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v0:\\n0:+5:test0:\\n10\"]")));
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[1])}, null, null);
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[2])}, null, null);

        Response resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
                0, 200, 1, "[]", "[\"v0:\\n0:+5:test0:\\n10\"]"),
                Response.class
        );
        resp.parseData(FileChangeRequest.class);
        req[0].getResponseHandler().handleResponse(resp);

        // Wait for patchMgr to have sent the request
        synchronized (patchMgr) {
            patchMgr.wait();
        }

        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v1:\\n1:+10:ttest2est1:\\n15\"]")));
    }

    @Test
    public void testSendBatchedRequest() throws IOException, ClassNotFoundException, InterruptedException {
        WSManager fakeWSMgr = mock(WSManager.class);
        PatchManager patchMgr = new PatchManager();
        PatchManager.notifyOnSend = true;
        PatchManager.PATCH_TIMEOUT_MILLIS = 5000;
        patchMgr.setWsMgr(fakeWSMgr);
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

        String[] patches = new String[]{
                "v0:\n0:+5:test0:\n10",
                "v1:\n1:+5:test1:\n15",
                "v1:\n2:+5:test2:\n20",
                "v2:\n3:+5:test3:\n25",
                "v2:\n4:+5:test4:\n30",
                "v2:\n5:+6:test15:\n35",
        };

        Request[] req = new Request[1];
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[0])}, null, null);

        // Wait for patchMgr to have sent the request
        synchronized (patchMgr) {
            patchMgr.wait();
        }

        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v0:\\n0:+5:test0:\\n10\"]")));

        Response resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
                0, 200, 1, "[]", "[\"v0:\\n0:+5:test0:\\n10\"]"),
                Response.class
        );
        resp.parseData(FileChangeRequest.class);
        req[0].getResponseHandler().handleResponse(resp);

        // Send 2 patches in same request
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[1]), new Patch(patches[2])}, null, null);

        // Wait for patchMgr to have sent the request
        synchronized (patchMgr) {
            patchMgr.wait();
        }

        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v1:\\n1:+10:ttest2est1:\\n15\"]")));


        // Enqueue 2 patches separately, make sure that they batch; delay previous response until after this one has been submitted.
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[3])}, null, null);
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[4])}, null, null);

        resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
                0, 200, 2, "[]", "[\"v1:\\n1:+10:ttest2est1:\\n15\"]"),
                Response.class
        );
        resp.parseData(FileChangeRequest.class);

        // Wait for patchMgr to have sent the request
        synchronized (patchMgr) {
            req[0].getResponseHandler().handleResponse(resp);
            patchMgr.wait();
        }

        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v2:\\n3:+10:ttest4est3:\\n25\"]")));

        // Test the auto-release after timeout
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[5])}, null, null);

        try {
            Thread.sleep((long)(PATCH_TIMEOUT_MILLIS * 1.5));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v2:\\n3:+16:tttest15est4est3:\\n25\"]")));
    }

    private ArgumentMatcher<Request> createArgChecker(Request[] req, String str) {
        return new ArgumentMatcher<Request>() {
            @Override
            public boolean matches(Request argument) {
                req[0] = argument;
                try {
                    return mapper.writeValueAsString(argument).contains(str);
                } catch (JsonProcessingException e) {
                    Assert.fail("Failed to parse request");
                    return false;
                }
            }
        };
    }
}
