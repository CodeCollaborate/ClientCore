package clientcore.websocket.models;

import clientcore.websocket.IRequestSendErrorHandler;
import clientcore.websocket.IResponseHandler;

/**
 * Created by fahslaj on 4/14/2016.
 */
public interface IRequestData {
	Request getRequest(IResponseHandler responseHandler, IRequestSendErrorHandler requestSendErrorHandler);
}
