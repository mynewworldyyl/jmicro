package cn.jmicro.api.async;

import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.client.IAsyncFailCallback;
import cn.jmicro.api.client.IAsyncSuccessCallback;

public interface IPromise<R> {
	
	public IPromise<R> then(IAsyncCallback<R> callback);
	
	public R getResult();
	
	public IPromise<R> success(IAsyncSuccessCallback<R> cb);
	
	public IPromise<R> fail(IAsyncFailCallback cb);
	
	
}
