package cn.jmicro.api;

import cn.jmicro.api.annotation.SO;

@SO
public class Resp<T> {
	
	public static final int CODE_SUCCESS = 0;
	
	public static final int CODE_FAIL = 1;
	public static final int CODE_NO_PERMISSION = 2;
	
	private String msg;
	private int code;
	private T data;
	
	private String key;
	
	private int total;
	private int pageSize;
	private int curPage;
	
	public Resp() {};
	public Resp(int code) {this.code = code;};
	public Resp(int code,String msg) {this.code = code;this.msg=msg;};
	
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public T getData() {
		return data;
	}
	public void setData(T data) {
		this.data = data;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public int getCurPage() {
		return curPage;
	}
	public void setCurPage(int curPage) {
		this.curPage = curPage;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	
	
}
