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
        Assert.assertEquals(1, patch.getBaseVersion());

        patch = new Patch(patchString);
        Assert.assertEquals(patch.toString(), patchString);
        Assert.assertEquals(patch.getDiffs(), diffs);
        Assert.assertEquals(1, patch.getBaseVersion());
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

    @Test
    public void testConvertToCRLF() {
        Diff diff1 = new Diff("0:+4:test");
        Patch patch = new Patch(0, Arrays.asList(diff1));
        Patch newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(0, newPatch.getDiffs().get(0).getStartIndex());

        diff1 = new Diff("1:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(1, newPatch.getDiffs().get(0).getStartIndex());

        diff1 = new Diff("2:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(3, newPatch.getDiffs().get(0).getStartIndex());

        diff1 = new Diff("7:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(9, newPatch.getDiffs().get(0).getStartIndex());

        diff1 = new Diff("2:+4:test");
        Diff diff2 = new Diff("7:+4:test");
        patch = new Patch(0, Arrays.asList(diff1, diff2));
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals(3, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(9, newPatch.getDiffs().get(1).getStartIndex());

        diff1 = new Diff("2:+4:test");
        diff2 = new Diff("7:+4:test");
        Diff diff3 = new Diff("0:+4:test");
        patch = new Patch(0, Arrays.asList(diff1, diff2, diff3));
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(3, newPatch.getDiffs().size());
        Assert.assertEquals(3, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(9, newPatch.getDiffs().get(1).getStartIndex());
        Assert.assertEquals(0, newPatch.getDiffs().get(2).getStartIndex());
    }

    @Test
    public void testConvertToLF() {
        Diff diff1 = new Diff("0:+4:test");
        Patch patch = new Patch(0, Arrays.asList(diff1));
        Patch newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(0, newPatch.getDiffs().get(0).getStartIndex());

        diff1 = new Diff("1:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(1, newPatch.getDiffs().get(0).getStartIndex());

        diff1 = new Diff("2:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(1, newPatch.getDiffs().get(0).getStartIndex());

        diff1 = new Diff("7:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(5, newPatch.getDiffs().get(0).getStartIndex());
    }

    @Test
    public void testUndo() {
        Diff diff1 = new Diff("0:+4:test");
        Patch patch = new Patch(0, Arrays.asList(diff1));
        Patch newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(0, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(false, newPatch.getDiffs().get(0).isInsertion());

        diff1 = new Diff("1:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(1, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(false, newPatch.getDiffs().get(0).isInsertion());

        diff1 = new Diff("2:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(2, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(false, newPatch.getDiffs().get(0).isInsertion());

        diff1 = new Diff("7:+4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(7, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(false, newPatch.getDiffs().get(0).isInsertion());

        diff1 = new Diff("0:-4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(0, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(true, newPatch.getDiffs().get(0).isInsertion());

        diff1 = new Diff("1:-4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(1, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(true, newPatch.getDiffs().get(0).isInsertion());

        diff1 = new Diff("2:-4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(2, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(true, newPatch.getDiffs().get(0).isInsertion());

        diff1 = new Diff("7:-4:test");
        patch = new Patch(0, Arrays.asList(diff1));
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals(7, newPatch.getDiffs().get(0).getStartIndex());
        Assert.assertEquals(true, newPatch.getDiffs().get(0).isInsertion());
    }


    @Test
    public void testTransformGeneral() {
        Diff diff1, diff2;
        Patch result;

        // The brown fox
        // The quick brown fox
        // The slow brown fox
        diff1 = new Diff(true, 4, "quick");
        Patch patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 4, "slow");
        Patch patch2 = new Patch(0, Arrays.asList(diff2));
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.getDiffs().size());
        Assert.assertEquals(9, result.getDiffs().get(0).getStartIndex());
    }

    @Test
    public void testHashCode() {
        Diff diff1, diff2;
        Patch patch1, patch2;

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertEquals(patch1.hashCode(), patch2.hashCode());
    }

    @Test
    public void testEquals() {
        Diff diff1, diff2;
        Patch patch1, patch2;

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 3, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 3, "quick");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(false, 4, "quick");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(false, 4, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 4, "slow");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "slow");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(false, 4, "slow");
        patch1 = new Patch(0, Arrays.asList(diff1));
        diff2 = new Diff(true, 3, "quick");
        patch2 = new Patch(0, Arrays.asList(diff2));
        Assert.assertNotEquals(patch1, patch2);
    }
}
