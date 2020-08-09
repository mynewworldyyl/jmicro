package cn.jmicro.api.async;

public class AsyncFailResult {

	private int code;
	
	private String msg;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "AsyncFailResult [code=" + code + ", msg=" + msg + "]";
	}
	
	
}
