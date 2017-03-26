package clientcore.patching;

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
}
