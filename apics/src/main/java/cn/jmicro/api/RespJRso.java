package cn.jmicro.api;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.Message;
import lombok.Data;

@SO
@Data
public class RespJRso<T> implements IResp{
	
	public static final int CODE_SUCCESS = 0;
	
	public static final int CODE_FAIL = 1;
	public static final int CODE_NO_PERMISSION = 2;
	
	public static final int CODE_TX_FAIL = 3;
	
	public static final int NEED_CHECK_CODE = 4;
	
	public static final int SE_NO_PERMISSION = 0x00000005;
	
	public static final int SE_NOT_LOGIN = 0x00000006;
	
	public static final int SE_SERVICE_NOT_FOUND= 0x00000007;
	
	public static final int SE_PACKET_TOO_MAX = 0x00000008;
	
	public static final int SE_INVALID_TOPIC = 0x00000009;
	
	public static final int SE_INVALID_OP_CODE = 0x0000000A;
	
	public static final int SE_INVALID_SUB_ID = 0x0000000B;
	
	public static final int SE_LIMITER = 0x0000000C;
	
	public static final int SE_LIMITER_ENTER_ASYNC = 0x0000000D;
	
	public static final int SE_ASYNC_PUBSUB_FAIL = 0x0000000E;
	
	public static final int SE_INVLID_LOGIN_KEY = 0x0000000F;
	
	public static final int SE_INVLID_ARGS = 0x00000010;
	
	public static final String INVALID_ARGS = "参数无效";
	
	private transient Message pkgMsg;
	
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
	
	@Override
	public Object getResult() {
		return this.data;
	}
	
}
