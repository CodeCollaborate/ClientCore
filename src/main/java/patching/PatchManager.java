package patching;

import java.util.List;

/**
 * Created by fahslaj on 5/5/2016.
 */
public class PatchManager {

    public String applyPatch(String content, List<Patch> patches) {
        boolean useCRLF = content.contains("\r\n");

        for (Patch patch : patches) {
            int startIndex = 0;
            StringBuilder sb = new StringBuilder();

            if (useCRLF) {
                patch.convertToCRLF(content);
            }
            for (Diff diff : patch.getDiffs()) {
                if (diff.getStartIndex() > 0 && diff.getStartIndex() < content.length()
                        && content.charAt(diff.getStartIndex() - 1) == '\r'
                        && content.charAt(diff.getStartIndex()) == '\n') {
                    throw new IllegalArgumentException("Tried to insert between \\r and \\n");
                }

                // Copy anything before the changes
                if (startIndex < diff.getStartIndex()) {
                    sb.append(content.substring(startIndex, diff.getStartIndex()));
                }

                if (diff.isInsertion()) {
                    // insert item
                    sb.append(diff.getChanges());

                    // If the diff's startIndex is greater, move it up.
                    // Otherwise, a previous delete may have deleted over the start index.
                    if (startIndex < diff.getStartIndex()) {
                        startIndex = diff.getStartIndex();
                    }
                } else {
                    // validate that we're deleting the right characters
                    if (!diff.getChanges().equals(content.substring(diff.getStartIndex(), diff.getStartIndex() + diff.getLength()))) {
                        throw new IllegalStateException(
                                String.format("PatchManager.ApplyText: Deleted text %s does not match changes in diff: %s",
                                        content.substring(diff.getStartIndex(), diff.getStartIndex() + diff.getLength()), diff.getChanges()));
                    }

                    // shift the start index of the next round
                    startIndex = diff.getStartIndex() + diff.getLength();
                }
            }

            sb.append(content.substring(startIndex));
            content = sb.toString();
        }

        return content;
    }
}
