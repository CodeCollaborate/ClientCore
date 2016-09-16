package patching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
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
        Diff diff = new Diff("0:+5:test%0A");
        Diff newDiff = diff.convertToCRLF("\r\ntest");
        Assert.assertEquals("0:+6:test%0D%0A", newDiff.toString());

        diff = new Diff("1:+5:test%0A");
        newDiff = diff.convertToCRLF("\r\ntest");
        Assert.assertEquals("2:+6:test%0D%0A", newDiff.toString());

        diff = new Diff("2:+5:test%0A");
        newDiff = diff.convertToCRLF("\r\ntest");
        Assert.assertEquals("3:+6:test%0D%0A", newDiff.toString());

        diff = new Diff("7:+5:test%0A");
        newDiff = diff.convertToCRLF("\r\ntes\r\nt");
        Assert.assertEquals("9:+6:test%0D%0A", newDiff.toString());
    }

    @Test
    public void testConvertToLF() {
        Diff diff = new Diff("0:+6:test%0D%0A");
        Diff newDiff = diff.convertToLF("\r\ntest");
        Assert.assertEquals("0:+5:test%0A", newDiff.toString());

        diff = new Diff("2:+6:test%0D%0A");
        newDiff = diff.convertToLF("\r\ntest");
        Assert.assertEquals("1:+5:test%0A", newDiff.toString());

        diff = new Diff("7:+6:test%0D%0A");
        newDiff = diff.convertToLF("\r\ntes\r\nt");
        Assert.assertEquals("5:+5:test%0A", newDiff.toString());
    }

    @Test
    public void testConvertBack() {
        Diff diff = new Diff("53:+1:a");
        Diff LFDiff = diff.convertToLF("package testPkg1;\r\n" +
                "\r\n" +
                "public class TestClass1 {\r\n" +
                "\r\n" +
                "}\r\n");
        Diff CRLFDiff = LFDiff.convertToCRLF("package testPkg1;\r\n" +
                "\r\n" +
                "public class TestClass1 {\r\n" +
                "\r\n" +
                "}\r\n");
        Assert.assertEquals(diff.toString(), CRLFDiff.toString());
    }

    @Test
    public void testUndo() {
        Diff diff = new Diff("0:+4:str1");
        Diff newDiff = diff.getUndo();
        Assert.assertEquals("0:-4:str1", newDiff.toString());
        Diff originalDiff = newDiff.getUndo();
        Assert.assertEquals(diff.toString(), originalDiff.toString());

        diff = new Diff("1:-4:str2");
        newDiff = diff.getUndo();
        Assert.assertEquals("1:+4:str2", newDiff.toString());
        originalDiff = newDiff.getUndo();
        Assert.assertEquals(diff.toString(), originalDiff.toString());
    }

    @Test
    public void testTransform1A() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff(true, 2, "str1");
        diff2 = new Diff(true, 4, "str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("8:+4:str2", result.get(0).toString());

        diff1 = new Diff(true, 0, "str1");
        diff2 = new Diff(true, 1, "str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("5:+4:str2", result.get(0).toString());
    }


    @Test
    public void testTransform1B() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff("2:+4:str1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("8:-4:str2", result.get(0).toString());

        diff1 = new Diff("0:+4:str1");
        diff2 = new Diff("1:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("5:-4:str2", result.get(0).toString());
    }


    @Test
    public void testTransform1C() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1
        diff1 = new Diff("2:-4:str1");
        diff2 = new Diff("4:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2:+4:str2", result.get(0).toString());

        diff1 = new Diff("2:-10:longerstr1");
        diff2 = new Diff("4:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2:+4:str2", result.get(0).toString());

        // Test else case
        diff1 = new Diff("2:-4:str1");
        diff2 = new Diff("6:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2:+4:str2", result.get(0).toString());

        diff1 = new Diff("2:-4:str1");
        diff2 = new Diff("8:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:+4:str2", result.get(0).toString());
    }

    @Test
    public void testTransform1D() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: if IndexA + LenA < IndexB (No overlap), shift B down by LenA
        diff1 = new Diff("2:-4:str1");
        diff2 = new Diff("8:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:-4:str2", result.get(0).toString());

        diff1 = new Diff("2:-4:str1");
        diff2 = new Diff("6:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2:-4:str2", result.get(0).toString());

        // Test case 2: if IndexA + LenA >= IndexB + LenB, ignore B
        diff1 = new Diff("2:-10:longerstr1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(0, result.size());

        // Test else cases: if overlapping, shorten B by overlap, shift down by LenA - overlap
        diff1 = new Diff("2:-4:str1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("2:-2:r2", result.get(0).toString());
    }

    @Test
    public void testTransform2A() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff("4:+4:str1");
        diff2 = new Diff("4:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("8:+4:str2", result.get(0).toString());

        diff1 = new Diff("0:+15:longTestString1");
        diff2 = new Diff("0:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals("15:+4:str2", result.get(0).toString());

    }

    @Test
    public void testTransform2B() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff("4:+4:str1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("8:-4:str2", result.get(0).toString());

        diff1 = new Diff("0:+15:longTestString1");
        diff2 = new Diff("0:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("15:-4:str2", result.get(0).toString());

    }

    @Test
    public void testTransform2C() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff("4:-4:str1");
        diff2 = new Diff("4:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(4, result.get(0).getStartIndex());
        Assert.assertEquals("4:+4:str2", result.get(0).toString());

        diff1 = new Diff("0:-15:longTestString1");
        diff2 = new Diff("0:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(true, result.get(0).isInsertion());
        Assert.assertEquals(0, result.get(0).getStartIndex());
        Assert.assertEquals("0:+4:str2", result.get(0).toString());

    }

    @Test
    public void testTransform2D() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: If LenB > LenA, remove LenA characters from B
        diff1 = new Diff("4:-4:str1");
        diff2 = new Diff("4:-15:longTestString2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:-11:TestString2", result.get(0).toString());

        // Test else case - if LenB <= LenA
        diff1 = new Diff("0:-15:longTestString1");
        diff2 = new Diff("0:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(0, result.size());

        diff1 = new Diff("4:-4:str1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(0, result.size());

    }

    @Test
    public void testTransform3A() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff("5:+4:str1");
        diff2 = new Diff("4:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:+4:str2", result.get(0).toString());

        diff1 = new Diff("4:+4:str1");
        diff2 = new Diff("0:+15:longTestString2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("0:+15:longTestString2", result.get(0).toString());
    }

    @Test
    public void testTransform3B() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: If IndexB + LenB > IndexA, split B into two diffs
        diff1 = new Diff("5:+4:str1");
        diff2 = new Diff("4:-8:longStr2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("4:-1:l", result.get(0).toString());
        Assert.assertEquals("8:-7:ongStr2", result.get(1).toString());

        // Test else case: no change
        diff1 = new Diff("8:+4:str1");
        diff2 = new Diff("0:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("0:-4:str2", result.get(0).toString());
    }

    @Test
    public void testTransform3C() {
        Diff diff1, diff2;
        List<Diff> result;

        diff1 = new Diff("9:-4:str1");
        diff2 = new Diff("4:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:+4:str2", result.get(0).toString());

        diff1 = new Diff("5:-15:longTestString1");
        diff2 = new Diff("4:+4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:+4:str2", result.get(0).toString());
    }

    @Test
    public void testTransform3D() {
        Diff diff1, diff2;
        List<Diff> result;

        // Test case 1: If IndexB + LenB > IndexA, shorten B by overlap (from end)
        diff1 = new Diff("6:-4:str1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:-2:st", result.get(0).toString());

        // Test else case: No change if no overlap
        diff1 = new Diff("8:-4:str1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:-4:str2", result.get(0).toString());

        diff1 = new Diff("10:-4:str1");
        diff2 = new Diff("4:-4:str2");
        result = diff2.transform(Collections.singletonList(diff1));
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("4:-4:str2", result.get(0).toString());
    }
}
