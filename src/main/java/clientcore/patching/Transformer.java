package clientcore.patching;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wongb on 3/9/17.
 */
public class Transformer {
    public static final Logger logger = LogManager.getLogger("clientcore/transformer");

    public static TransformResult transformPatches(Patch patchX, Patch patchY) {
        if (patchX.getDocLength() != patchY.getDocLength()) {
            logger.error("Base document lengths for patch X [%d] was different from that of patch Y [%d]");
        }

        List<Diff> patchXPrime = new ArrayList<>();
        List<Diff> patchYPrime = new ArrayList<>();

        final int[] indexX = {-1};
        final int[] indexY = {-1};

        final int[] startIndexX = {0};
        final int[] startIndexY = {0};

        final int[] noOpXLen = new int[1];
        final Diff[] diffX = new Diff[1];
        final int[] noOpYLen = new int[1];
        final Diff[] diffY = new Diff[1];

        NextDiffLambda getNextDiffX = () -> {
            indexX[0]++;

            if (indexX[0] < patchX.getDiffs().size()) {
                diffX[0] = patchX.getDiffs().get(indexX[0]).clone();

                noOpXLen[0] = diffX[0].getStartIndex();
                if (indexX[0] > 0) {
                    Diff prev = patchX.getDiffs().get(indexX[0] - 1);
                    if (prev.isInsertion() || prev.getStartIndex() == diffX[0].getStartIndex()) {
                        noOpXLen[0] = diffX[0].getStartIndex() - prev.getStartIndex();
                    } else {
                        noOpXLen[0] = diffX[0].getStartIndex() - (prev.getStartIndex() + prev.getLength());
                    }
                }
            } else {
                // Last no-op
                Diff prev = patchX.getDiffs().get(patchX.getDiffs().size() - 1);
                noOpXLen[0] = patchX.getDocLength() - (prev.getStartIndex() + (prev.isInsertion() ? 0 : prev.getLength()));
                diffX[0] = null;
            }
        };

        NextDiffLambda getNextDiffY = () -> {
            indexY[0]++;

            if (indexY[0] < patchY.getDiffs().size()) {
                diffY[0] = patchY.getDiffs().get(indexY[0]).clone();

                noOpYLen[0] = diffY[0].getStartIndex();
                if (indexY[0] > 0) {
                    Diff prev = patchY.getDiffs().get(indexY[0] - 1);
                    if (prev.isInsertion() || prev.getStartIndex() == diffY[0].getStartIndex()) {
                        noOpYLen[0] = diffY[0].getStartIndex() - prev.getStartIndex();
                    } else {
                        noOpYLen[0] = diffY[0].getStartIndex() - (prev.getStartIndex() + prev.getLength());
                    }
                }
            } else {
                // Last no-op
                Diff prev = patchY.getDiffs().get(patchY.getDiffs().size() - 1);
                noOpYLen[0] = patchY.getDocLength() - (prev.getStartIndex() + (prev.isInsertion() ? 0 : prev.getLength()));
                diffY[0] = null;
            }
        };

        getNextDiffX.next();
        getNextDiffY.next();

        while (true) {
            int noOpLength = Math.max(0, Math.min(noOpXLen[0], noOpYLen[0]));
            startIndexX[0] += noOpLength;
            startIndexY[0] += noOpLength;
            noOpXLen[0] -= noOpLength;
            noOpYLen[0] -= noOpLength;
            if (diffX[0] == null && diffY[0] == null) {
                break;
            }

            int diffXLen = diffX[0] == null ? noOpXLen[0] : diffX[0].getLength();
            int diffYLen = diffY[0] == null ? noOpYLen[0] : diffY[0].getLength();

            if (diffX[0] != null && diffX[0].isInsertion() && noOpXLen[0] == 0) {
                patchXPrime.add(new Diff(true, startIndexX[0], diffX[0].getChanges()));

                startIndexY[0] += diffXLen;

                getNextDiffX.next();
                continue;
            } else if (diffY[0] != null && diffY[0].isInsertion() && noOpYLen[0] == 0) {
                patchYPrime.add(new Diff(true, startIndexY[0], diffY[0].getChanges()));

                startIndexX[0] += diffYLen;

                getNextDiffY.next();
                continue;
            }

            if (noOpXLen[0] == 0 && noOpYLen[0] == 0) {
                if (diffXLen < diffYLen) {
                    // Nothing to commit

                    // Already been deleted; remove first lenX characters from diffY
                    diffY[0].setChanges(diffY[0].getChanges().substring(diffXLen));

                    // Done with this diffX
                    getNextDiffX.next();
                } else if (diffXLen == diffYLen) {
                    // Both deleted the same text; nothing to commit

                    getNextDiffX.next();
                    getNextDiffY.next();
                } else {
                    // Nothing to commit

                    // Already been deleted; remove first lenY characters from diffX
                    diffX[0].setChanges(diffX[0].getChanges().substring(diffYLen));

                    // Done with this diffY
                    getNextDiffY.next();
                }
            } else if (noOpXLen[0] == 0 && noOpYLen[0] > 0) {
                int commitLength = Math.min(noOpYLen[0], diffXLen);
                patchXPrime.add(new Diff(false, startIndexX[0], diffX[0].getChanges().substring(0, commitLength)));

                startIndexX[0] += commitLength;
                diffX[0].setChanges(diffX[0].getChanges().substring(commitLength));

                noOpYLen[0] -= commitLength;

                // If we have exhausted the entire diffY, proceed to next diff
                if (diffX[0].getLength() == 0) {
                    getNextDiffX.next();
                }
            } else if (noOpXLen[0] > 0 && noOpYLen[0] == 0) {
                int commitLength = Math.min(noOpXLen[0], diffYLen);
                patchYPrime.add(new Diff(false, startIndexY[0], diffY[0].getChanges().substring(0, commitLength)));

                startIndexY[0] += commitLength;
                diffY[0].setChanges(diffY[0].getChanges().substring(commitLength));

                noOpXLen[0] -= commitLength;

                // If we have exhausted the entire diffY, proceed to next diff
                if (diffY[0].getLength() == 0) {
                    getNextDiffY.next();
                }
            } else {
                throw new IllegalStateException(String.format("Got to invalid state based on noOpXLen [%d] and noOpYLen[%d]", noOpXLen[0], noOpYLen[0]));
            }
        }

        // Less efficient, but easier to conceptualize
        int newDocXLen = patchX.getDocLength();
        for (Diff diff : patchY.getDiffs()) {
            if (diff.isInsertion()) {
                newDocXLen += diff.getLength();
            } else {
                newDocXLen -= diff.getLength();
            }
        }

        int newDocYLen = patchY.getDocLength();
        for (Diff diff : patchX.getDiffs()) {
            if (diff.isInsertion()) {
                newDocYLen += diff.getLength();
            } else {
                newDocYLen -= diff.getLength();
            }
        }

        return new TransformResult(
                new Patch(patchY.getBaseVersion() + 1, patchXPrime, newDocXLen),
                new Patch(patchX.getBaseVersion() + 1, patchYPrime, newDocYLen)
        );
    }

    public static class TransformResult {
        public final Patch patchXPrime;
        public final Patch patchYPrime;

        public TransformResult(Patch patchXPrime, Patch patchYPrime) {
            this.patchXPrime = patchXPrime;
            this.patchYPrime = patchYPrime;
        }
    }

    private interface NextDiffLambda {
        void next();
    }
}
