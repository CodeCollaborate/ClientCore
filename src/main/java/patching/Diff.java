package patching;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Benedict Wong on 5/8/2016.
 */

public class Diff {
    private final boolean insertion;
    private final int startIndex;
    private final String changes;

    public Diff(Diff diff) {
        this.insertion = diff.insertion;
        this.startIndex = diff.startIndex;
        this.changes = diff.changes;
    }

    public Diff(boolean insertion, int startIndex, String changes) {
        this.insertion = insertion;
        this.startIndex = startIndex;
        this.changes = changes;
    }

    public Diff(String str) {

        if (!str.matches("\\d+:(\\+|-)\\d+:.+")) {
            throw new IllegalArgumentException("Illegal patch format; should be %d:-%d:%s or %d:+%d:%s");
        }

        String[] parts = str.split(":");

        try {
            this.startIndex = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid offset: " + parts[0], e);
        }

        switch (parts[1].charAt(0)) {
            case '+':
                this.insertion = true;
                break;
            case '-':
                this.insertion = false;
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + parts[1].charAt(0));
        }

        int length = Integer.parseInt(parts[1].substring(1));

        try {
            this.changes = URLDecoder.decode(parts[2], "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Invalid encoding type provided: UTF-8", e);
        }

        if (this.changes.length() != length) {
            throw new IllegalArgumentException(
                    String.format("Length does not match length of change: %d != %s", length, this.changes));
        }
    }

    public boolean isInsertion() {
        return insertion;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public Diff getOffsetDiff(int offset) {
        return new Diff(this.insertion, this.startIndex + offset, this.changes);
    }

    public String getChanges() {
        return changes;
    }

    public int getLength() {
        return this.changes.length();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changes == null) ? 0 : changes.hashCode());
        result = prime * result + (insertion ? 1231 : 1237);
        result = prime * result + startIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Diff other = (Diff) obj;
        if (changes == null) {
            if (other.changes != null)
                return false;
        } else if (!changes.equals(other.changes))
            return false;
        if (insertion != other.insertion)
            return false;
        if (startIndex != other.startIndex)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(startIndex);
        sb.append(":");
        sb.append(insertion ? "+" : "-");
        sb.append(changes.length());
        sb.append(":");
        try {
            sb.append(URLEncoder.encode(changes, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Invalid encoding type provided: UTF-8", e);
        }

        return sb.toString();
    }

    public Diff convertToCRLF(String base) {
        int newStartIndex = this.startIndex;
        String newChanges = this.changes.replace("\n", "\r\n");

        for (int i = 0; i < newStartIndex && i < base.length() - 1; i++) {
            if (base.charAt(i) == '\r' && base.charAt(i + 1) == '\n') {
                newStartIndex++;
            }
        }

        return new Diff(this.insertion, newStartIndex, newChanges);
    }

    public Diff convertToLF(String base) {
        int newStartIndex = this.startIndex;
        String newChanges = this.changes.replace("\r\n", "\n");

        for (int i = 0; i < startIndex - 1 && i < base.length() - 1; i++) {
            if (base.charAt(i) == '\r' && base.charAt(i + 1) == '\n') {
                newStartIndex--;
            }
        }

        return new Diff(this.insertion, newStartIndex, newChanges);
    }

    public Diff getUndo() {
        return new Diff(!this.insertion, this.startIndex, this.changes);
    }

    public List<Diff> transform(List<Diff> other) {
        return transform(other.toArray(new Diff[other.size()]));
    }

    public List<Diff> transform(Diff... others) {
        List<Diff> intermediateDiffs = new ArrayList<>();
        intermediateDiffs.add(this);

        for (Diff other : others) {
            List<Diff> newIntermediateDiffs = new ArrayList<>();
            for (Diff intermediate : intermediateDiffs) {

                // CASE 1: IndexA < IndexB
                if (other.startIndex < intermediate.startIndex) {
                    // CASE 1a: Ins - Ins
                    if (other.insertion && intermediate.insertion) {
                        int newStartLoc = intermediate.startIndex + other.getLength();
                        Diff newDiff = new Diff(intermediate.insertion, newStartLoc, intermediate.changes);
                        newIntermediateDiffs.add(newDiff);
                    }
                    // CASE 1b: Ins - Rmv
                    else if (other.insertion && !intermediate.insertion) {
                        int newStartLoc = intermediate.startIndex + other.getLength();
                        Diff newDiff = new Diff(intermediate.insertion, newStartLoc, intermediate.changes);
                        newIntermediateDiffs.add(newDiff);

                    }
                    // CASE 1c: Rmv - Ins
                    else if (!other.insertion && intermediate.insertion) {
                        if ((other.startIndex + other.getLength()) > intermediate.startIndex) {
                            int newStartLoc = intermediate.startIndex - (intermediate.startIndex - other.startIndex);
                            Diff newDiff = new Diff(intermediate.insertion, newStartLoc, intermediate.changes);
                            newIntermediateDiffs.add(newDiff);
                        } else {
                            int newStartLoc = intermediate.startIndex - other.getLength();
                            Diff newDiff = new Diff(intermediate.insertion, newStartLoc, intermediate.changes);
                            newIntermediateDiffs.add(newDiff);
                        }

                    }
                    // CASE 1d: Rmv - Rmv
                    else if (!other.insertion && !intermediate.insertion) {
                        if ((other.startIndex + other.getLength()) <= intermediate.startIndex) {
                            int newStartLoc = intermediate.startIndex - other.getLength();
                            Diff newDiff = new Diff(intermediate.insertion, newStartLoc, intermediate.changes);
                            newIntermediateDiffs.add(newDiff);
                        } else if ((other.startIndex + other.getLength()) >= (intermediate.startIndex
                                + intermediate.getLength())) {
                            // Do nothing
                        } else {
                            int overlap = other.startIndex + other.getLength() - intermediate.startIndex;
                            int newStartLoc = intermediate.startIndex - other.getLength() + overlap;
                            String newChanges = intermediate.changes.substring(overlap);
                            Diff newDiff = new Diff(intermediate.insertion, newStartLoc, newChanges);
                            newIntermediateDiffs.add(newDiff);
                        }
                    }
                    // FAIL: Should never have been able to get here.
                    else {
                        throw new IllegalStateException("Got to invalid state while transforming [" + this.toString()
                                + "] on predessors [" + others + "]");
                    }
                }
                // CASE 2: IndexA = IndexB
                else if (other.startIndex == intermediate.startIndex) {
                    // CASE 2a: Ins - Ins
                    if (other.insertion && intermediate.insertion) {
                        int newStartLoc = intermediate.startIndex + other.getLength();
                        Diff newDiff = new Diff(intermediate.insertion, newStartLoc, intermediate.changes);
                        newIntermediateDiffs.add(newDiff);
                    }
                    // CASE 2b: Ins - Rmv
                    else if (other.insertion && !intermediate.insertion) {
                        int newStartLoc = intermediate.startIndex + other.getLength();
                        Diff newDiff = new Diff(intermediate.insertion, newStartLoc, intermediate.changes);
                        newIntermediateDiffs.add(newDiff);
                    }
                    // CASE 2c: Rmv - Ins
                    else if (!other.insertion && intermediate.insertion) {
                        newIntermediateDiffs.add(intermediate);
                    }
                    // CASE 2d: Rmv - Rmv
                    else if (!other.insertion && !intermediate.insertion) {
                        if (intermediate.getLength() > other.getLength()) {
                            String newChanges = intermediate.changes.substring(other.getLength());
                            Diff newDiff = new Diff(intermediate.insertion, intermediate.startIndex, newChanges);
                            newIntermediateDiffs.add(newDiff);
                        }
                    }
                    // FAIL: Should never have been able to get here.
                    else {
                        throw new IllegalStateException("Got to invalid state while transforming [" + this.toString()
                                + "] on predessors [" + others + "]");
                    }
                }
                // CASE 3: IndexA = IndexB
                else if (other.startIndex > intermediate.startIndex) {
                    // CASE 3a: Ins - Ins
                    if (other.insertion && intermediate.insertion) {
                        newIntermediateDiffs.add(intermediate);
                    }
                    // CASE 3b: Ins - Rmv
                    else if (other.insertion && !intermediate.insertion) {

                        if ((intermediate.startIndex + intermediate.getLength()) > other.startIndex) {
                            int length1 = other.startIndex - intermediate.startIndex;
                            String changes1 = intermediate.changes.substring(0, length1);
                            String changes2 = intermediate.changes.substring(length1);
                            Diff diff1 = new Diff(intermediate.insertion, intermediate.startIndex, changes1);
                            Diff diff2 = new Diff(intermediate.insertion, intermediate.startIndex + other.getLength(),
                                    changes2);

                            newIntermediateDiffs.add(diff1);
                            newIntermediateDiffs.add(diff2);
                        } else {
                            newIntermediateDiffs.add(intermediate);
                        }
                    }
                    // CASE 3c: Rmv - Ins
                    else if (!other.insertion && intermediate.insertion) {
                        newIntermediateDiffs.add(intermediate);
                    }
                    // CASE 3d: Rmv - Rmv
                    else if (!other.insertion && !intermediate.insertion) {
                        if ((intermediate.startIndex + intermediate.getLength()) > other.startIndex) {
                            int nonOverlap = other.startIndex - intermediate.startIndex;
                            String newChanges = intermediate.changes.substring(0,
                                    intermediate.getLength() - nonOverlap);
                            Diff newDiff = new Diff(intermediate.insertion, intermediate.startIndex, newChanges);
                            newIntermediateDiffs.add(newDiff);
                        } else {
                            newIntermediateDiffs.add(intermediate);
                        }
                    }
                    // FAIL: Should never have been able to get here.
                    else {
                        throw new IllegalStateException("Got to invalid state while transforming [" + this.toString()
                                + "] on predessors [" + others + "]");
                    }

                } else {
                    throw new IllegalStateException("Got to invalid state");
                }
            }
            intermediateDiffs = newIntermediateDiffs;
        }
        return intermediateDiffs;
    }
}
