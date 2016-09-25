package patching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestPatch {
    @Test
    public void TestInit() {

        String patchString = "v1:\n3:-8:deletion,\n2:+6:insert";

        // Test List<Diff> init
        Diff diff1 = new Diff(false, 3, "deletion");
        Diff diff2 = new Diff(true, 2, "insert");
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
        Patch patch = new Patch("v0:\n0:+5:test%0A");
        Patch newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n0:+6:test%0D%0A", newPatch.toString());

        patch = new Patch("v0:\n1:+5:test%0A");
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+6:test%0D%0A", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A");
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n3:+6:test%0D%0A", newPatch.toString());

        patch = new Patch("v0:\n7:+5:test%0A");
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n9:+6:test%0D%0A", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A,\n7:+5:test%0A");
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A,\n7:+5:test%0A,\n0:+5:test%0A");
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(3, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A,\n0:+6:test%0D%0A", newPatch.toString());
    }

    @Test
    public void testConvertToLF() {
        Patch patch = new Patch("v0:\n0:+6:test%0D%0A");
        Patch newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n0:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n2:+6:test%0D%0A");
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n1:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n3:+6:test%0D%0A");
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n9:+6:test%0D%0A");
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n7:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A");
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+5:test%0A,\n7:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A,\n0:+6:test%0D%0A");
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(3, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+5:test%0A,\n7:+5:test%0A,\n0:+5:test%0A", newPatch.toString());
    }

    @Test
    public void testUndo() {
        Patch patch = new Patch("v0:\n0:+5:test%0A");
        Patch newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n0:-5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n1:-5:test%0A");
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n1:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A");
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:-5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n7:-5:test%0A");
        newPatch = patch.getUndo();
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n7:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n2:-5:test%0A,\n7:+5:test%0A");
        newPatch = patch.getUndo();
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n7:-5:test%0A,\n2:+5:test%0A", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A,\n7:-5:test%0A,\n0:-5:test%0A");
        newPatch = patch.getUndo();
        Assert.assertEquals(3, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n0:+5:test%0A,\n7:+5:test%0A,\n2:-5:test%0A", newPatch.toString());
    }


    @Test
    public void testTransformGeneral() {
        Patch patch1 = new Patch("v1:\n0:-1:a");
        Patch patch2 = new Patch("v0:\n3:-8:deletion,\n3:+6:insert");
        Patch newPatch = patch2.transform(Collections.singletonList(patch1));
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals("v1:\n2:-8:deletion,\n2:+6:insert", newPatch.toString());

        patch1 = new Patch("v1:\n0:-1:a");
        patch2 = new Patch("v2:\n0:-1:b");
        Patch patch3 = new Patch("v0:\n3:-8:deletion,\n3:+6:insert");
        newPatch = patch3.transform(Arrays.asList(patch1, patch2));
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals("v2:\n1:-8:deletion,\n1:+6:insert", newPatch.toString());
    }

    @Test
    public void testHashCode() {
        Diff diff1, diff2;
        Patch patch1, patch2;

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertEquals(patch1.hashCode(), patch2.hashCode());
    }

    @Test
    public void testEquals() {
        Diff diff1, diff2;
        Patch patch1, patch2;

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 3, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 3, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(false, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(false, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 4, "slow");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "slow");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(false, 4, "slow");
        patch1 = new Patch(0, Collections.singletonList(diff1));
        diff2 = new Diff(true, 3, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2));
        Assert.assertNotEquals(patch1, patch2);
    }
}
