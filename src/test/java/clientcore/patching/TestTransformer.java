package clientcore.patching;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by wongb on 3/25/17.
 */
public class TestTransformer {
    @Test
    public void testPrecednce() {

        String baseText = "abdeletion";

        Patch patchA = new Patch("v0:\n2:-8:deletion,\n2:+6:insert:\n10");
        Patch patchB = new Patch("v0:\n10:+1:a:\n10");

        // Test with A having precedence
        Transformer.TransformResult result = Transformer.transformPatches(patchA, patchB);
        // Validate the the document that each path produces is the same
        Assert.assertEquals(
                new PatchManager().applyPatch(baseText, Arrays.asList(patchA, result.patchYPrime)),
                new PatchManager().applyPatch(baseText, Arrays.asList(patchB, result.patchXPrime))
        );

        // Test with B having precedence
        result = Transformer.transformPatches(patchB, patchA);
        // Validate the the document that each path produces is the same
        Assert.assertEquals(
                new PatchManager().applyPatch(baseText, Arrays.asList(patchB, result.patchYPrime)),
                new PatchManager().applyPatch(baseText, Arrays.asList(patchA, result.patchXPrime))
        );
    }

    @Test
    public void testOverlappingDeletes() {
        String baseText = "\n\tHello, my name is Ben. This is a test of whether this works properly.\n" +
                "\t\n" +
                "\tWow eclipse is dumb. It changed my \"Properly\" word to a entire public main method\n" +
                "\ttesting\n" +
                "\tSystem.out.println(\"Hellow this is a test\");\n" +
                "\t\n" +
                "\tif (true == false) {\n" +
                "\t\tdo all the things\n" +
                "\t\t2 = 1\n" +
                "\t\tThis is definitely coherent.  DEFINITELY\n" +
                "\t}";
        Patch patchA = new Patch("v557:\n0:-309:%0A%09Hello%2C+my+name+is+Ben.+This+is+a+test+of+whether+this+works+properly.%0A%09%0A%09Wow+eclipse+is+dumb.+It+changed+my+%22Properly%22+word+to+a+entire+public+main+method%0A%09testing%0A%09System.out.println%28%22Hellow+this+is+a+test%22%29%3B%0A%09%0A%09if+%28true+%3D%3D+false%29+%7B%0A%09%09do+all+the+things%0A%09%092+%3D+1%0A%09%09This+is+definitely+coherent.++DEFINITELY%0A%09%7D,\n0:+1:m:\n309");
        Patch patchB = new Patch("v557:\n308:-1:%7D:\n309");

        // Test with A having precedence
        Transformer.TransformResult result = Transformer.transformPatches(patchA, patchB);
        // Validate the the document that each path produces is the same
        Assert.assertEquals(
                new PatchManager().applyPatch(baseText, Arrays.asList(patchA, result.patchYPrime)),
                new PatchManager().applyPatch(baseText, Arrays.asList(patchB, result.patchXPrime))
        );

        // Test with B having precedence
        result = Transformer.transformPatches(patchB, patchA);
        // Validate the the document that each path produces is the same
        Assert.assertEquals(
                new PatchManager().applyPatch(baseText, Arrays.asList(patchB, result.patchYPrime)),
                new PatchManager().applyPatch(baseText, Arrays.asList(patchA, result.patchXPrime))
        );
    }

    private class TransformationTest {
        String desc;
        Patch patchA;
        Patch patchB;
        String baseText;
        Patch expectedPatchAPrime;
        Patch expectedPatchBPrime;
        boolean canReverse;

        public TransformationTest(String desc, Patch patchA, Patch patchB, String baseText, Patch expectedPatchAPrime, Patch expectedPatchBPrime, boolean canReverse) {
            this.desc = desc;
            this.patchA = patchA;
            this.patchB = patchB;
            this.baseText = baseText;
            this.expectedPatchAPrime = expectedPatchAPrime;
            this.expectedPatchBPrime = expectedPatchBPrime;
            this.canReverse = canReverse;
        }
    }

    private void runTests(TransformationTest[] tests) {
        for (TransformationTest test : tests) {
            Transformer.TransformResult result = null;

            try{
                result = Transformer.transformPatches(test.patchA, test.patchB);
            } catch(Exception e){
                Assert.fail(String.format("TestConsolidator[%s]: Transforming threw exception: %s", test.desc, e));
            }
            Assert.assertEquals(String.format("TestConsolidator[%s]: Patch A' was incorrect; expected [%s], got [%s]",
                    test.desc, test.expectedPatchAPrime.toString().replace("\n", "\\n"),
                    result.patchXPrime.toString().replace("\n", "\\n")), test.expectedPatchAPrime, result.patchXPrime);
            Assert.assertEquals(String.format("TestConsolidator[%s]: Patch B' was incorrect; expected [%s], got [%s]",
                    test.desc, test.expectedPatchBPrime.toString().replace("\n", "\\n"),
                    result.patchYPrime.toString().replace("\n", "\\n")), test.expectedPatchBPrime, result.patchYPrime);

            // Validate the the document that each path produces is the same
            Assert.assertEquals(String.format("TestConsolidator[%s]: Document was different based on patch application order", test.desc),
                    new PatchManager().applyPatch(test.baseText, Arrays.asList(test.patchA, result.patchYPrime)),
                    new PatchManager().applyPatch(test.baseText, Arrays.asList(test.patchB, result.patchXPrime))
            );

            // If is reversible (does not require precedence, try running it in reverse
            if(test.canReverse){
                try{
                    result = Transformer.transformPatches(test.patchB, test.patchA);
                } catch(Exception e){
                    Assert.fail(String.format("TestConsolidator[%s-Reverse]: Transforming threw exception: %s", test.desc, e));
                }
                Assert.assertEquals(String.format("TestConsolidator[%s-Reverse]: Patch B' was incorrect; expected [%s], got [%s]",
                        test.desc, test.expectedPatchAPrime.toString().replace("\n", "\\n"),
                        result.patchXPrime.toString().replace("\n", "\\n")), test.expectedPatchBPrime, result.patchXPrime);
                Assert.assertEquals(String.format("TestConsolidator[%s-Reverse]: Patch A' was incorrect; expected [%s], got [%s]",
                        test.desc, test.expectedPatchBPrime.toString().replace("\n", "\\n"),
                        result.patchYPrime.toString().replace("\n", "\\n")), test.expectedPatchAPrime, result.patchYPrime);

                // Validate the the document that each path produces is the same
                Assert.assertEquals(String.format("TestConsolidator[%s-Reverse]: Document was different based on patch application order", test.desc),
                        new PatchManager().applyPatch(test.baseText, Arrays.asList(test.patchB, result.patchYPrime)),
                        new PatchManager().applyPatch(test.baseText, Arrays.asList(test.patchA, result.patchXPrime))
                );
            }
        }
    }

    @Test
    public void testTransform1A() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Non-Overlapping strings",
                        new Patch("v1:\n0:+4:str1:\n8"),
                        new Patch("v1:\n6:+4:str2:\n8"),
                        "baseText",
                        new Patch("v2:\n0:+4:str1:\n12"),
                        new Patch("v2:\n10:+4:str2:\n12"),
                        true
                ),
                new TransformationTest(
                        "Overlapping strings",
                        new Patch("v1:\n2:+4:str1:\n8"),
                        new Patch("v1:\n4:+4:str2:\n8"),
                        "baseText",
                        new Patch("v2:\n2:+4:str1:\n12"),
                        new Patch("v2:\n8:+4:str2:\n12"),
                        true
                )
        };

        runTests(tests);
    }


    @Test
    public void testTransform1B() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Non-Overlapping strings",
                        new Patch("v1:\n0:+2:s1:\n8"),
                        new Patch("v1:\n4:-4:Text:\n8"),
                        "baseText",
                        new Patch("v2:\n0:+2:s1:\n4"),
                        new Patch("v2:\n6:-4:Text:\n10"),
                        true
                ),
                new TransformationTest(
                        "Overlapping strings",
                        new Patch("v1:\n2:+4:str1:\n8"),
                        new Patch("v1:\n4:-4:Text:\n8"),
                        "baseText",
                        new Patch("v2:\n2:+4:str1:\n4"),
                        new Patch("v2:\n8:-4:Text:\n12"),
                        true
                )
        };

        runTests(tests);
    }


    @Test
    public void testTransform1C() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Non-Overlapping strings",
                        new Patch("v1:\n0:-2:ba:\n8"),
                        new Patch("v1:\n4:+4:abcd:\n8"),
                        "baseText",
                        new Patch("v2:\n0:-2:ba:\n12"),
                        new Patch("v2:\n2:+4:abcd:\n6"),
                        true
                ),
                new TransformationTest(
                        "Overlapping strings",
                        new Patch("v1:\n2:-4:seTe:\n8"),
                        new Patch("v1:\n4:+4:abcd:\n8"),
                        "baseText",
                        new Patch("v2:\n2:-2:se,\n8:-2:Te:\n12"),
                        new Patch("v2:\n2:+4:abcd:\n4"),
                        true
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform1D() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Non-Overlapping strings",
                        new Patch("v1:\n2:-4:str1:\n16"),
                        new Patch("v1:\n8:-4:str2:\n16"),
                        "bastr1sestr2Text",
                        new Patch("v2:\n2:-4:str1:\n12"),
                        new Patch("v2:\n4:-4:str2:\n12"),
                        true
                ),
                new TransformationTest(
                        "Non-Overlapping strings, adjacent",
                        new Patch("v1:\n2:-4:str1:\n16"),
                        new Patch("v1:\n6:-4:str2:\n16"),
                        "bastr1str2seText",
                        new Patch("v2:\n2:-4:str1:\n12"),
                        new Patch("v2:\n2:-4:str2:\n12"),
                        true
                ),
                new TransformationTest(
                        "Overlapping strings",
                        new Patch("v1:\n2:-4:seTe:\n8"),
                        new Patch("v1:\n4:-4:Text:\n8"),
                        "baseText",
                        new Patch("v2:\n2:-2:se:\n4"),
                        new Patch("v2:\n2:-2:xt:\n4"),
                        true
                ),
                new TransformationTest(
                        "Overlapping strings, B subset of A",
                        new Patch("v1:\n2:-6:seText:\n8"),
                        new Patch("v1:\n4:-2:Te:\n8"),
                        "baseText",
                        new Patch("v2:\n2:-4:sext:\n6"),
                        new Patch("v2:\n:\n2"),
                        true
                ),
        };

        runTests(tests);
    }

    @Test
    public void testTransform2A() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Same length strings",
                        new Patch("v1:\n4:+4:str1:\n8"),
                        new Patch("v1:\n4:+4:str2:\n8"),
                        "testText",
                        new Patch("v2:\n4:+4:str1:\n12"),
                        new Patch("v2:\n8:+4:str2:\n12"),
                        false
                ),
                new TransformationTest(
                        "A longer",
                        new Patch("v1:\n4:+8:longstr1:\n8"),
                        new Patch("v1:\n4:+4:str2:\n8"),
                        "testText",
                        new Patch("v2:\n4:+8:longstr1:\n12"),
                        new Patch("v2:\n12:+4:str2:\n16"),
                        false
                ),
                new TransformationTest(
                        "B longer",
                        new Patch("v1:\n4:+4:str1:\n8"),
                        new Patch("v1:\n4:+8:longstr2:\n8"),
                        "testText",
                        new Patch("v2:\n4:+4:str1:\n16"),
                        new Patch("v2:\n8:+8:longstr2:\n12"),
                        false
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform2B() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Same length strings",
                        new Patch("v1:\n2:+4:str1:\n8"),
                        new Patch("v1:\n2:-4:stTe:\n8"),
                        "testText",
                        new Patch("v2:\n2:+4:str1:\n4"),
                        new Patch("v2:\n6:-4:stTe:\n12"),
                        true
                ),
                new TransformationTest(
                        "A longer",
                        new Patch("v1:\n2:+8:longstr1:\n8"),
                        new Patch("v1:\n2:-4:stTe:\n8"),
                        "testText",
                        new Patch("v2:\n2:+8:longstr1:\n4"),
                        new Patch("v2:\n10:-4:stTe:\n16"),
                        true
                ),
                new TransformationTest(
                        "B longer",
                        new Patch("v1:\n4:+4:str1:\n14"),
                        new Patch("v1:\n4:-6:longer:\n14"),
                        "testlongerText",
                        new Patch("v2:\n4:+4:str1:\n8"),
                        new Patch("v2:\n8:-6:longer:\n18"),
                        true
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform2C() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Same length strings",
                        new Patch("v1:\n2:-4:stTe:\n8"),
                        new Patch("v1:\n2:+4:str2:\n8"),
                        "testText",
                        new Patch("v2:\n6:-4:stTe:\n12"),
                        new Patch("v2:\n2:+4:str2:\n4"),
                        true
                ),
                new TransformationTest(
                        "A longer",
                        new Patch("v1:\n4:-6:longer:\n14"),
                        new Patch("v1:\n4:+4:str2:\n14"),
                        "testlongerText",
                        new Patch("v2:\n8:-6:longer:\n18"),
                        new Patch("v2:\n4:+4:str2:\n8"),
                        true
                ),
                new TransformationTest(
                        "B longer",
                        new Patch("v1:\n2:-4:stTe:\n8"),
                        new Patch("v1:\n2:+8:longstr2:\n8"),
                        "testText",
                        new Patch("v2:\n10:-4:stTe:\n16"),
                        new Patch("v2:\n2:+8:longstr2:\n4"),
                        true
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform2D() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Same length strings",
                        new Patch("v1:\n2:-4:stTe:\n8"),
                        new Patch("v1:\n2:-4:stTe:\n8"),
                        "testText",
                        new Patch("v2:\n:\n4"),
                        new Patch("v2:\n:\n4"),
                        true
                ),
                new TransformationTest(
                        "A longer",
                        new Patch("v1:\n4:-6:longer:\n14"),
                        new Patch("v1:\n4:-4:long:\n14"),
                        "testlongerText",
                        new Patch("v2:\n4:-2:er:\n10"),
                        new Patch("v2:\n:\n8"),
                        true
                ),
                new TransformationTest(
                        "B longer",
                        new Patch("v1:\n2:-4:stlo:\n14"),
                        new Patch("v1:\n2:-6:stlong:\n14"),
                        "testlongerText",
                        new Patch("v2:\n:\n8"),
                        new Patch("v2:\n2:-2:ng:\n10"),
                        true
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform3A() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Overlapping strings",
                        new Patch("v1:\n5:+4:str1:\n8"),
                        new Patch("v1:\n4:+4:str2:\n8"),
                        "testText",
                        new Patch("v2:\n9:+4:str1:\n12"),
                        new Patch("v2:\n4:+4:str2:\n12"),
                        true
                ),
                new TransformationTest(
                        "Non-overlapping strings",
                        new Patch("v1:\n4:+4:str1:\n8"),
                        new Patch("v1:\n0:+15:longTestString2:\n8"),
                        "testText",
                        new Patch("v2:\n19:+4:str1:\n23"),
                        new Patch("v2:\n0:+15:longTestString2:\n12"),
                        true
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform3B() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Overlapping strings",
                        new Patch("v1:\n5:+4:str1:\n8"),
                        new Patch("v1:\n4:-4:Text:\n8"),
                        "testText",
                        new Patch("v2:\n4:+4:str1:\n4"),
                        new Patch("v2:\n4:-1:T,\n9:-3:ext:\n12"),
                        true
                ),
                new TransformationTest(
                        "Non-overlapping strings",
                        new Patch("v1:\n5:+4:str1:\n8"),
                        new Patch("v1:\n2:-2:st:\n8"),
                        "testText",
                        new Patch("v2:\n3:+4:str1:\n6"),
                        new Patch("v2:\n2:-2:st:\n12"),
                        true
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform3C() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Overlapping strings",
                        new Patch("v1:\n5:-3:ext:\n8"),
                        new Patch("v1:\n4:+4:str2:\n8"),
                        "testText",
                        new Patch("v2:\n9:-3:ext:\n12"),
                        new Patch("v2:\n4:+4:str2:\n5"),
                        true
                ),
                new TransformationTest(
                        "Non-overlapping strings",
                        new Patch("v1:\n5:-3:ext:\n8"),
                        new Patch("v1:\n1:+2:s2:\n8"),
                        "testText",
                        new Patch("v2:\n7:-3:ext:\n10"),
                        new Patch("v2:\n1:+2:s2:\n5"),
                        true
                )
        };

        runTests(tests);
    }

    @Test
    public void testTransform3D() {
        TransformationTest[] tests = {
                new TransformationTest(
                        "Overlapping strings, A extends past B",
                        new Patch("v1:\n4:-8:LongerTe:\n14"),
                        new Patch("v1:\n2:-4:stLo:\n14"),
                        "testLongerText",
                        new Patch("v2:\n2:-6:ngerTe:\n10"),
                        new Patch("v2:\n2:-2:st:\n6"),
                        true
                ),
                new TransformationTest(
                        "Overlapping strings, A ends at same index as B",
                        new Patch("v1:\n4:-4:Long:\n14"),
                        new Patch("v1:\n2:-6:stLong:\n14"),
                        "testLongerText",
                        new Patch("v2:\n:\n8"),
                        new Patch("v2:\n2:-2:st:\n10"),
                        true
                ),
                new TransformationTest(
                        "Overlapping strings, A ends before B",
                        new Patch("v1:\n4:-3:Lon:\n14"),
                        new Patch("v1:\n2:-6:stLong:\n14"),
                        "testLongerText",
                        new Patch("v2:\n:\n8"),
                        new Patch("v2:\n2:-3:stg:\n11"),
                        true
                ),
                new TransformationTest(
                        "Non-overlapping strings",
                        new Patch("v1:\n5:-3:ext:\n8"),
                        new Patch("v1:\n1:-2:es:\n8"),
                        "testText",
                        new Patch("v2:\n3:-3:ext:\n6"),
                        new Patch("v2:\n1:-2:es:\n5"),
                        true
                )
        };

        runTests(tests);
    }
}
