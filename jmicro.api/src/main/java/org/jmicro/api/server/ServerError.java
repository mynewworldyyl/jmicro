package org.jmicro.api.server;

public class ServerError {

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
	
	
}
