package cn.jmicro.api.async;

import cn.jmicro.api.client.IAsyncCallback;

public interface IPromise<R> {
	
	public void then(IAsyncCallback<R> callback);
	
	public R getResult();
}
