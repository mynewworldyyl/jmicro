package cn.jmicro.api;

import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
public class RespJRso<T> {
	
	public static final int CODE_SUCCESS = 0;
	
	public static final int CODE_FAIL = 1;
	public static final int CODE_NO_PERMISSION = 2;
	
	public static final int CODE_TX_FAIL = 3;
	
	public static final int NEED_CHECK_CODE = 4;
	
	private String msg;
	private int code;
	private T data;
	
	private String key;
	
	private int total;
	private int pageSize;
	private int curPage;
	
	public RespJRso() {};
	public RespJRso(int code) {this.code = code;};
	public RespJRso(int code,String msg) {this.code = code;this.msg=msg;};
	public RespJRso(int code,T data) {this.code = code;this.data=data;};
	@Override
	public String toString() {
		return "code: " + this.getCode()+" ,msg: " + this.getMsg();
	}
	
}
