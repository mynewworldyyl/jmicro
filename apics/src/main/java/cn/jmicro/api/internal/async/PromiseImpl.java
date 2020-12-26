package cn.jmicro.api.internal.async;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.client.IAsyncFailCallback;
import cn.jmicro.api.client.IAsyncSuccessCallback;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.common.CommonException;

public class PromiseImpl<R> implements IPromise<R>{

	private final static Logger logger = LoggerFactory.getLogger(PromiseImpl.class);
	
	private IAsyncCallback<R>[] callbacks ;
	
	private SuccessCallback<R> custCb = null;
	
	private R result;
	
	private boolean done = false;
	
	private  AsyncFailResult fail;
	
	private Object locker = new Object();
	
	private Object context = null;
	
	private int timeout = 30000;
	
	private AtomicInteger ai = null;
	
	public PromiseImpl() {
		this.callbacks = new IAsyncCallback[1];
	}
	
	public PromiseImpl(R defResult) {
		this();
		this.setResult(defResult);
	}
	
	@Override
	public IPromise<R> then(IAsyncCallback<R> callback) {
		if(done) {
			callback.onResult(result, fail,context);
		} else {
			addCallback(callback);
		}
		return this;
	}

	private void addCallback(IAsyncCallback<R> cb) {
		if(this.callbacks[0] == null) {
			this.callbacks[0] = cb;
			return;
		}
		IAsyncCallback<R>[] arr = new IAsyncCallback[1+this.callbacks.length];
		System.arraycopy(this.callbacks, 0, arr, 0, this.callbacks.length);
		arr[arr.length-1] = cb;
		this.callbacks = arr;
	}

	@Override
	public IPromise<R> success(IAsyncSuccessCallback<R> cb) {
		if(done && this.fail == null) {
			cb.success(result,context);
		} else {
			if(custCb == null) {
				custCb = new SuccessCallback<R>();
				addCallback(custCb);
			}
			custCb.add(cb);
		}
		return this;
	}

	@Override
	public IPromise<R> fail(IAsyncFailCallback cb) {
		if(done && this.fail != null) {
			cb.fail(fail.getCode(),fail.getMsg(),context);
		} else {
			if(custCb == null) {
				custCb = new SuccessCallback<R>();
				addCallback(custCb);
			}
			custCb.add(cb);
		}
		return this;
	}

	public void done() {
		
		synchronized(locker) {
			locker.notifyAll();
		}
		
		if(done) {
			logger.error("callback have been call: ");
			return;
		}
		
		done = true;
		if(callbacks != null) {
			for(int i = 0; i < this.callbacks.length; i++) {
				if(callbacks[i] != null) {
					callbacks[i].onResult(result, fail,context);
				}
			}
		} 
	}

	public R getResult() {
		if(!done) {
			synchronized(locker) {
				try {
					this.locker.wait(timeout);
				} catch (InterruptedException e) {
					//logger.error("getResult",e);
					this.setFail(MC.MT_REQ_TIMEOUT, "timeout");
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
	
	public void setFail(int code, String msg) {
		 fail = new AsyncFailResult(code,msg);
	}
	
	public void setCounter(int cnt) {
		if(cnt <= 0) {
			throw new CommonException("Count val cannot be null");
		}
		this.ai = new AtomicInteger(cnt);
	}

	public boolean decCounter(int cnt,boolean doDone) {
		if(this.ai.addAndGet(-cnt)<=0) {
			if(doDone) {
				this.done();
			}
			return true;
		}else {
			return false;
		}
	}
	
	public boolean counterFinish() {
		return ai != null && ai.get() <= 0;
	}

	@Override
	public AsyncFailResult getFailResult() {
		getResult();
		return getFail();
	}

	@Override
	public String getFailMsg() {
		getResult();
		if(fail != null) {
			return fail.getMsg();
		} else {
			return null;
		}
	}
	
	@Override
	public boolean isSuccess() {
		getResult();
		return fail == null;
	}

	@Override
	public int getFailCode() {
		getResult();
		if(fail != null) {
			return fail.getCode();
		}else {
			return Resp.CODE_SUCCESS;
		}
	}

	public <T> void setContext(T context) {
		this.context = context;
	}

	public <T> T getContext() {
		return (T)context;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	private class SuccessCallback<R> implements IAsyncCallback<R>{

		private IAsyncSuccessCallback<R>[] successCbs = new IAsyncSuccessCallback[1];
		
		private IAsyncFailCallback[] failCbs = new IAsyncFailCallback[1];
		
		@Override
		public void onResult(R msg, AsyncFailResult fail, Object cxt) {
			if(fail == null) {
				for(int i = 0; i < this.successCbs.length; i++) {
					if(this.successCbs[i] != null) {
						this.successCbs[i].success(msg, cxt);
					}
				}
			} else {
				for(int i = 0; i < this.failCbs.length; i++) {
					if(this.failCbs[i] != null) {
						this.failCbs[i].fail(fail.getCode(), fail.getMsg(), cxt);
					}
				}
			}
		}
		
		private void add(IAsyncSuccessCallback<R> cb) {
			if(successCbs.length == 1 && successCbs[0] == null) {
				successCbs[0] = cb;
			}else {
				IAsyncSuccessCallback<R>[] arr = new IAsyncSuccessCallback[1+this.successCbs.length];
				System.arraycopy(this.successCbs, 0, arr, 0, this.successCbs.length);
				arr[arr.length-1] = cb;
				this.successCbs = arr;
			}
		}
		
		private void add(IAsyncFailCallback cb) {
			if(failCbs.length == 1 && failCbs[0] == null) {
				failCbs[0] = cb;
			}else {
				IAsyncFailCallback[] arr = new IAsyncFailCallback[1+this.failCbs.length];
				System.arraycopy(this.failCbs, 0, arr, 0, this.failCbs.length);
				arr[arr.length-1] = cb;
				this.failCbs = arr;
			}
		}
		
	}
	
}
