package patching;

import java.util.List;

/**
 * Created by fahslaj on 5/5/2016.
 */
public class PatchManager {

    public String applyPatch(String content, List<Patch> patches) {

        String newContent = content;

        boolean useCRLF = newContent.contains("\r\n");

        for (Patch patch : patches) {
            if (useCRLF) {
                patch.convertToCRLF(newContent);
            }
            for (Diff diff : patch.getDiffs()) {
                if (diff.getStartIndex() > 0 && diff.getStartIndex() < newContent.length()
                        && newContent.charAt(diff.getStartIndex() - 1) == '\r'
                        && newContent.charAt(diff.getStartIndex()) == '\n') {
                    throw new IllegalArgumentException("Tried to insert between \\r and \\n");
                }

                if (diff.isInsertion()) {
                    StringBuilder sb = new StringBuilder();

                    sb.append(newContent.substring(0, diff.getStartIndex()));
                    sb.append(diff.getChanges());
                    sb.append(newContent.substring(diff.getStartIndex()));

                    newContent = sb.toString();
                } else {
                    StringBuilder sb = new StringBuilder();

                    sb.append(newContent.substring(0, diff.getStartIndex()));
                    sb.append(newContent.substring(diff.getStartIndex() + diff.getLength()));

                    newContent = sb.toString();
                }
            }
        }

        return newContent;
    }
}
