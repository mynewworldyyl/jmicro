package org.jmicro.example.pubsub.impl;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.common.Constants;
import org.jmicro.example.api.rpc.IAsyncRpcCallback;

@Service(namespace="asyncRpcCallback",version="0.0.1", baseTimeUnit=Constants.TIME_SECONDS)
@Component
public class AsyncRpcCallbackImpl implements IAsyncRpcCallback {

	@SMethod
	public void callback(String name) {
		System.out.println("Got async callback:"+name);
	}

}
