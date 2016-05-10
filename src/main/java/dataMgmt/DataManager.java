package dataMgmt;

import patching.PatchManager;

/**
 * Created by Benedict on 5/9/2016.
 */
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
    private MetadataManager metadataManager;
    private SessionStorage sessionStorage;

    private DataManager() {
//        this.patchManager = new PatchManager();
//        this.fileContentWriter = new FileContentWriter(patchManager);
//        this.metadataManager = new MetadataManager();
//        this.sessionStorage = new SessionStorage();
    }

    public FileContentWriter getFileContentWriter() {
        if(fileContentWriter == null){
            this.fileContentWriter = new FileContentWriter(getPatchManager());
        }
        return fileContentWriter;
    }

    public MetadataManager getMetadataManager() {
        if(metadataManager == null){
            this.metadataManager = new MetadataManager();
        }
        return metadataManager;
    }

    public PatchManager getPatchManager() {
        if(patchManager == null){
            this.patchManager = new PatchManager();
        }
        return patchManager;
    }

    public SessionStorage getSessionStorage() {
        if(sessionStorage == null){
            this.sessionStorage = new SessionStorage();
        }
        return sessionStorage;
    }

    public void setFileContentWriter(FileContentWriter fileContentWriter) {
        this.fileContentWriter = fileContentWriter;
    }

    public void setMetadataManager(MetadataManager metadataManager) {
        this.metadataManager = metadataManager;
    }

    public void setPatchManager(PatchManager patchManager) {
        this.patchManager = patchManager;
    }

    public void setSessionStorage(SessionStorage sessionStorage) {
        this.sessionStorage = sessionStorage;
    }
}
