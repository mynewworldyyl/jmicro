package cn.jmicro.api.client;

import java.util.Map;

public interface IAsyncFailCallback {
	void fail(int code,String errorMsg,Object context);
}
