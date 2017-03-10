package clientcore.patching;

import clientcore.websocket.models.responses.FileChangeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import clientcore.websocket.WSManager;
import clientcore.websocket.models.Notification;
import clientcore.websocket.models.Request;
import clientcore.websocket.models.Response;
import clientcore.websocket.models.notifications.FileChangeNotification;
import clientcore.websocket.models.requests.FileChangeRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static clientcore.patching.PatchManager.PATCH_TIMEOUT_MILLIS;

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
        patchMgr.setWsMgr(fakeWSMgr);
        ArgumentCaptor<Request> argumentCaptor = ArgumentCaptor.forClass(Request.class);
        final Semaphore sem = new Semaphore(0);


        String[] patches = new String[]{
                "v0:\n0:+5:test0:\n10",
                "v0:\n1:+5:test1:\n10",
                "v1:\n2:+5:test2:\n15",
                "v2:\n3:+5:test3:\n20",
        };

        String patchStrFormat = "{\"Resource\": \"File\", \"Method\": \"Change\", \"ResourceID\": 1, \"Data\": {\"BaseFileVersion\": %d, \"FileVersion\": %d, \"Changes\": %s}}";

        // Add first patch to batching queue
        patchMgr.sendPatch(1, getPatches(Arrays.copyOfRange(patches, 0, 1)), null, null);

        Thread.sleep(100);

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
                String.format(patchStrFormat, 0, 1, mapper.writeValueAsString(Arrays.copyOfRange(patches, 1, 2))),
                Notification.class);
        notif.parseData();
        patchMgr.handleNotification(notif);

        if (!sem.tryAcquire(10, TimeUnit.SECONDS)) {
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
                String.format(patchStrFormat, 1, 2, mapper.writeValueAsString(Arrays.copyOfRange(patches, 2, 3))),
                Notification.class);
        notif.parseData();
        patchMgr.handleNotification(notif);

        if (!sem.tryAcquire(10, TimeUnit.SECONDS)) {
            Assert.fail("Failed to acquire sem");
        }

        // Send response to first file change request
        verify(fakeWSMgr, atLeastOnce()).sendAuthenticatedRequest(argumentCaptor.capture());
        Request req = argumentCaptor.getValue();
        req.getResponseHandler().handleResponse(new Response(1, 200, new FileChangeResponse(1, Arrays.copyOfRange(patches, 2, 3), new String[]{})));

        // Expect that patch 1 will be transformed against 2, since 2 is in the done queue, but of a higher version.
        patchMgr.setNotifHandler((notification, expectedModificationStamp) -> {
            Patch transformedPatch = new Patch(patches[2]);
            Patch queuedPatch = new Patch(patches[2]);
            transformedPatch = transformedPatch.transform(true, queuedPatch);

            Assert.assertEquals(1, ((FileChangeNotification) notification.getData()).changes.length);
            Assert.assertEquals(transformedPatch.toString(), ((FileChangeNotification) notification.getData()).changes[0]);

            sem.release();

            return 1L;
        });

        // Generate and send notification
        notif = mapper.readValue(
                String.format(patchStrFormat, 0, 1, mapper.writeValueAsString(Arrays.copyOfRange(patches, 2, 3))),
                Notification.class);
        notif.parseData();
        patchMgr.handleNotification(notif);

        if (!sem.tryAcquire(10, TimeUnit.SECONDS)) {
            Assert.fail("Failed to acquire sem");
        }

        // Expect that patch 3 will not be transformed against 2, since 2 is in the done queue, but of a lower version.
        patchMgr.setNotifHandler((notification, expectedModificationStamp) -> {
            Patch transformedPatch = new Patch(patches[3]);

            Assert.assertEquals(1, ((FileChangeNotification) notification.getData()).changes.length);
            Assert.assertEquals(transformedPatch.toString(), ((FileChangeNotification) notification.getData()).changes[0]);

            sem.release();

            return 1L;
        });

        // Generate and send notification
        notif = mapper.readValue(
                String.format(patchStrFormat, 0, 1, mapper.writeValueAsString(Arrays.copyOfRange(patches, 3, 4))),
                Notification.class);
        notif.parseData();
        patchMgr.handleNotification(notif);

        if (!sem.tryAcquire(10, TimeUnit.SECONDS)) {
            Assert.fail("Failed to acquire sem");
        }
    }

//    @Test
//    public void testSynchronizationFinishRequestBeforeHandleNotification() throws IOException, ClassNotFoundException {
//        final String[] testText = {"jhb"};
//
//        WSManager fakeWSMgr = mock(WSManager.class);
//        PatchManager patchMgr = new PatchManager();
//        patchMgr.setWsMgr(fakeWSMgr);
//        patchMgr.setNotifHandler(new INotificationHandler() {
//            @Override
//            public void handleNotification(Notification n) {
//                Patch[] patches = new Patch[((FileChangeNotification) n.getData()).changes.length];
//                for (int i = 0; i < ((FileChangeNotification) n.getData()).changes.length; i++) {
//                    patches[i] = new Patch(((FileChangeNotification) n.getData()).changes[i]);
//                }
//                testText[0] = patchMgr.applyPatch(testText[0], Arrays.asList(patches));
//            }
//        });
//        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);
//
//
//        String[] patches = new String[]{
//                "v0:\n0:+1:j", // Becomes v1
//                "v0:\n1:+1:h", // Becomes v3
//                "v0:\n2:+1:b", // Becomes v3
//        };
//        String[] notifications = new String[]{
//                "[\"v1:\\n1:+1:k\"]",
//                "[\"v3:\\n1:+1:j\", \"v3:\\n2:+1:n\"]",
//        };
//
//        // Send patch1
//        // Receive notification1
//        // Send patches 2, 3
//        // Receive notifications 2, 3
//        // Expect JKHBJN
//
//        Request[] req = new Request[1];
//
//        // Add all patches, expect only first one to be sent immediately
//        patchMgr.sendPatch(1, 0, new Patch[]{new Patch(patches[0])}, null, null);
//        patchMgr.sendPatch(1, 0, new Patch[]{new Patch(patches[1])}, null, null);
//        patchMgr.sendPatch(1, 0, new Patch[]{new Patch(patches[2])}, null, null);
//
//        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v0:\\n0:+1:j\"]")));
//
//        Response resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
//                0, 200, 1, "[]", "[\"v0:\\n0:+1:j\"]"),
//                Response.class
//        );
//        resp.parseData(FileChangeRequest.class);
//        req[0].getResponseHandler().handleResponse(resp);
//        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v0:\\n0:+1:j\"]")));
//
//        Notification notif = mapper.readValue(String.format("{\"Type\":\"Notification\",\"ServerMessage\":{\"Resource\":\"File\",\"Method\":\"Change\",\"ResourceID\":%d,\"Data\":{\"FileVersion\":%s,\"Changes\":%s}}}",
//                1, 2, notifications[0]),
//                Notification.class
//        );
//        notif.parseData();
//        patchMgr.handleNotification(notif);
//
//        Response resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
//                0, 200, 1, "[]", "[\"v0:\\n0:+1:j\"]"),
//                Response.class
//        );
//        resp.parseData(FileChangeRequest.class);
//        req[0].getResponseHandler().handleResponse(resp);
//
//        Notification notif = mapper.readValue(String.format("{\"Type\":\"Notification\",\"ServerMessage\":{\"Resource\":\"File\",\"Method\":\"Change\",\"ResourceID\":%d,\"Data\":{\"FileVersion\":%s,\"Changes\":%s}}}",
//                1, 2, notifications[0]),
//                Notification.class
//        );
//        notif.parseData();
//        patchMgr.handleNotification(notif);
//    }

    @Test
    public void testSendBatchedRequest() throws IOException, ClassNotFoundException, InterruptedException {
        WSManager fakeWSMgr = mock(WSManager.class);
        PatchManager patchMgr = new PatchManager();
        patchMgr.setWsMgr(fakeWSMgr);
        ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

        String[] patches = new String[]{
                "v0:\n0:+5:test0:\n10",
                "v1:\n1:+5:test1:\n15",
                "v1:\n2:+5:test2:\n20",
                "v2:\n3:+5:test3:\n25",
                "v2:\n4:+5:test4:\n30",
                "v2:\n5:+5:test5:\n35",
                "v2:\n10:+6:test10:\n40",
                "v4:\n15:+6:test15:\n46",
                "v4:\n20:+6:test16:\n52",
        };

        Request[] req = new Request[1];
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[0])}, null, null);
        Thread.sleep(100); // Wait for transformAndSendPatch thread to spool up
        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v0:\\n0:+5:test0:\\n10\"]")));

        Response resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
                0, 200, 1, "[]", "[\"v0:\\n0:+5:test0:\\n10\"]"),
                Response.class
        );
        resp.parseData(FileChangeRequest.class);
        req[0].getResponseHandler().handleResponse(resp);

        // Send 2 patches in same request
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[1]), new Patch(patches[2])}, null, null);
        Thread.sleep(100); // Wait for transformAndSendPatch thread to spool up
        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v1:\\n1:+10:ttest2est1:\\n15\"]")));


        // Enqueue 2 patches separately, make sure that they batch; delay previous response until after this one has been submitted.
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[3])}, null, null);
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[4])}, null, null);

        resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
                0, 200, 2, "[]", "[\"v1:\\n1:+10:ttest2est1:\\n15\"]"),
                Response.class
        );
        resp.parseData(FileChangeRequest.class);
        req[0].getResponseHandler().handleResponse(resp);

        Thread.sleep(100); // Wait for transformAndSendPatch thread to spool up
        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v2:\\n3:+5:test3\",\"v2:\\n4:+5:test4\"]")));

        resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
                0, 200, 3, "[]", "[\"v2:\\n3:+5:test3\",\"v2:\\n4:+5:test4\"]"),
                Response.class
        );
        resp.parseData(FileChangeRequest.class);
        req[0].getResponseHandler().handleResponse(resp);

        // Test sending a patch for a version that is "out of date"
        // Use prevTag + 1; since the previous one was generated after the response came back
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[5])}, null, null);

        Thread.sleep(100); // Wait for transformAndSendPatch thread to spool up
        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v3:\\n5:+5:test5\"]")));

        // Test sending two patches; one for a version that is out of date, the other with a version greater than the last response
        resp = mapper.readValue(String.format("{\"Tag\":%d,\"Status\":%d,\"Data\":{\"FileVersion\":%d,\"MissingPatches\":%s,\"Changes\":%s}}",
                0, 200, 4, "[\"v2:\\n0:+5:test0\",\"v2:\\n1:+5:test1\"]", "[\"v3:\\n15:+5:test5\"]"),
                Response.class
        );
        resp.parseData(FileChangeRequest.class);
        req[0].getResponseHandler().handleResponse(resp);

        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[6]), new Patch(patches[7])}, null, null);

        Thread.sleep(100); // Wait for transformAndSendPatch thread to spool up
        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v4:\\n20:+6:test10\",\"v4:\\n15:+6:test15\"]")));

        // Test the auto-release after timeout
        patchMgr.sendPatch(1, new Patch[]{new Patch(patches[8])}, null, null);

        try {
            Thread.sleep(PATCH_TIMEOUT_MILLIS + 500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Thread.sleep(100); // Wait for transformAndSendPatch thread to spool up
        verify(fakeWSMgr).sendAuthenticatedRequest(argThat(createArgChecker(req, "[\"v4:\\n20:+6:test10\",\"v4:\\n15:+6:test15\",\"v4:\\n20:+6:test16\"]")));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ArgumentMatcher<Request> createArgChecker(Request[] req, String str) {
        return new ArgumentMatcher<Request>() {
            @Override
            public boolean matches(Request argument) {
                req[0] = (Request) argument;
                try {
                    System.out.println(mapper.writeValueAsString(argument));
                    return mapper.writeValueAsString(argument).contains(str);
                } catch (JsonProcessingException e) {
                    Assert.fail("Failed to parse request");
                    return false;
                }
            }
        };
    }
}
