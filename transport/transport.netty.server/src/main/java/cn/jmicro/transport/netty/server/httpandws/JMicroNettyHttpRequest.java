package cn.jmicro.transport.netty.server.httpandws;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;

import cn.jmicro.api.http.HttpRequest;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.transport.netty.server.NettyServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

public class JMicroNettyHttpRequest implements HttpRequest {

	private FullHttpRequest r;
	
	private boolean success = true;
	private String retMsg;

	private String textBody;
	
	private boolean kv = true;
	
	private String contentType;
	
	private String path;//去除参数后的路径
	private String qryParams;//去除路径后的参数串
	
	private Map<String,String> reqParams =  new HashMap<>();
	
	private NettyServerSession session;
	
	public JMicroNettyHttpRequest(FullHttpRequest request,NettyServerSession session) {
		this.r = request;
		this.session = session;
		parseContent();
	}
	
	private void parseContent() {
		
		if(!(HttpMethod.POST.equals(r.method()) || HttpMethod.GET.equals(r.method()))) {
			this.success = false;
			this.retMsg = "目前仅支持POST或GET请求";
			return;
		}
		
		String uri = this.getUri();
		
		int idx = uri.indexOf("?");
		if(idx > 0) {
			this.path = uri.substring(0,idx);
			this.qryParams = uri.substring(idx+1);
			//是GET请求
	        QueryStringDecoder decoder = new QueryStringDecoder(r.uri());
	        decoder.parameters().entrySet().forEach( entry -> {
	        	// entry.getValue()是⼀个List, 只取第⼀个元素
	        	reqParams.put(entry.getKey(), entry.getValue().get(0));
	        });
		} else {
			this.path = uri;
		}
		
		if(HttpMethod.POST.equals(r.method())) {
			// 是POST请求
			this.contentType = getHeaderParam(HttpHeaderNames.CONTENT_TYPE.toString());
			if(Utils.isEmpty(contentType)) {
				contentType = HttpHeaderValues.APPLICATION_JSON.toString();
			}
			
			if(contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())
				|| contentType.startsWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString())) {
				HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(r);
				List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
				for (InterfaceHttpData parm : parmList) {
					Attribute data = (Attribute) parm;
					try {
						reqParams.put(data.getName(), data.getValue());
					} catch (IOException e) {
						throw new CommonException("",e);
					}
				}
				this.kv = true;
			}else if(contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString()) 
					|| contentType.startsWith(HttpHeaderValues.APPLICATION_XML.toString())
					|| contentType.startsWith(HttpHeaderValues.TEXT_PLAIN.toString())) {
				
				kv = false;
				String charset = Constants.CHARSET;
				idx = contentType.indexOf(";charset=");
				if(idx > 0) {
					charset = contentType.substring(idx+9);
				}
				
				ByteBuf bb = r.content();
				byte[] bts = new byte[bb.readableBytes()];
				bb.readBytes(bts);
				try {
					this.textBody = new String(bts,charset).trim();
					if(contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString())&&
							this.textBody.startsWith("{") && this.textBody.endsWith("}")) {
						JSONObject jo = JSONObject.parseObject(this.textBody);
						jo.forEach((k,v)->{
							reqParams.put(k, v != null ? v.toString():null);
						});
					}
				} catch (UnsupportedEncodingException e) {
					this.success = false;
					this.retMsg = "请求体编码类型错误：" + charset;
					return;
				}
				
			}
		}
	}

	@Override
	public int getContentLen() {
		return r.content().array().length;
	}

	@Override
	public String getMethod() {
		return r.method().name();
	}

	@Override
	public String getUri() {
		return r.uri();
	}
	
	@Override
	public String getReqParam(String headerName) {
		return reqParams.get(headerName);
	}

	@Override
	public Map<String, String> getAllParam() {
		return reqParams;
	}

	@Override
	public String getHeaderParam(String headerName) {
		return r.headers().get(headerName);
	}

	@Override
	public Map<String, String> getHeaderParams() {
		Map<String,String> hs = new HashMap<>();
		List<Map.Entry<String, String>> l = r.headers().entries();
		if(l != null) {
			for(Map.Entry<String, String> e : l) {
				hs.put(e.getKey(), e.getValue());
			}
		}
		return hs;
	}
	public boolean isSuccess() {
		return success;
	}

	public String getTextBody() {
		return textBody;
	}

	public boolean isKv() {
		return kv;
	}

	public String getRetMsg() {
		return retMsg;
	}

	public String getContentType() {
		return contentType;
	}

	public String getPath() {
		return path;
	}

	public String getQryParams() {
		return qryParams;
	}

	public NettyServerSession getSession() {
		return session;
	}

	public void setSession(NettyServerSession session) {
		this.session = session;
	}
	
}
