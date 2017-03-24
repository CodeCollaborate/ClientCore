package clientcore.patching;

import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;

/**
 * Created by Benedict Wong on 5/8/2016.
 */

public class Patch {
    private long baseVersion;
    private List<Diff> diffs;
    private final int docLength;

    public static Patch[] getPatches(String[] patchStrs){
        Patch[] patches = new Patch[patchStrs.length];
        for(int i = 0; i < patchStrs.length; i++){
            patches[i] = new Patch(patchStrs[i]);
        }
        return patches;
    }

    public Patch(long baseVersion, List<Diff> diffs, int docLength) {
        this.baseVersion = baseVersion;
        this.diffs = diffs;
        this.docLength = docLength;

        this.simplify();
    }

    public Patch(String str) {
        String[] parts = str.split(":\n");

        if(parts.length < 3) {
            throw new IllegalArgumentException("Invalid patch format: Not enough sections");
        }

        this.baseVersion = Integer.parseInt(parts[0].substring(1));

        diffs = new ArrayList<>();
        String[] diffStrs = parts[1].split(",\n");
        for (String diffStr : diffStrs) {
            if(diffStr.isEmpty()){
                continue;
            }
            diffs.add(new Diff(diffStr));
        }

        this.docLength = Integer.parseInt(parts[2]);

        this.simplify();
    }

    public Patch clone(){
        return new Patch(baseVersion, diffs, docLength);
    }

    public List<Diff> getDiffs() {
        return diffs;
    }

    public long getBaseVersion() {
        return baseVersion;
    }

    public void setBaseVersion(long baseVersion) {
        this.baseVersion = baseVersion;
    }

    public int getDocLength() {
        return docLength;
    }

    public Patch simplify() {
        if (this.diffs.size() != 0) {
            List<Diff> result = new ArrayList<>();
            result.add(this.diffs.get(0).clone());

            for (int i = 1, j = 0; i < this.diffs.size(); i++) {
                if (this.diffs.get(i) == null) {
                    break;
                }
                Diff curr = this.diffs.get(i);
                Diff prev = result.get(j);

                if (!curr.isInsertion() && !prev.isInsertion() && prev.getStartIndex() + prev.getLength() == curr.getStartIndex()) {
                    prev.setChanges(prev.getChanges() + curr.getChanges());
                } else if (curr.isInsertion() && prev.isInsertion() && prev.getStartIndex() == curr.getStartIndex()) {
                    prev.setChanges(prev.getChanges() + curr.getChanges());
                } else {
                    j++;
                    result.add(curr.clone());
                }
            }

            this.diffs = result;
        }

        return this;
    }

    public Patch convertToCRLF(String base) {
        List<Diff> CRLFDiffs = new ArrayList<>();
        for (Diff diff : diffs) {
            CRLFDiffs.add(diff.convertToCRLF(base));
        }
        return new Patch(baseVersion, CRLFDiffs, base.replace("\n", "\r\n").length());
    }

    public Patch convertToLF(String base) {
        List<Diff> LFDiffs = new ArrayList<>();
        for (Diff diff : diffs) {
            LFDiffs.add(diff.convertToLF(base));
        }
        return new Patch(baseVersion, LFDiffs, base.replace("\r\n", "\n").length());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("v");
        sb.append(baseVersion);

        sb.append(":\n");
        for (Diff diff : diffs) {
            sb.append(diff);
            sb.append(",\n");
        }

        if (!diffs.isEmpty()){
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(":\n");
        sb.append(this.docLength);

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patch patch = (Patch) o;

        if (baseVersion != patch.baseVersion) return false;
        if (docLength != patch.docLength) return false;
        return diffs != null ? diffs.equals(patch.diffs) : patch.diffs == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (baseVersion ^ (baseVersion >>> 32));
        result = 31 * result + (diffs != null ? diffs.hashCode() : 0);
        result = 31 * result + docLength;
        return result;
    }

    public Patch transform(boolean othersHavePrecedence, List<Patch> patches) {
        return transform(othersHavePrecedence, patches.toArray(new Patch[patches.size()]));
    }

    public Patch transform(boolean othersHavePrecedence, Patch... patches) {
        List<Diff> intermediateDiffs = diffs;
        long maxVersionSeen = baseVersion-1;

        for (Patch patch : patches) {
            // Must be able to transform backwards as well?
            List<Diff> newIntermediateDiffs = new ArrayList<>();

            for (Diff diff : intermediateDiffs) {
                newIntermediateDiffs.addAll(diff.transform(othersHavePrecedence, patch.diffs));
            }

            intermediateDiffs = newIntermediateDiffs;
            maxVersionSeen = Math.max(patch.baseVersion, maxVersionSeen);
        }

        int newDocLen = this.docLength;
        for (Patch patch : patches){
            for(Diff diff : patch.getDiffs()){
                if (diff.isInsertion()){
                    newDocLen += diff.getLength();
                } else {
                    newDocLen -= diff.getLength();
                }
            }
        }

        return new Patch(maxVersionSeen+1, intermediateDiffs, newDocLen);
    }
}
