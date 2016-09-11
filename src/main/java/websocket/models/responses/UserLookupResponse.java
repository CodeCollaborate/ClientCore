package websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import websocket.models.IResponseData;
import websocket.models.User;

/**
 * Created by Benedict on 9/3/2016.
 */
public class UserLookupResponse implements IResponseData {
    @JsonProperty("Users")
    protected User[] users;

    public User[] getUsers() {
        return users;
    }
}
