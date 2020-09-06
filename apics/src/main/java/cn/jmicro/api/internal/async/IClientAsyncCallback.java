package cn.jmicro.api.internal.async;

import cn.jmicro.api.net.IResponse;

public interface IClientAsyncCallback {

	void onResponse(IResponse resp);
	
}
