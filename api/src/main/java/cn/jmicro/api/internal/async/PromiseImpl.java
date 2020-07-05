package cn.jmicro.api.internal.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.client.IAsyncCallback;

public class PromiseImpl<R> implements IPromise<R>{

	private final static Logger logger = LoggerFactory.getLogger(PromiseImpl.class);
	
	private IAsyncCallback<R> callback = null;
	
	private R result;
	
	private boolean done = false;
	
	private  AsyncFailResult fail;
	
	private Object locker = new Object();
	
	@Override
	public void then(IAsyncCallback<R> callback) {
		if(this.callback != null) {
			logger.error("callback have been set: " + callback.getClass().getName());
			return;
		}
		
		this.callback = callback;
		if(done) {
			callback.onMessage(result, fail);
		}
	}
	
	public void done() {
		
		synchronized(locker) {
			locker.notifyAll();
		}
		
		if(done) {
			logger.error("callback have been call: " + callback.getClass().getName());
			return;
		}
		done = true;
		if(callback != null) {
			callback.onMessage(result, fail);
		} 
	}

	public R getResult() {
		if(!done) {
			synchronized(locker) {
				try {
					this.locker.wait();
				} catch (InterruptedException e) {
					logger.error("getResult",e);
				}
			}
		}
		return result;
	}

	public void setResult(R result) {
		this.result = result;
	}

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public AsyncFailResult getFail() {
		return fail;
	}

	public void setFail(AsyncFailResult fail) {
		this.fail = fail;
	}

}
