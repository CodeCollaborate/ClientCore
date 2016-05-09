package patching;

import java.util.List;

/**
 * Created by fahslaj on 5/5/2016.
 */
public class PatchManager {

    private static PatchManager instance;

    /**
     * Get the active instance of the SessionStorage class.
     * @return the instance of the SessionStorage class
     */
    public static PatchManager getInstance() {
        if (instance == null) {
            synchronized (PatchManager.class) {
                if (instance == null) {
                    instance = new PatchManager();
                }
            }
        }
        return instance;
    }

    public String applyPatch(String content, List<Patch> patches) {
        return ""; // TODO: Implement
    }
}
