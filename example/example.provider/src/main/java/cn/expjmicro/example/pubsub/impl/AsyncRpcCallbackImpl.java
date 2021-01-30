package cn.expjmicro.example.pubsub.impl;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.common.Constants;
import cn.expjmicro.example.api.rpc.IAsyncRpcCallback;

@Service(version="0.0.1", baseTimeUnit=Constants.TIME_SECONDS,showFront=false,clientId=Constants.NO_CLIENT_ID)
@Component
public class AsyncRpcCallbackImpl implements IAsyncRpcCallback {

	@SMethod
	public void callback(String name) {
		System.out.println("Got async callback:"+name);
	}

}
