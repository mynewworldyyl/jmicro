package cn.jmicro.api.client;

public interface IAsyncSuccessCallback<R> {
	void success(R msg,Object context);
}
