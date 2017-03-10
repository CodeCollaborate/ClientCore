package clientcore.patching;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by wongb on 3/9/17.
 */
public class Consolidator {
    public static Patch consolidatePatches(Collection<Patch> patches){
        return consolidatePatches(patches.toArray(new Patch[patches.size()]));
    }

    public static Patch consolidatePatches(Patch[] patches){
        if(patches.length <= 0){
            return null;
        }

        Patch patchA = patches[0];
        for(int i = 1; i < patches.length; i++){
            Patch patchB = patches[i];

            final int[] indexAArr = {-1};
            final int[] indexBArr = {-1};
            final ArrayList<Diff> resultDiffs = new ArrayList<>();
            int currIndex = 0;

            NextDiffResult ndResultA = getNextDiff(patchA, indexAArr[0], false);
            NextDiffResult ndResultB = getNextDiff(patchB, indexBArr[0], false);

            final Diff[] diffAArr = {ndResultA.diff};
            indexAArr[0] = ndResultA.index;
            final Diff[] diffBArr = {ndResultB.diff};
            indexBArr[0] = ndResultB.index;

            Patch finalPatchA = patchA;
            NextDiffLambda getNextDiffA = () -> {
                NextDiffResult ndResult = getNextDiff(finalPatchA, indexAArr[0], false);

                diffAArr[0] = ndResult.diff;
                indexAArr[0] = ndResult.index;
            };
            NextDiffLambda getNextDiffB = () -> {
                NextDiffResult ndResult = getNextDiff(patchB, indexAArr[0], false);

                diffBArr[0] = ndResult.diff;
                indexBArr[0] = ndResult.index;
            };

            int finalCurrIndex = currIndex;
            Committer committer = (Diff diff, int numChars) -> {
                if (numChars == -1) {
                    resultDiffs.add(new Diff(diff.isInsertion(), finalCurrIndex, diff.getChanges()));
                } else {
                    resultDiffs.add(new Diff(diff.isInsertion(), finalCurrIndex, diff.getChanges().substring(0, numChars)));
                }
            };

            while (diffAArr[0] != null && diffBArr[0] != null){
                // Get lengths of each diff
                int lenA = 0;
                int lenB = 0;
                if (isNoOp(diffAArr[0])) {
                    lenA = noOpLength(diffAArr[0], patchA, indexAArr[0]);
                } else {
                    lenA = diffAArr[0].getLength();
                }
                if (isNoOp(diffBArr[0])) {
                    lenB = noOpLength(diffBArr[0], patchB, indexBArr[0]);
                } else {
                    lenB = diffBArr[0].getLength();
                }

                if (!diffAArr[0].isInsertion() && !isNoOp(diffAArr[0])) {
                    committer.commit(diffAArr[0], -1);
                    currIndex += lenA;
                    getNextDiffA.next();
                } else if (diffBArr[0].isInsertion() && !isNoOp(diffBArr[0])) {
                    committer.commit(diffBArr[0], -1);
                    getNextDiffB.next();
                } else {
                    // Commit changes and update currIndex as needed
                    if(!isNoOp(diffAArr[0]) && diffAArr[0].isInsertion() && !isNoOp(diffBArr[0]) && !diffBArr[0].isInsertion()) {
                        // do nothing
                    } else if(!isNoOp(diffAArr[0]) && diffAArr[0].isInsertion() && isNoOp(diffBArr[0])) {
                        if (lenA < lenB || lenA == lenB) {
                            committer.commit(diffAArr[0], -1);
                        } else {
                                committer.commit(diffAArr[0], lenB);
                        }
                    } else if (isNoOp(diffAArr[0]) && !isNoOp(diffBArr[0]) && !diffBArr[0].isInsertion()) {
                        if (lenA < lenB) {
                            committer.commit(diffBArr[0], lenA);
                            currIndex += lenA;
                        } else if (lenA == lenB) {
                            committer.commit(diffBArr[0], -1);
                            currIndex += lenA;
                        } else {
                            committer.commit(diffBArr[0], -1);
                            currIndex += lenB;
                        }
                    } else if (isNoOp(diffAArr[0]) && isNoOp(diffBArr[0])) {
                        if (lenA < lenB || lenA == lenB) {
                            currIndex += lenA;
                        } else {
                            currIndex += lenB;
                        }
                    }

                    // Update the diff and get new ones if needed
                    if (lenA < lenB) {
                        if (isNoOp(diffBArr[0])) {
                            diffBArr[0].setStartIndex(diffBArr[0].getStartIndex() + lenA);
                        } else{
                            diffBArr[0].setChanges(diffBArr[0].getChanges().substring(lenA));
                        }
                        getNextDiffA.next();
                    } else if (lenA == lenB) {
                        getNextDiffA.next();
                        getNextDiffB.next();
                    } else {
                            if (isNoOp(diffAArr[0])) {
                            diffAArr[0].setStartIndex(diffAArr[0].getStartIndex() + lenB);
                        } else {
                            diffAArr[0].setChanges(diffAArr[0].getChanges().substring(lenB));
                        }
                        getNextDiffB.next();
                    }
                }
            }
            patchA = new Patch(patchA.getBaseVersion(), resultDiffs, patchA.getDocLength());
        }
        return patchA;
    }

    private interface NextDiffLambda{
        void next();
    }
    private interface Committer{
        void commit(Diff diff, int numChars);
    }

    private static class NextDiffResult{
        private Diff diff;
        private int index;

        public NextDiffResult(Diff diff, int index) {
            this.diff = diff;
            this.index = index;
        }
    }

    private static NextDiffResult getNextDiff(Patch patch, int currIndex, boolean wasNoOp){
        if (currIndex == -1){
            if (!wasNoOp) {
                return new NextDiffResult(new Diff(true, 0, ""), -1);
            } else if (patch.getDiffs().size() <= 0){
                return new NextDiffResult(null, -1);
            }
            return new NextDiffResult(patch.getDiffs().get(0).clone(), 0);
        }

        // If previous one was a noOp, return either the end or the next actual diff
        if (wasNoOp){
            // Return end if we have gone past it.
            if (currIndex+1 >= patch.getDiffs().size()) {
                return new NextDiffResult(null, -1);
            }
            // Return next diff otherwise.
            return new NextDiffResult(patch.getDiffs().get(currIndex+1).clone(), currIndex + 1);
        }
        // Else return the next value
        Diff currDiff = patch.getDiffs().get(currIndex);
        // If we have no more slice, and our current diff does not go to the end, return a new noop diff
        if (currIndex+1 >= patch.getDiffs().size()) {
            // If it is an insertion, return new noOp diff at start index.
            // Else, return a noop diff after the removed block
            if (currDiff.isInsertion()) {
                return new NextDiffResult(new Diff(true, currDiff.getStartIndex(), ""), currIndex);
            }
            return new NextDiffResult(new Diff(true, currDiff.getStartIndex()+currDiff.getLength(), ""), currIndex);

        }
        //return the next diff if it is adjacent, or a noOp otherwise.
        Diff nextDiff = patch.getDiffs().get(currIndex+1);
        if (nextDiff.getStartIndex() == currDiff.getStartIndex() || !currDiff.isInsertion()
                && currDiff.getStartIndex()+currDiff.getLength() >= nextDiff.getStartIndex()) {
            return new NextDiffResult(nextDiff.clone(), currIndex + 1);
        }
        if (currDiff.isInsertion()) {
            return new NextDiffResult(new Diff(true, currDiff.getStartIndex(), ""), currIndex);
        }
        return new NextDiffResult(new Diff(true, currDiff.getStartIndex()+currDiff.getLength(), ""), currIndex);
    }

    private static boolean isNoOp(Diff diff){
        return diff.getLength() == 0;
    }

    private static int noOpLength(Diff diff, Patch patch, int currIndex){
        if (currIndex + 1 >= patch.getDiffs().size()){
            return patch.getDocLength() - diff.getStartIndex() + 1;
        }
        return patch.getDiffs().get(currIndex + 1).getStartIndex() - diff.getStartIndex();
    }
}


