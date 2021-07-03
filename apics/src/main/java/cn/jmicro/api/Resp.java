package cn.jmicro.api;

import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
public class Resp<T> {
	
	public static final int CODE_SUCCESS = 0;
	
	public static final int CODE_FAIL = 1;
	public static final int CODE_NO_PERMISSION = 2;
	
	public static final int CODE_TX_FAIL = 3;
	
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
	public Resp(int code,T data) {this.code = code;this.data=data;};
	
}
