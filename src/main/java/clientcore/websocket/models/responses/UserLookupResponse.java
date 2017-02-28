package clientcore.websocket.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.IResponseData;
import clientcore.websocket.models.User;

/**
 * Created by Benedict on 9/3/2016.
 */
public class UserLookupResponse implements IResponseData {
    @JsonProperty("Users")
    public final User[] users;

    public UserLookupResponse(@JsonProperty("Users") User[] users) {
        this.users = users;
    }
}
