package dataMgmt;

import patching.Patch;

/**
 * Created by fahslaj on 5/7/2016.
 */
public interface IFileWritingQueue {

    boolean offerPatch(Patch patches, String absolutePath);
    boolean offerPatch(Patch[] patches, String absolutePath);
}
