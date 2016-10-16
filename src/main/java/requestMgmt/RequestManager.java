package requestMgmt;

import dataMgmt.DataManager;
import websocket.IRequestSendErrorHandler;
import websocket.WSManager;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectLookupRequest;
import websocket.models.requests.UserProjectsRequest;
import websocket.models.responses.ProjectLookupResponse;
import websocket.models.responses.UserProjectsResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fahslaj on 10/15/2016.
 */
public class RequestManager {

    private DataManager dataManager;
    private WSManager wsManager;

    private IRequestSendErrorHandler requestSendErrorHandler;
    private IInvalidResponseHandler invalidResponseHandler;

    public RequestManager(DataManager dataManager, WSManager wsManager) {
        this.dataManager = dataManager;
        this.wsManager = wsManager;
    }

    public void loginAndSubscribe() {

    }

    public void fetchProjects() {
        Request getProjectsRequest = new UserProjectsRequest().getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                List<Project> projects = Arrays.asList(((UserProjectsResponse) response.getData()).getProjects());
                sendProjectsLookupRequest(projects);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Error fetching projects");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(getProjectsRequest);
    }

    private void sendProjectsLookupRequest(List<Project> projects) {
        List<Long> ids = new ArrayList<>();
        projects.forEach((project) -> ids.add(project.getProjectID()));
        Request getProjectDetails = new ProjectLookupRequest(ids).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                List<Project> lookedUpProjects = Arrays.asList(((ProjectLookupResponse) response.getData()).getProjects());
                this.dataManager.getSessionStorage().setProjects(lookedUpProjects);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Error fetching projects' details");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(getProjectDetails);
    }

    public void setRequestSendErrorHandler(IRequestSendErrorHandler handler) {
        this.requestSendErrorHandler = handler;
    }

    public void setInvalidResponseHandler(IInvalidResponseHandler handler) {
        this.invalidResponseHandler = handler;
    }
}
