package cn.jmicro.api.http;

import java.util.Map;

public interface HttpRequest {

	String getMethod();
	
	String getUri();
	
	String getReqParam(String headerName);
	
	Map<String,String> getAllParam();
	
	String getHeaderParam(String headerName);
	
	Map<String,String> getHeaderParams();
	
}
