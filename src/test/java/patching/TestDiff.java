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
        Diff diff = new Diff("0:+4:test");
        Diff newDiff = diff.convertToCRLF("\r\ntest");
        Assert.assertEquals(0, newDiff.getStartIndex());

        diff = new Diff("1:+4:test");
        newDiff = diff.convertToCRLF("\r\ntest");
        Assert.assertEquals(1, newDiff.getStartIndex());

        diff = new Diff("2:+4:test");
        newDiff = diff.convertToCRLF("\r\ntest");
        Assert.assertEquals(3, newDiff.getStartIndex());

        diff = new Diff("7:+4:test");
        newDiff = diff.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals(9, newDiff.getStartIndex());

        diff = new Diff("7:+4:test");
        newDiff = diff.convertToCRLF("\r\ntes\r\n");
        Assert.assertEquals(9, newDiff.getStartIndex());
    }

    @Test
    public void testConvertToLF() {
        Diff diff = new Diff("0:+4:test");
        Diff newDiff = diff.convertToLF("\r\ntest");
        Assert.assertEquals(0, newDiff.getStartIndex());

        diff = new Diff("1:+4:test");
        newDiff = diff.convertToLF("\r\ntest");
        Assert.assertEquals(1, newDiff.getStartIndex());

        diff = new Diff("2:+4:test");
        newDiff = diff.convertToLF("\r\ntest");
        Assert.assertEquals(1, newDiff.getStartIndex());

        diff = new Diff("7:+4:test");
        newDiff = diff.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals(5, newDiff.getStartIndex());

        diff = new Diff("7:+4:test");
        newDiff = diff.convertToLF("\r\ntes\r\n");
        Assert.assertEquals(5, newDiff.getStartIndex());
    }

    @Test
    public void testTransformGeneral() {
        Diff diff1, diff2, diff3;
        List<Diff> result;

        // The brown fox
        // The quick brown fox
        // The slow brown fox
        diff1 = new Diff(true, 4, "quick");
        diff2 = new Diff(true, 4, "slow");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(9, result.get(0).getStartIndex());

        // ||||||
        // |||||quick|
        // ||||slow||
        // ||||slow|quick|
        diff1 = new Diff(true, 5, "quick");
        diff2 = new Diff(true, 4, "slow");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());

        // ||||||
        // |||quick|||
        // ||||slow||
        // |||quick|slow||
        diff1 = new Diff(true, 3, "quick");
        diff2 = new Diff(true, 4, "slow");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(9, result.get(0).getStartIndex());

        // the quick ||||||
        // quick ||||||
        // the ||||||
        // ||||||
        diff1 = new Diff(false, 0, "the ");
        diff2 = new Diff(false, 6, "quick");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals(5, result.get(0).getLength());

        // the quick ||||||
        // the ck ||||||
        // the qu ||||||
        // the ||||||
        diff1 = new Diff(false, 4, "qui");
        diff2 = new Diff(false, 6, "ick");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals(2, result.get(0).getLength());

        // the quick ||||||
        // the k ||||||
        // the qu ||||||
        // the ||||||
        diff1 = new Diff(false, 4, "quic");
        diff2 = new Diff(false, 6, "ick");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals(1, result.get(0).getLength());

        // the quick brown fox ||||||
        // the quick fox ||||||
        // the k brown fox ||||||
        // the k fox ||||||
        diff1 = new Diff(false, 10, "brown");
        diff2 = new Diff(false, 4, "quic");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals(4, result.get(0).getLength());

        // the quick brown fox||||||
        // the brown fox||||||
        // the quick brown ||||||
        // the brown ||||||
        diff1 = new Diff(false, 4, "quick");
        diff2 = new Diff(false, 16, "fox");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(11, result.get(0).getStartIndex());
        Assert.assertEquals(3, result.get(0).getLength());

        // abcdefgh
        // abpklfgh // Remove "cde", then add "pkl"
        // abcdgh // Remove "ef"
        // abpklgh // Should have a net removal of "cdef", add "pkl"
        diff1 = new Diff(false, 2, "cde");
        diff2 = new Diff(true, 2, "pkl");
        diff3 = new Diff(false, 4, "ef");
        result = diff3.transform(Arrays.asList(diff1, diff2));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(5, result.get(0).getStartIndex());
        Assert.assertEquals(1, result.get(0).getLength());
    }


    @Test
    public void testTransform1A() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(true, 2, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(true, 0, "str1");
        diff2 = new Diff(true, 1, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(5, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }


    @Test
    public void testTransform1B() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(true, 2, "str1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(true, 0, "str1");
        diff2 = new Diff(false, 1, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(5, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }


    @Test
    public void testTransform1C() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case i
        diff1 = new Diff(false, 2, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(false, 2, "longerstr1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        // Test else case
        diff1 = new Diff(false, 2, "str1");
        diff2 = new Diff(true, 6, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(false, 2, "str1");
        diff2 = new Diff(true, 8, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }

    @Test
    public void testTransform1D() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: if IndexA + LenA < IndexB (No overlap), shift B down by LenA
        diff1 = new Diff(false, 2, "str1");
        diff2 = new Diff(false, 8, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(false, 2, "str1");
        diff2 = new Diff(false, 6, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        // Test case 2: if IndexA + LenA >= IndexB + LenB, ignore B
        diff1 = new Diff(false, 2, "longerstr1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(0, result.size());

        // Test else cases: if overlapping, shorten B by overlap, shift down by LenA - overlap
        diff1 = new Diff(false, 2, "str1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(2, result.get(0).getStartIndex());
        Assert.assertEquals("r2", result.get(0).getChanges());
    }

    @Test
    public void testTransform2A() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(true, 4, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(true, 0, "longTestString1");
        diff2 = new Diff(true, 0, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(15, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

    }

    @Test
    public void testTransform2B() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(true, 4, "str1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(8, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(true, 0, "longTestString1");
        diff2 = new Diff(false, 0, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(15, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

    }

    @Test
    public void testTransform2C() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(false, 4, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(false, 0, "longTestString1");
        diff2 = new Diff(true, 0, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(0, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

    }

    @Test
    public void testTransform2D() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: If LenB > LenA, remove LenA characters from B
        diff1 = new Diff(false, 4, "str1");
        diff2 = new Diff(false, 4, "longTestString2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("TestString2", result.get(0).getChanges());

        // Test else case - if LenB <= LenA
        diff1 = new Diff(false, 0, "longTestString1");
        diff2 = new Diff(false, 0, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(0, result.size());

        diff1 = new Diff(false, 4, "str1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(0, result.size());

    }

    @Test
    public void testTransform3A() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(true, 5, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(true, 4, "str1");
        diff2 = new Diff(true, 0, "longTestString2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(0, result.get(0).getStartIndex());
        Assert.assertEquals("longTestString2", result.get(0).getChanges());
    }

    @Test
    public void testTransform3B() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: If IndexB + LenB > IndexA, split B into two diffs
        diff1 = new Diff(true, 5, "str1");
        diff2 = new Diff(false, 4, "longStr2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("l", result.get(0).getChanges());
        Assert.assertEquals(false, result.get(1).isInsertion());
        Assert.assertEquals(8, result.get(1).getStartIndex());
        Assert.assertEquals("ongStr2", result.get(1).getChanges());

        // Test else case: no change
        diff1 = new Diff(true, 8, "str1");
        diff2 = new Diff(false, 0, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(0, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }

    @Test
    public void testTransform3C() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(false, 9, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(false, 5, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());
    }

    @Test
    public void testTransform3D() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: If IndexB + LenB > IndexA, shorten B by overlap (from end)
        diff1 = new Diff(false, 6, "str1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("st", result.get(0).getChanges());

        // Test else case: No change if no overlap
        diff1 = new Diff(false, 8, "str1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(false, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("str2", result.get(0).getChanges());

        diff1 = new Diff(false, 10, "str1");
        diff2 = new Diff(false, 4, "str2");
        result = diff2.transform(Arrays.asList(diff1));
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
