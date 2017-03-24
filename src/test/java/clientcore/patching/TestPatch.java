package clientcore.patching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestPatch {
    @Test
    public void TestInit() {

        String patchString = "v1:\n3:-8:deletion,\n2:+6:insert:\n11";

        // Test List<Diff> init
        Diff diff1 = new Diff(false, 3, "deletion");
        Diff diff2 = new Diff(true, 2, "insert");
        List<Diff> diffs = Arrays.asList(diff1, diff2);
        Patch patch = new Patch(1, diffs, 11);
        Assert.assertEquals(patch.toString(), patchString);
        Assert.assertEquals(patch.getDiffs(), diffs);
        Assert.assertEquals(1, patch.getBaseVersion());

        patch = new Patch(patchString);
        Assert.assertEquals(patch.toString(), patchString);
        Assert.assertEquals(patch.getDiffs(), diffs);
        Assert.assertEquals(1, patch.getBaseVersion());
    }

    @Test
    public void testConvertToCRLF() {
        Patch patch = new Patch("v0:\n0:+5:test%0A:\n12");
        Patch newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n0:+6:test%0D%0A:\n7", newPatch.toString());

        patch = new Patch("v0:\n1:+5:test%0A:\n12");
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+6:test%0D%0A:\n7", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A:\n12");
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n3:+6:test%0D%0A:\n7", newPatch.toString());

        patch = new Patch("v0:\n7:+5:test%0A:\n12");
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n9:+6:test%0D%0A:\n10", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A,\n7:+5:test%0A:\n12");
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A:\n10", newPatch.toString());

        patch = new Patch("v0:\n2:+5:test%0A,\n7:+5:test%0A,\n0:+5:test%0A:\n12");
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(3, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A,\n0:+6:test%0D%0A:\n10", newPatch.toString());
    }

    @Test
    public void testConvertToLF() {
        Patch patch = new Patch("v0:\n0:+6:test%0D%0A:\n6");
        Patch newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n0:+5:test%0A:\n5", newPatch.toString());

        patch = new Patch("v0:\n2:+6:test%0D%0A:\n6");
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n1:+5:test%0A:\n5", newPatch.toString());

        patch = new Patch("v0:\n3:+6:test%0D%0A:\n6");
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+5:test%0A:\n5", newPatch.toString());

        patch = new Patch("v0:\n9:+6:test%0D%0A:\n8");
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(1, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n7:+5:test%0A:\n6", newPatch.toString());

        patch = new Patch("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A:\n8");
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(2, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+5:test%0A,\n7:+5:test%0A:\n6", newPatch.toString());

        patch = new Patch("v0:\n3:+6:test%0D%0A,\n9:+6:test%0D%0A,\n0:+6:test%0D%0A:\n8");
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(3, newPatch.getDiffs().size());
        Assert.assertEquals("v0:\n2:+5:test%0A,\n7:+5:test%0A,\n0:+5:test%0A:\n6", newPatch.toString());
    }

    @Test
    public void TestTransform() {
        String patch1String, patch2String, patch3String, expectedString;
        Patch patch1, patch2, patch3;

        patch1String = "v1:\n0:-1:a:\n10";
        patch2String = "v0:\n3:-8:deletion,\n3:+6:insert:\n10";
        patch1 = new Patch(patch1String);
        patch2 = new Patch(patch2String);
        expectedString = "v2:\n2:-8:deletion,\n2:+6:insert:\n9";
        Patch result = patch2.transform(true, patch1);
        Assert.assertEquals(expectedString, result.toString());

        patch1String = "v1:\n0:-1:a:\n10";
        patch2String = "v2:\n0:-1:b:\n10";
        patch3String = "v0:\n3:-8:deletion,\n3:+6:insert:\n10";
        patch1 = new Patch(patch1String);
        patch2 = new Patch(patch2String);
        patch3 = new Patch(patch3String);
        expectedString = "v3:\n1:-8:deletion,\n1:+6:insert:\n8";
        result = patch3.transform(true, patch1, patch2);
        Assert.assertEquals(expectedString, result.toString());
    }

    @Test
    public void testHashCode() {
        Diff diff1, diff2;
        Patch patch1, patch2;

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertEquals(patch1.hashCode(), patch2.hashCode());
    }

    @Test
    public void testEquals() {
        Diff diff1, diff2;
        Patch patch1, patch2;

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 6);
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 3, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 3, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(false, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(false, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "quick");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 4, "slow");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(true, 4, "slow");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 4, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertNotEquals(patch1, patch2);

        diff1 = new Diff(false, 4, "slow");
        patch1 = new Patch(0, Collections.singletonList(diff1), 5);
        diff2 = new Diff(true, 3, "quick");
        patch2 = new Patch(0, Collections.singletonList(diff2), 5);
        Assert.assertNotEquals(patch1, patch2);
    }

    private static class PatchSimplifyTest {
        String desc;
        String patchStr;
        String expected;

        PatchSimplifyTest(String desc, String patchStr, String expected) {
            this.desc = desc;
            this.patchStr = patchStr;
            this.expected = expected;
        }
    }

    @Test
    public void TestPatch_Simplify() {
        PatchSimplifyTest[] tests = {
                new PatchSimplifyTest("Double-Insert, Adjacent",
                        "v1:\n0:+1:a,\n1:+1:b:\n10",
                        "v1:\n0:+1:a,\n1:+1:b:\n10"
                ),
                new PatchSimplifyTest("Double-Remove, Adjacent",
                        "v1:\n0:-1:a,\n1:-1:b:\n10",
                        "v1:\n0:-2:ab:\n10"
                ),
                new PatchSimplifyTest("Insert-Remove, Adjacent",
                        "v1:\n0:+1:a,\n1:-1:b:\n10",
                        "v1:\n0:+1:a,\n1:-1:b:\n10"
                ),
                new PatchSimplifyTest("Remove-Insert, Adjacent",
                        "v1:\n0:-1:a,\n1:+1:b:\n10",
                        "v1:\n0:-1:a,\n1:+1:b:\n10"
                ),
                new PatchSimplifyTest("Double-Insert, Not adjacent",
                        "v1:\n0:+1:a,\n2:+1:b:\n10",
                        "v1:\n0:+1:a,\n2:+1:b:\n10"
                ),
                new PatchSimplifyTest("Double-Remove, Not adjacent",
                        "v1:\n0:-1:a,\n2:-1:b:\n10",
                        "v1:\n0:-1:a,\n2:-1:b:\n10"
                ),
                new PatchSimplifyTest("Insert-Remove, Not adjacent",
                        "v1:\n0:+1:a,\n2:-1:b:\n10",
                        "v1:\n0:+1:a,\n2:-1:b:\n10"
                ),
                new PatchSimplifyTest("Remove-Insert, Not adjacent",
                        "v1:\n0:-1:a,\n2:+1:b:\n10",
                        "v1:\n0:-1:a,\n2:+1:b:\n10"
                ),
                new PatchSimplifyTest("Triple-Insert, Adjacent",
                        "v1:\n0:+1:a,\n1:+1:b,\n2:+1:c:\n10",
                        "v1:\n0:+1:a,\n1:+1:b,\n2:+1:c:\n10"
                ),
                new PatchSimplifyTest("Triple-Remove, Adjacent",
                        "v1:\n0:-1:a,\n1:-1:b,\n2:-1:c:\n10",
                        "v1:\n0:-3:abc:\n10"
                ),
                new PatchSimplifyTest("Double-Insert, Single Remove, Adjacent",
                        "v1:\n0:+1:a,\n1:+1:b,\n2:-1:c:\n10",
                        "v1:\n0:+1:a,\n1:+1:b,\n2:-1:c:\n10"
                ),
                new PatchSimplifyTest("Single-Remove, Double-Insert, Adjacent",
                        "v1:\n0:-1:a,\n1:+1:b,\n2:+1:c:\n10",
                        "v1:\n0:-1:a,\n1:+1:b,\n2:+1:c:\n10"
                ),
                new PatchSimplifyTest("Double-Remove, Single Insert, Adjacent",
                        "v1:\n0:-1:a,\n1:-1:b,\n2:+1:c:\n10",
                        "v1:\n0:-2:ab,\n2:+1:c:\n10"
                ),
                new PatchSimplifyTest("Single-Insert, Double-Remove, Adjacent",
                        "v1:\n0:+1:a,\n1:-1:b,\n2:-1:c:\n10",
                        "v1:\n0:+1:a,\n1:-2:bc:\n10"
                ),
                new PatchSimplifyTest("Double-Insert, Single Remove, Not adjacent",
                        "v1:\n0:+1:a,\n2:+1:b,\n3:-1:c:\n10",
                        "v1:\n0:+1:a,\n2:+1:b,\n3:-1:c:\n10"
                ),
                new PatchSimplifyTest("Single-Remove, Double-Insert, Not adjacent",
                        "v1:\n0:-1:a,\n1:+1:b,\n3:+1:c:\n10",
                        "v1:\n0:-1:a,\n1:+1:b,\n3:+1:c:\n10"
                ),
                new PatchSimplifyTest("Double-Remove, Single Insert, Not adjacent",
                        "v1:\n0:-1:a,\n2:-1:b,\n3:+1:c:\n10",
                        "v1:\n0:-1:a,\n2:-1:b,\n3:+1:c:\n10"
                ),
                new PatchSimplifyTest("Single-Insert, Double-Remove, Not adjacent",
                        "v1:\n0:+1:a,\n1:-1:b,\n3:-1:c:\n10",
                        "v1:\n0:+1:a,\n1:-1:b,\n3:-1:c:\n10"
                ),
                new PatchSimplifyTest("Interleaved Insert-Delete-Insert, Adjacent",
                        "v1:\n0:+1:a,\n1:-1:b,\n2:+1:c:\n10",
                        "v1:\n0:+1:a,\n1:-1:b,\n2:+1:c:\n10"
                ),
                new PatchSimplifyTest("Interleaved Delete-Insert-Delete, Adjacent",
                        "v1:\n0:-1:a,\n1:+1:b,\n2:-1:c:\n10",
                        "v1:\n0:-1:a,\n1:+1:b,\n2:-1:c:\n10"
                )
        };

        for (PatchSimplifyTest test : tests) {
            Patch patch = new Patch(test.patchStr);

            Assert.assertEquals(
                    String.format("TestPatchSimplify[%s]: Patch was not simplified on creation. Expected %s, but got %s.",
                            test.desc, test.expected, patch.toString()),
                    test.expected, patch.toString());

        }
    }

    @Test
    public void testExtraCases(){
        Patch patch1 = new Patch("v557:\n38:+1:m,\n38:-309:%0A%09Hello%2C+my+name+is+Ben.+This+is+a+test+of+whether+this+works+properly.%0A%09%0A%09Wow+eclipse+is+dumb.+It+changed+my+%22Properly%22+word+to+a+entire+public+main+method%0A%09testing%0A%09System.out.println%28%22Hellow+this+is+a+test%22%29%3B%0A%09%0A%09if+%28true+%3D%3D+false%29+%7B%0A%09%09do+all+the+things%0A%09%092+%3D+1%0A%09%09This+is+definitely+coherent.++DEFINITELY%0A%09%7D:\n349");
        Patch patch2 = new Patch("v557:\n346:-1:%7D:\n349");

        System.out.println(patch2.toString().replace("\n", "\\n"));
        System.out.println(patch2.transform(true, patch1).toString().replace("\n", "\\n"));
    }
}
