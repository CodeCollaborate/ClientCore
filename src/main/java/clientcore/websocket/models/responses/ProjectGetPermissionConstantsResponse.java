package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import clientcore.websocket.models.IResponseData;

import java.util.Map;

/**
 * @author Joel Shapiro on 10/3/16
 */
public class ProjectGetPermissionConstantsResponse implements IResponseData {
    @JsonProperty("Constants")
    public final Map<String, Byte> constants;

    public ProjectGetPermissionConstantsResponse(@JsonProperty("Constants") Map<String, Byte> constants) {
        this.constants = constants;
    }
}
