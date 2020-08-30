package cn.jmicro.api.client;

import java.util.Map;

public interface IAsyncSuccessCallback<R> {
	void success(R msg,Map<String,Object> context);
}
