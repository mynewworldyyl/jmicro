package org.jmicro.api.server;

public class ServerError {
	
	public static final int SE_LIMITER = 0xfffffff1;

	private int errorCode;
	private String msg;
	
	public ServerError(){}
	
	public ServerError(int errorCode,String msg) {
		this.errorCode = errorCode;
		this.msg = msg;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "ServerError [errorCode=" + errorCode + ", msg=" + msg + "]";
	}
	
	
}
