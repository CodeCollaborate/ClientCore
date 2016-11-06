package requestMgmt;

import com.google.common.collect.BiMap;
import constants.CoreStringConstants;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import websocket.ConnectException;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.WSManager;
import websocket.models.File;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.*;
import websocket.models.responses.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by fahslaj on 10/15/2016.
 */
public abstract class RequestManager {

    private DataManager dataManager;
    private WSManager wsManager;

    private IRequestSendErrorHandler requestSendErrorHandler;
    private IInvalidResponseHandler invalidResponseHandler;

    public RequestManager(DataManager dataManager, WSManager wsManager,
                          IRequestSendErrorHandler requestSendErrorHandler,
                          IInvalidResponseHandler invalidResponseHandler) {
        this.dataManager = dataManager;
        this.wsManager = wsManager;
        this.requestSendErrorHandler = requestSendErrorHandler;
        this.invalidResponseHandler = invalidResponseHandler;
    }

    /**
     * Login to the CodeCollaborate server.
     * @param username username to log in with
     * @param password password for the given username
     */
    public void login(String username, String password) {
        Request loginRequest = new UserLoginRequest(username, password).getRequest(response -> {
            UserLoginResponse loginResponse = (UserLoginResponse) response.getData();
            int status = response.getStatus();
            if (status == 200) {
                SessionStorage storage = this.dataManager.getSessionStorage();
                storage.setUsername(username);
                storage.setAuthenticationToken(loginResponse.getToken());
                this.wsManager.setAuthInfo(username, loginResponse.getToken());
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Could not login to the CodeCollaborate server.");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendRequest(loginRequest);
    }

    /**
     * Logout of the CodeCollaborate server.
     */
    public void logout() {
        this.dataManager.getSessionStorage().setUsername(null);
        this.wsManager.setAuthInfo(null, null);
    }

    /**
     * Fetch all of the projects the current user has permissions for.
     */
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

    /**
     * Subscribe to the project with the given id
     * @param id id of the project to subscribe to
     */
    public void subscribeToProject(long id) {
        Request subscribeRequest = (new ProjectSubscribeRequest(id)).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                dataManager.getSessionStorage().setSubscribed(id);
                Request requestForFiles = (new ProjectGetFilesRequest(id)).getRequest(response2 -> {
                    int status2 = response2.getStatus();
                    if (status2 == 200) {
                        ProjectGetFilesResponse r = (ProjectGetFilesResponse) response2.getData();
                        finishSubscribeToProject(id, r.files);
                    } else {
                        this.invalidResponseHandler.handleInvalidResponse(status, "Error getting project files: " + id);
                    }
                }, this.requestSendErrorHandler);
                wsManager.sendAuthenticatedRequest(requestForFiles);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Error subscribing to project: " + id);
            }
        }, this.requestSendErrorHandler);
        wsManager.sendAuthenticatedRequest(subscribeRequest);
    }

    /**
     * Finish the subscription action by using the project's files from the server and creating metadata.
     * @param id id of the project subscribed to
     * @param files files from the server
     */
    public abstract void finishSubscribeToProject(long id, File[] files);

    /**
     * Unsubscribe from the project with the given id
     * @param id id of the project to unsubscribe from
     */
    public void unsubscribeFromProject(long id) {
        Request request = (new ProjectUnsubscribeRequest(id)).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                dataManager.getSessionStorage().setUnsubscribed(id);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to unsubscribe from project: " + id);
            }
        }, this.requestSendErrorHandler);
        wsManager.sendAuthenticatedRequest(request);
    }

    /**
     * Fetch all projects and subscribe to the set of projects with the given ids.
     * @param ids the ids of the projects to subscribe to
     */
    public void fetchAndSubscribeAll(List<Long> ids) {
        Request getProjectsRequest = new UserProjectsRequest().getRequest(response2 -> {
            int status2 = response2.getStatus();
            if (status2 == 200) {
                List<Project> projects =
                        Arrays.asList(((UserProjectsResponse) response2.getData()).getProjects());
                sendProjectsLookupRequest(projects);
                for (long id : ids) {
                    subscribeToProject(id);
                }
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status2, "Error fetching projects");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(getProjectsRequest);
    }

    /**
     * Create a project with the given name
     * @param projectName name of the project to create
     */
    public void createProject(String projectName) {
        Request request = (new ProjectCreateRequest(projectName)).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                long pid = ((ProjectCreateResponse) response.getData()).getProjectID();
                Request request2 = (new ProjectSubscribeRequest(pid)).getRequest(response2 -> {
                    int status2 = response2.getStatus();
                    if (status2 == 200) {
                        Long[] ids = {pid};
                        Request request3 = (new ProjectLookupRequest(ids)).getRequest(response3 -> {
                            int status3 = response3.getStatus();
                            if (status3 == 200) {
                                Project project = ((ProjectLookupResponse) response3.getData()).getProjects()[0];
                                dataManager.getSessionStorage().setProject(project);
                                dataManager.getSessionStorage().setSubscribed(project.getProjectID());
                                finishCreateProject(project);
                            } else {
                                this.invalidResponseHandler.handleInvalidResponse(status3,
                                        "Failed to lookup project: " + pid);
                            }
                        }, requestSendErrorHandler);
                        wsManager.sendAuthenticatedRequest(request3);
                    } else {
                        this.invalidResponseHandler.handleInvalidResponse(status2,
                                "Failed to subscribe to project: " + pid);
                    }
                }, requestSendErrorHandler);
                wsManager.sendAuthenticatedRequest(request2);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to create project: " + projectName);
            }
        }, requestSendErrorHandler);
        wsManager.sendAuthenticatedRequest(request);
    }

    /**
     * Finish creating a project by storing metadata and individually creating files
     * @param project
     */
    public abstract void finishCreateProject(Project project);

    /**
     * Delete the project with the given id
     * @param id id of the project to delete
     */
    public void deleteProject(long id) {
        Request request = (new ProjectDeleteRequest(id)).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                dataManager.getSessionStorage().setUnsubscribed(id);
                dataManager.getSessionStorage().removeProjectById(id);
                dataManager.getMetadataManager().projectDeleted(id);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to delete project: " + id);
            }
        },  requestSendErrorHandler);
        wsManager.sendAuthenticatedRequest(request);
    }

    /**
     * Add a user to a project with the given permission level
     * @param id id of the project to add
     * @param username username of the user to add
     * @param permissionLevel level of permission to add
     */
    public void addUserToProject(long id, String username, int permissionLevel) {
        Request request = (new ProjectGrantPermissionsRequest(id, username, permissionLevel)).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                Project project = dataManager.getSessionStorage().getProjectById(id);
                project.getPermissions().put(username, new Permission(username, permissionLevel, null, null));
                dataManager.getSessionStorage().setProject(project);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to add user to project: " + id);
            }
        }, requestSendErrorHandler);
        wsManager.sendAuthenticatedRequest(request);
    }

    /**
     * Remove a user from a project
     * @param id id of the project
     * @param username username of the user to remove
     */
    public void removeUserFromProject(long id, String username) {
        Request request = (new ProjectRevokePermissionsRequest(id, username)).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                Project project = dataManager.getSessionStorage().getProjectById(id);
                project.getPermissions().remove(id);
                dataManager.getSessionStorage().setProject(project);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to remove user from project: " + id);
            }
        }, requestSendErrorHandler);
        wsManager.sendAuthenticatedRequest(request);
    }

    /**
     * Remove the logged in user from a project
     * @param id id of the project to remove from
     */
    public void removeSelfFromProject(long id) {
        String username = dataManager.getSessionStorage().getUsername();
        Request request = (new ProjectRevokePermissionsRequest(id, username)).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                dataManager.getSessionStorage().setUnsubscribed(id);
                dataManager.getSessionStorage().removeProjectById(id);
                dataManager.getMetadataManager().projectDeleted(id);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status,
                        "Failed to remove logged in user from project: " + id);
            }
        }, requestSendErrorHandler);
        wsManager.sendAuthenticatedRequest(request);
    }

    /**
     * Fetch permission constants from the server
     */
    public void fetchPermissionConstants() {
        Request getPermyConstants = new ProjectGetPermissionConstantsRequest().getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                BiMap<String, Byte> permyConstants =
                        (((ProjectGetPermissionConstantsResponse) response.getData()).getConstants());
                this.dataManager.getSessionStorage().setPermissionConstants(permyConstants);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Error fetching permission constants");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(getPermyConstants);
    }

    /**
     * Creates the given file on the server.
     * Also creates the metadata for this file.
     *
     * @param name
     * @param fullPath
     * @param relativePath
     * @param projectID
     * @param fileBytes
     */
    public void createFile(String name, String fullPath, String relativePath, long projectID, byte[] fileBytes) {
        Request createFileReq = new FileCreateRequest(name, relativePath, projectID, fileBytes).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                long fileID = ((FileCreateResponse) response.getData()).getFileID();
                FileMetadata fMeta =  new FileMetadata();
                fMeta.setFileID(fileID);
                fMeta.setFilename(name);
                fMeta.setRelativePath(relativePath);
                fMeta.setVersion(0);
                this.dataManager.getMetadataManager().putFileMetadata(fullPath, projectID, fMeta);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to create file \"" + name +
                        "\" on the server.");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(createFileReq);
    }

    /**
     * Renames the given file on the server and changes the corresponding metadata.
     *
     * @param fileID
     * @param newName
     */
    public void renameFile(long fileID, String newName) {
        Request renameFileReq = new FileRenameRequest(fileID, newName).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                FileMetadata fileMD = dataManager.getMetadataManager().getFileMetadata(fileID);
                fileMD.setFilename(newName);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to rename file to \"" + newName +
                        "\" on server.");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(renameFileReq);
    }

    /**
     * Moves the given file to the specified relative path on the server.
     * The full path is needed for the mapping of the file's metadata.
     *
     * @param fileID
     * @param newFullPath
     * @param newRelativePath
     */
    public void moveFile(long fileID, String newFullPath, String newRelativePath) {
        Request moveFileReq = new FileMoveRequest(fileID, newRelativePath).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                dataManager.getMetadataManager().fileMoved(fileID, newFullPath);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to move file on server: " + fileID);
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(moveFileReq);
    }

    /**
     * Deletes the given file on the server and within the metadata.
     *
     * @param fileID
     */
    public void deleteFile(long fileID) {
        Request deleteFileReq = new FileDeleteRequest(fileID).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                this.dataManager.getMetadataManager().fileDeleted(fileID);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to delete file from server: " + status);
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(deleteFileReq);
    }

    /**
     * Renames the given project on the server.
     * Also renames the project in its corresponding metadata file.
     *
     * @param projectID
     * @param newName
     */
    public void renameProject(long projectID, String newName, String newPath) {
        Request renameProjectReq = new ProjectRenameRequest(projectID, newName).getRequest(response -> {
            int status = response.getStatus();
            if (status == 200) {
                ProjectMetadata pMeta = this.dataManager.getMetadataManager().getProjectMetadata(projectID);
                pMeta.setName(newName);
                this.dataManager.getMetadataManager().putProjectMetadata(newPath, pMeta);
            } else {
                this.invalidResponseHandler.handleInvalidResponse(status, "Failed to rename project to \"" + newName +
                        "\" on server.");
            }
        }, requestSendErrorHandler);
        this.wsManager.sendAuthenticatedRequest(renameProjectReq);
    }

    public void sendFileChanges(long fileID, String[] changes, long baseFileVersion) {
        MetadataManager mm = this.dataManager.getMetadataManager();

        FileMetadata fMeta = mm.getFileMetadata(fileID);
        ProjectMetadata pMeta = mm.getProjectMetadata(mm.getProjectIDForFileID(fileID));
        String projRootPath = mm.getProjectLocation(pMeta.getProjectID());

        Request req = getFileChangeRequest(fMeta, changes, response -> {
            fMeta.setVersion(((FileChangeResponse) response.getData()).getFileVersion());
            this.dataManager.getMetadataManager().writeProjectMetadataToFile(pMeta, projRootPath,
                    CoreStringConstants.CONFIG_FILE_NAME);
        }, null, 1);

        try {
            this.wsManager.sendRequest(req);
        } catch (ConnectException e) {
            System.out.println("Failed to send change request.");
            e.printStackTrace();
        }

    }

    private Request getFileChangeRequest(FileMetadata fileMeta, String[] changes, IResponseHandler respHandler,
                                         IRequestSendErrorHandler sendErrHandler, int retryCount) {

        return new FileChangeRequest(fileMeta.getFileID(), changes, fileMeta.getVersion()).getRequest(response -> {

            // If we failed the first time around, update the fileVersion and
            // retry.
            if (response.getStatus() == 409 && retryCount > 0) {
                Request req = getFileChangeRequest(fileMeta, changes, respHandler, sendErrHandler, retryCount - 1);
                try {
                    this.wsManager.sendRequest(req);
                } catch (ConnectException e) {
                    System.out.println("Failed to send change request.");
                    e.printStackTrace();
                }
                return;
            }

            respHandler.handleResponse(response);
        }, sendErrHandler);
    }

    public void setRequestSendErrorHandler(IRequestSendErrorHandler handler) {
        this.requestSendErrorHandler = handler;
    }

    public void setInvalidResponseHandler(IInvalidResponseHandler handler) {
        this.invalidResponseHandler = handler;
    }

    public IRequestSendErrorHandler getRequestSendErrorHandler() {
        return requestSendErrorHandler;
    }

    public IInvalidResponseHandler getInvalidResponseHandler() {
        return invalidResponseHandler;
    }
}
