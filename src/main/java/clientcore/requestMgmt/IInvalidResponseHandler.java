package clientcore.requestMgmt;

/**
 * Created by fahslaj on 10/15/2016.
 */
public interface IInvalidResponseHandler {
    public void handleInvalidResponse(int errorCode, String message);
}
