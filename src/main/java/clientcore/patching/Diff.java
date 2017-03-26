package clientcore.patching;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Benedict Wong on 5/8/2016.
 */

public class Diff implements Comparable<Diff> {
    private boolean insertion;
    private int startIndex;
    private String changes;

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

    public Diff clone() {
        return new Diff(insertion, startIndex, changes);
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

    public void setInsertion(boolean insertion) {
        this.insertion = insertion;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public int getLength() {
        return this.changes.length();
    }

    @Override
    public int compareTo(Diff o) {
        if (this.startIndex == o.startIndex) {
            if (this.isInsertion() == o.isInsertion()) {
                return this.changes.compareTo(o.changes);
            } else if (this.insertion && !o.insertion) {
                return -1;
            } else {
                return 1;
            }
        } else {
            return Integer.compare(this.startIndex, o.startIndex);
        }
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
}
