package clientcore.websocket.models.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;
import clientcore.websocket.models.IRequestData;
import clientcore.websocket.models.Request;

import java.util.Arrays;
import java.util.List;

/**
 * Created by loganga on 5/9/2016
 */
public class ProjectLookupRequest implements IRequestData {

    @JsonProperty("ProjectIDs")
    private List<Long> projectIDs;

    public ProjectLookupRequest(List<Long> projectIDs) {
        this.projectIDs = projectIDs;
    }

    public ProjectLookupRequest(Long[] projectIDs) {
        this.projectIDs = Arrays.asList(projectIDs);
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("Project", "Lookup", this, responseHandler, requestSendErrorHandler);
    }

    public List<Long> getProjectIDs() {
        return projectIDs;
    }
}
