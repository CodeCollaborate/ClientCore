package patching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by Benedict on 5/9/2016.
 */
public class TestPatchManager {

    @Test
    public void testApplyPatch() {

        // The quick brown fox jumped over the lazy dog
        // The brown fox jumped over the lazy dog.
        // The quick brown fox jumped the lazy dog
        String baseString = "The quick brown fox jumped over the lazy dog";
        String expectedString1 = "The brown fox jumped over the lazy dog.";
        String expectedString2 = "The quick brown fox jumped the lazy dog";
        String expectedString3 = "The brown fox jumped the lazy dog.";

        Patch patch1 = new Patch("v1:\n4:-6:quick+,\n38:+1:.");
        Patch patch2 = new Patch("v1:\n27:-5:over+");
        Patch patch3 = patch2.transform(patch1);

        PatchManager mgr = new PatchManager();

        Assert.assertEquals(expectedString1, mgr.applyPatch(baseString, Arrays.asList(patch1)));
        Assert.assertEquals(expectedString2, mgr.applyPatch(baseString, Arrays.asList(patch2)));
        Assert.assertEquals(expectedString3, mgr.applyPatch(baseString, Arrays.asList(patch1, patch3)));
    }


    @Test
    public void testApplyPatchInvalidLocation() {

        // The quick brown fox jumped over the lazy dog
        // The brown fox jumped over the lazy dog.
        // The quick brown fox jumped the lazy dog
        String baseString = "The quick brown fox jumped over the lazy dog";

        Patch patch1 = new Patch("v1:\n99:-6:quick+");

        PatchManager mgr = new PatchManager();

        try {
            mgr.applyPatch(baseString, Arrays.asList(patch1));
            Assert.fail("Should have failed; invalid location");
        } catch (Exception e) {
            // Succeed
        }
    }
}
