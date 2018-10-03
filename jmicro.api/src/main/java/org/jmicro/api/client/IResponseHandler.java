package org.jmicro.api.client;

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;
import org.jmicro.api.server.ServerError;

public interface IResponseHandler {
	public void onResponse(IResponse resp,IRequest req,ServerError error);
}
