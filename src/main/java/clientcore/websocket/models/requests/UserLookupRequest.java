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
public class UserLookupRequest implements IRequestData {

    @JsonProperty("Usernames")
    private List<String> usernames;

    public UserLookupRequest(List<String> usernames) {
        this.usernames = usernames;
    }

    public UserLookupRequest(String[] usernames) {
        this.usernames = Arrays.asList(usernames);
    }

    @JsonIgnore
    @Override
    public Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler) {
        return new Request("User", "Lookup", this, responseHandler, requestSendErrorHandler);
    }

    public List<String> getUsernames() {
        return usernames;
    }
}
