package cn.jmicro.transport.netty.server.httpandws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.http.HttpRequest;
import cn.jmicro.common.Utils;
import io.netty.handler.codec.http.FullHttpRequest;

public class JMicroNettyHttpRequest implements HttpRequest {

	private FullHttpRequest r;
	
	private Map<String,String> reqParams = null;
	
	public JMicroNettyHttpRequest(FullHttpRequest request) {
		this.r = request;
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
		if(reqParams != null) {
			return reqParams.get(headerName);
		}
		
		reqParams = new HashMap<>();
		
		String p = r.uri();
		if(Utils.isEmpty(p)) {
			return null;
		}
		
		int idx = p.indexOf("?");
		if(idx <= 0) return null;
		
		
		p = p.substring(idx+1);
		String[] pa = p.split("&");
		for(String pv : pa) {
			String[] pvv = pv.split("=");
			reqParams.put(pvv[0], pvv[1]);
		}
		
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

}
