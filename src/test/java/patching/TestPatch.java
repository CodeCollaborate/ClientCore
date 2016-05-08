package patching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestPatch {
    @Test
    public void TestInit() {

        String patchString = "v1:\n3:-8:deletion,\n3:+6:insert";

        // Test List<Diff> init
        Diff diff1 = new Diff(false, 3, "deletion");
        Diff diff2 = new Diff(true, 3, "insert");
        List<Diff> diffs = Arrays.asList(diff1, diff2);
        Patch patch = new Patch(1, diffs);
        Assert.assertEquals(patch.toString(), patchString);
        Assert.assertEquals(patch.getDiffs(), diffs);

        patch = new Patch(patchString);
        Assert.assertEquals(patch.toString(), patchString);
        Assert.assertEquals(patch.getDiffs(), diffs);
    }

    @Test
    public void TestTransform() {
        String patch1String, patch2String, patch3String, expectedString;
        Patch patch1, patch2, patch3;

        patch1String = "v1:\n0:-1:a";
        patch2String = "v0:\n3:-8:deletion,\n3:+6:insert";
        patch1 = new Patch(patch1String);
        patch2 = new Patch(patch2String);
        expectedString = "v1:\n2:-8:deletion,\n2:+6:insert";
        Patch result = patch2.transform(patch1);
        Assert.assertEquals(expectedString, result.toString());

        patch1String = "v1:\n0:-1:a";
        patch2String = "v2:\n0:-1:b";
        patch3String = "v0:\n3:-8:deletion,\n3:+6:insert";
        patch1 = new Patch(patch1String);
        patch2 = new Patch(patch2String);
        patch3 = new Patch(patch3String);
        expectedString = "v2:\n1:-8:deletion,\n1:+6:insert";
        result = patch3.transform(patch1, patch2);
        Assert.assertEquals(expectedString, result.toString());

    }
}
