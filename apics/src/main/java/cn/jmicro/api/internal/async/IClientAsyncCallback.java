package cn.jmicro.api.internal.async;

import cn.jmicro.api.RespJRso;

public interface IClientAsyncCallback {

	void onResponse(RespJRso resp);
	
}
