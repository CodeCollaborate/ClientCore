package patching;

import org.junit.Test;

/**
 * Created by Benedict on 5/9/2016.
 */
public class TestPatchManager {

    @Test
    public void testApplyPatch() {

        // The quick brown fox jumped over the lazy dog
        // The brown fox jumped over the lazy dog.
        // The quick brown fox jumped the lazy dog
        String patchString1 = "v1:\n4:-6:quick+,\n38:+1:.";
        String patchString2 = "v1:\n27:-5:over+";
    }
}
