package cn.jmicro.api.client;

public interface IAsyncFailCallback {
	void fail(int code,String errorMsg,Object context);
}
