package patching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDiff {

    @Test
    public void testInit() {
        // Test addition
        Diff diff = new Diff(true, 1, "test");
        Assert.assertEquals(true, diff.isInsertion());
        Assert.assertEquals(1, diff.getStartIndex());
        Assert.assertEquals("test", diff.getChanges());

        // Test duplicate diff
        Diff dupDiff = new Diff(diff);
        Assert.assertEquals(diff, dupDiff);

        // Test removal
        diff = new Diff(false, 10, "string");
        Assert.assertEquals(false, diff.isInsertion());
        Assert.assertEquals(10, diff.getStartIndex());
        Assert.assertEquals("string", diff.getChanges());

        dupDiff = new Diff(diff);
        Assert.assertEquals(diff, dupDiff);

        // Test insertion from string
        diff = new Diff("10:+4:test");
        Assert.assertEquals(true, diff.isInsertion());
        Assert.assertEquals(10, diff.getStartIndex());
        Assert.assertEquals("test", diff.getChanges());

        dupDiff = new Diff(diff);
        Assert.assertEquals(diff, dupDiff);

        // Test removal from string
        diff = new Diff("3:-3:del");
        Assert.assertEquals(false, diff.isInsertion());
        Assert.assertEquals(3, diff.getStartIndex());
        Assert.assertEquals("del", diff.getChanges());

        dupDiff = new Diff(diff);
        Assert.assertEquals(diff, dupDiff);

        // Test invalid string format
        try {
            diff = new Diff("delete 2");
            Assert.fail("Invalid diff format; should have thrown an exception");
        } catch (IllegalArgumentException e) {
            // do nothing, this is expected.
        }

        // Test wrong length
        try {
            diff = new Diff("3:-1:del");
            Assert.fail("Length and change string length do not match, should have thrown exception");
        } catch (IllegalArgumentException e) {
            // do nothing, this is expected.
        }

        // Test wrong length2
        try {
            diff = new Diff("9:+1:test");
            Assert.fail("Length and change string length do not match, should have thrown exception");
        } catch (IllegalArgumentException e) {
            // do nothing, this is expected.
        }
    }

    @Test
    public void testConvertToCRLF() {
        Diff patch = new Diff("0:+4:test");
        Diff newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(0, newPatch.getStartIndex());

        patch = new Diff("1:+4:test");
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getStartIndex());

        patch = new Diff("2:+4:test");
        newPatch = patch.convertToCRLF("\r\ntest");
        Assert.assertEquals(3, newPatch.getStartIndex());

        patch = new Diff("7:+4:test");
        newPatch = patch.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(9, newPatch.getStartIndex());

        patch = new Diff("7:+4:test");
        newPatch = patch.convertToCRLF("\r\ntes\r\n");
        Assert.assertEquals(9, newPatch.getStartIndex());
    }

    @Test
    public void testConvertToLF() {
        Diff patch = new Diff("0:+4:test");
        Diff newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(0, newPatch.getStartIndex());

        patch = new Diff("1:+4:test");
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getStartIndex());

        patch = new Diff("2:+4:test");
        newPatch = patch.convertToLF("\r\ntest");
        Assert.assertEquals(1, newPatch.getStartIndex());

        patch = new Diff("7:+4:test");
        newPatch = patch.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(5, newPatch.getStartIndex());

        patch = new Diff("7:+4:test");
        newPatch = patch.convertToLF("\r\ntes\r\n");
        Assert.assertEquals(5, newPatch.getStartIndex());
    }

    @Test
    public void testTransformGeneral() {
        Diff patch1, patch2, patch3;
        List<Diff> result;

        // The brown fox
        // The quick brown fox
        // The slow brown fox
        patch1 = new Diff(true, 4, "quick");
        patch2 = new Diff(true, 4, "slow");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(9, result.get(0).getStartIndex());

        // ||||||
        // |||||quick|
        // ||||slow||
        // ||||slow|quick|
        patch1 = new Diff(true, 5, "quick");
        patch2 = new Diff(true, 4, "slow");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());

        // ||||||
        // |||quick|||
        // ||||slow||
        // |||quick|slow||
        patch1 = new Diff(true, 3, "quick");
        patch2 = new Diff(true, 4, "slow");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(9, result.get(0).getStartIndex());

        // the quick ||||||
        // quick ||||||
        // the ||||||
        // ||||||
        patch1 = new Diff(false, 0, "the ");
        patch2 = new Diff(false, 6, "quick");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals(5, result.get(0).getLength());

        // the quick ||||||
        // the ck ||||||
        // the qu ||||||
        // the ||||||
        patch1 = new Diff(false, 4, "qui");
        patch2 = new Diff(false, 6, "ick");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals(2, result.get(0).getLength());

        // the quick ||||||
        // the k ||||||
        // the qu ||||||
        // the ||||||
        patch1 = new Diff(false, 4, "quic");
        patch2 = new Diff(false, 6, "ick");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals(1, result.get(0).getLength());

        // the quick brown fox ||||||
        // the quick fox ||||||
        // the k brown fox ||||||
        // the k fox ||||||
        patch1 = new Diff(false, 10, "brown");
        patch2 = new Diff(false, 4, "quic");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals(4, result.get(0).getLength());

        // the quick brown fox||||||
        // the brown fox||||||
        // the quick brown ||||||
        // the brown ||||||
        patch1 = new Diff(false, 4, "quick");
        patch2 = new Diff(false, 16, "fox");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(11, result.get(0).getStartIndex());
        Assert.assertEquals(3, result.get(0).getLength());

        // abcdefgh
        // abpklfgh // Remove "cde", then add "pkl"
        // abcdgh // Remove "ef"
        // abpklgh // Should have a net removal of "cdef", add "pkl"
        patch1 = new Diff(false, 2, "cde");
        patch2 = new Diff(true, 2, "pkl");
        patch3 = new Diff(false, 4, "ef");
        result = patch3.transform(Arrays.asList(patch1, patch2));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(5, result.get(0).getStartIndex());
        Assert.assertEquals(1, result.get(0).getLength());
    }


    @Test
    public void testTransform1A() {
        Diff patch1, patch2;
        List<Diff> result;

        patch1 = new Diff(true, 2, "str1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(true, 0, "str1");
        patch2 = new Diff(true, 1, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(5, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }


    @Test
    public void testTransform1B() {
        Diff patch1, patch2;
        List<Diff> result;

        patch1 = new Diff(true, 2, "str1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(true, 0, "str1");
        patch2 = new Diff(false, 1, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(5, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }


    @Test
    public void testTransform1C() {
        Diff patch1, patch2;
        List<Diff> result;

        // Test case i
        patch1 = new Diff(false, 2, "str1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(false, 2, "longerstr1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        // Test else case
        patch1 = new Diff(false, 2, "str1");
        patch2 = new Diff(true, 6, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(false, 2, "str1");
        patch2 = new Diff(true, 8, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }

    @Test
    public void testTransform1D() {
        Diff patch1, patch2;
        List<Diff> result;

        // Test case 1: if IndexA + LenA < IndexB (No overlap), shift B down by LenA
        patch1 = new Diff(false, 2, "str1");
        patch2 = new Diff(false, 8, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(false, 2, "str1");
        patch2 = new Diff(false, 6, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        // Test case 2: if IndexA + LenA >= IndexB + LenB, ignore B
        patch1 = new Diff(false, 2, "longerstr1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(0, result.size());

        // Test else cases: if overlapping, shorten B by overlap, shift down by LenA - overlap
        patch1 = new Diff(false, 2, "str1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("r2", result.get(0).getChanges());
    }

    @Test
    public void testTransform2A() {
        Diff patch1, patch2;
        List<Diff> result;

        patch1 = new Diff(true, 4, "str1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(true, 0, "longTestString1");
        patch2 = new Diff(true, 0, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(15, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

    }

    @Test
    public void testTransform2B() {
        Diff patch1, patch2;
        List<Diff> result;

        patch1 = new Diff(true, 4, "str1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(true, 0, "longTestString1");
        patch2 = new Diff(false, 0, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(15, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

    }

    @Test
    public void testTransform2C() {
        Diff patch1, patch2;
        List<Diff> result;

        patch1 = new Diff(false, 4, "str1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(false, 0, "longTestString1");
        patch2 = new Diff(true, 0, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(0, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

    }

    @Test
    public void testTransform2D() {
        Diff patch1, patch2;
        List<Diff> result;

        // Test case 1: If LenB > LenA, remove LenA characters from B
        patch1 = new Diff(false, 4, "str1");
        patch2 = new Diff(false, 4, "longTestString2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("TestString2", result.get(0).getChanges());

        // Test else case - if LenB <= LenA
        patch1 = new Diff(false, 0, "longTestString1");
        patch2 = new Diff(false, 0, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(0, result.size());

        patch1 = new Diff(false, 4, "str1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(0, result.size());

    }

    @Test
    public void testTransform3A() {
        Diff patch1, patch2;
        List<Diff> result;

        patch1 = new Diff(true, 5, "str1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(true, 4, "str1");
        patch2 = new Diff(true, 0, "longTestString2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(0, result.get(0).getStartIndex());
        Assert.assertEquals("longTestString2", result.get(0).getChanges());
    }

    @Test
    public void testTransform3B() {
        Diff patch1, patch2;
        List<Diff> result;

        // Test case 1: If IndexB + LenB > IndexA, split B into two diffs
        patch1 = new Diff(true, 5, "str1");
        patch2 = new Diff(false, 4, "longStr2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("l", result.get(0).getChanges());
        Assert.assertEquals(false, result.get(1).isInsertion());
        Assert.assertEquals(8, result.get(1).getStartIndex());
        Assert.assertEquals("ongStr2", result.get(1).getChanges());

        // Test else case: no change
        patch1 = new Diff(true, 8, "str1");
        patch2 = new Diff(false, 0, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(0, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }

    @Test
    public void testTransform3C() {
        Diff patch1, patch2;
        List<Diff> result;

        patch1 = new Diff(false, 9, "str1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(false, 5, "str1");
        patch2 = new Diff(true, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }

    @Test
    public void testTransform3D() {
        Diff patch1, patch2;
        List<Diff> result;

        // Test case 1: If IndexB + LenB > IndexA, shorten B by overlap (from end)
        patch1 = new Diff(false, 6, "str1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("st", result.get(0).getChanges());

        // Test else case: No change if no overlap
        patch1 = new Diff(false, 8, "str1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        patch1 = new Diff(false, 10, "str1");
        patch2 = new Diff(false, 4, "str2");
        result = patch2.transform(Arrays.asList(patch1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }

    @Test
    public void testUndo() {
        Diff diff = new Diff(false, 2, "cde");
        Assert.assertNotEquals(diff.isInsertion(), diff.getUndo().isInsertion());

        diff = new Diff(true, 2, "cde");
        Assert.assertNotEquals(diff.isInsertion(), diff.getUndo().isInsertion());
    }
}
