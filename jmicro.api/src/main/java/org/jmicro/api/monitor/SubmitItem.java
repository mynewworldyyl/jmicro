package org.jmicro.api.monitor;

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;

public class SubmitItem{

	private int type = -1;
	private boolean finish = true;
	private IRequest req = null;
	private IResponse resp = null;
	private Object[] args  = null;
	private long time;
	
	public boolean isFinish() {
		return finish;
	}
	public void setFinish(boolean finish) {
		this.finish = finish;
	}
	public IRequest getReq() {
		return req;
	}
	public void setReq(IRequest req) {
		this.req = req;
	}
	public IResponse getResp() {
		return resp;
	}
	public void setResp(IResponse resp) {
		this.resp = resp;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	
}
