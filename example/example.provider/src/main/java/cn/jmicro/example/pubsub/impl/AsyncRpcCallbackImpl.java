package cn.jmicro.example.pubsub.impl;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.rpc.IAsyncRpcCallback;

@Service(namespace="asyncRpcCallback",version="0.0.1", baseTimeUnit=Constants.TIME_SECONDS, clientId=1000)
@Component
public class AsyncRpcCallbackImpl implements IAsyncRpcCallback {

	@SMethod
	public void callback(String name) {
		System.out.println("Got async callback:"+name);
	}

}
