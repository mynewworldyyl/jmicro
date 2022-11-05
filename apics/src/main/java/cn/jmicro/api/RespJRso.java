package cn.jmicro.api;

import com.alibaba.fastjson.JSON;

import cn.jmicro.api.http.JHttpStatus;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.Message;
import lombok.Data;
import lombok.Serial;

@Serial
@Data
public class RespJRso<T> implements IResp/*,cn.jmicro.api.codec.ISerializeObject*/{
	
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
	private String sign;
	
	private int total;
	private int pageSize;
	private int curPage;
	
	public RespJRso() {};
	
	public RespJRso(int code) {this.code = code;};
	
	public RespJRso(int code,String msg) {this.code = code;this.msg=msg;};
	
	//public RespJRso(int code,T data) {this.code = code;this.data=data;};
	
	public static <R> RespJRso<R> copy(RespJRso fr) {
		RespJRso<R> r = new RespJRso<>(fr.getCode(),fr.getMsg());
		r.setCurPage(fr.getCurPage());
		r.setData((R)fr.getData());
		r.setKey(fr.getKey());
		r.setMsg(fr.getMsg());
		r.setPageSize(fr.getPageSize());
		r.setPkgMsg(fr.getPkgMsg());
		r.setSign(fr.getSign());
		r.setTotal(fr.getTotal());
		return r;
	}
	
	public static <R> RespJRso<R> r() {
		return new RespJRso<>();
	}
	
	public static <R> RespJRso<R> r(int code) {
		return new RespJRso<>(code);
	}
	
	public static <R> RespJRso<R> r(int code, R data) {
		RespJRso<R> r = new RespJRso<>(code);
		r.data(data);
		return r;
	}
	
	public static <R> RespJRso<R> r(int code,String msg) {
		return new RespJRso<>(code,msg);
	}
	
	@Override
	public String toString() {
		return "code: " + this.getCode()+" ,msg: " + this.getMsg();
	}
	
	@Override
	public Object getResult() {
		return this.data;
	}
	
	//http 302重定向
	public static RespJRso<String> httpRedirect(String url) {
		return RespJRso.r(JHttpStatus.HTTP_MOVED_TEMP,url);
		/* this.code(JHttpStatus.HTTP_MOVED_TEMP).data(url);
		 return this;*/
	}
	
	public RespJRso<T> code(int code) {
		this.code = code;
		return this;
	}
	
	public RespJRso<T> msg(String msg) {
		this.msg = msg;
		return this;
	}
	
	public RespJRso<T> data(T data) {
		this.data = data;
		return this;
	} 
	
	 /** 输出json格式字符串 **/
    public String toJSONString(){
        return JSON.toJSONString(this);
    }
    
  /* // @java.lang.Override()
    public void encode(final java.io.DataOutput buf) throws java.io.IOException {
        cn.jmicro.api.codec.JDataOutput out = (cn.jmicro.api.codec.JDataOutput)buf;
        out.writeInt(this.code);
        out.writeInt(this.curPage);
        if (this.data == null) out.write(cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_NULL); else {
            out.write(cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_PROXY);
            cn.jmicro.api.codec.typecoder.ITypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactoryUtils.getIns().getDefaultCoder();
            __coder.encode(buf, this.data, null, null);
        }
        if (this.key == null) out.writeUTF(""); else out.writeUTF(this.key);
        if (this.msg == null) out.writeUTF(""); else out.writeUTF(this.msg);
        out.writeInt(this.pageSize);
        if (this.sign == null) out.writeUTF(""); else out.writeUTF(this.sign);
        out.writeInt(this.total);
    }
    
    //@java.lang.Override()
    public void decode(final java.io.DataInput buf) throws java.io.IOException {
        cn.jmicro.api.codec.JDataInput in = (cn.jmicro.api.codec.JDataInput)buf;
        this.code = in.readInt();
        this.curPage = in.readInt();
        if (in.readByte() == cn.jmicro.api.codec.DecoderConstant.PREFIX_TYPE_NULL) this.data = null; else {
            cn.jmicro.api.codec.typecoder.ITypeCoder __coder = cn.jmicro.api.codec.TypeCoderFactoryUtils.getIns().getDefaultCoder();
            this.data = (T)__coder.decode(buf, null, null);
        }
        this.key = in.readUTF();
        this.msg = in.readUTF();
        this.pageSize = in.readInt();
        this.sign = in.readUTF();
        this.total = in.readInt();
    }*/
}
