package cn.jmicro.api.async;

import java.lang.reflect.Type;

import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.client.IAsyncFailCallback;
import cn.jmicro.api.client.IAsyncSuccessCallback;

public interface IPromise<R> {
	
	public static final int ASYNC_CALL_RPC = 1;
	
	public IPromise<R> then(IAsyncCallback<R> callback);
	
	public R getResult();
	
	public Type resultType();
	
	public IPromise<R> success(IAsyncSuccessCallback<R> cb);
	
	public IPromise<R> fail(IAsyncFailCallback cb);
	
	public <T> T getContext();
	
	public <T> void setContext(T context);
	
	public AsyncFailResult getFailResult();
	
	public String getFailMsg();
	
	public int getFailCode();
	
	public boolean isSuccess();
	
	void setCounter(int cnt);
	
	boolean decCounter(int cnt,boolean doDone);
	
	boolean counterFinish();
	
}
