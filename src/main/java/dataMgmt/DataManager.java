package dataMgmt;

import patching.PatchManager;

/**
 * The facade that contains the active PatchManager, FileContentWriter, and SessionStorage.
 * Created by Benedict on 5/9/2016.
 */
@Deprecated
public class DataManager {

    private static DataManager instance;

    /**
     * Get the active instance of the FileContentWriter class.
     *
     * @return the instance of the FileContentWriter class
     */
    public static DataManager getInstance() {
        if (instance == null) {
            synchronized (DataManager.class) {
                if (instance == null) {
                    instance = new DataManager();
                }
            }
        }
        return instance;
    }

    private PatchManager patchManager;
    private FileContentWriter fileContentWriter;
    private SessionStorage sessionStorage;

    /**
     * Get the active FileContentWriter
     * @return the active FileContentWriter
     */
    public FileContentWriter getFileContentWriter() {
        if(fileContentWriter == null){
            this.fileContentWriter = new FileContentWriter(getPatchManager());
        }
        return fileContentWriter;
    }

    /**
     * Get the active PatchManager
     * @return the active PatchManager
     */
    public PatchManager getPatchManager() {
        if(patchManager == null){
            this.patchManager = new PatchManager();
        }
        return patchManager;
    }

    /**
     * Get the active SessionStorage
     * @return the active SessionStorage
     */
    public SessionStorage getSessionStorage() {
        if(sessionStorage == null){
            this.sessionStorage = new SessionStorage();
        }
        return sessionStorage;
    }

    /**
     * Set the active FileContentWriter
     * @param fileContentWriter the new active FileContentWriter
     */
    public void setFileContentWriter(FileContentWriter fileContentWriter) {
        this.fileContentWriter = fileContentWriter;
    }

    /**
     * Set the active PatchManager
     * @param patchManager the new active PatchManager
     */
    public void setPatchManager(PatchManager patchManager) {
        this.patchManager = patchManager;
    }

    /**
     * Set the active SessionStorage
     * @param sessionStorage the new active SessionStorage
     */
    public void setSessionStorage(SessionStorage sessionStorage) {
        this.sessionStorage = sessionStorage;
    }
}
