package cn.jmicro.api.internal.async;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.async.IFinish;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.client.IAsyncFailCallback;
import cn.jmicro.api.client.IAsyncSuccessCallback;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.ServerErrorJRso;
import cn.jmicro.common.CommonException;

public class PromiseImpl<R> implements IPromise<R>{

	private IAsyncCallback<R>[] callbacks ;
	
	private SuccessCallback<R> custCb = null;
	
	private R result;
	
	private Type resultType;
	
	private boolean done = false;
	
	private  AsyncFailResult fail;
	
	private final Object locker = new Object();
	
	private final Object cbLocker = new Object();
	
	private Object context = null;
	
	private int timeout = 30000;
	
	private AtomicInteger ai = null;
	
	private CommonException ex;
	
	public PromiseImpl(IFinish<R> f) {
		this.callbacks = new IAsyncCallback[1];
		f.onResult((R r)->{
			this.setResult(r);
			this.done();
		}, (code,msg)->{
			this.setFail(code, msg);
			this.done();
		});
	}
	
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
			synchronized(cbLocker) {
				addCallback(callback);
			}
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
				synchronized(this.cbLocker) {
					if(custCb == null) {
						custCb = new SuccessCallback<R>();
					}
					addCallback(custCb);
				}
			}
			custCb.add(cb);
		}
		return this;
	}

	@Override
	public IPromise<R> fail(IAsyncFailCallback cb) {
		if(cb == null) {
			throw new NullPointerException();
		}
		if(done && this.fail != null) {
			//done status do callback at once
			cb.fail(fail.getCode(),fail.getMsg(),context);
		} else {
			if(custCb == null) {
				synchronized(cbLocker) {//大部份情况下在偏向锁下情况下工作
					if(custCb == null) {
						custCb = new SuccessCallback<R>();
					}
					addCallback(custCb);
				}
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
			return;
		}
		
		done = true;
		if(callbacks != null) {
			Exception e1 = null;
			for(int i = 0; i < this.callbacks.length; i++) {
				if(callbacks[i] != null) {
					try {
						callbacks[i].onResult(result, fail,context);
					}catch(Exception e) {
						e.printStackTrace();
						e1 = e;
					}
					
				}
			}
			if(e1 != null) {
				if(e1 instanceof CommonException)
					throw (CommonException)e1;
				else 
					throw new CommonException("",e1);
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
		if(ex != null) {
			CommonException ex0 = this.ex;
			this.ex = null;
			throw ex0;
		}
		
		if(result instanceof ServerErrorJRso ) {
			ServerErrorJRso se = (ServerErrorJRso)result;
			throw new CommonException(se.getCode(),se.getMsg());
		} else {
			return result;
		}
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
			return RespJRso.CODE_SUCCESS;
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
	
	public Type resultType() {
		return this.resultType;
	}
	
	public void setResultType(Type rt) {
		this.resultType = rt;
	}

	public Throwable getEx() {
		return ex;
	}

	public void setEx(CommonException ex) {
		this.ex = ex;
	}
	
}
