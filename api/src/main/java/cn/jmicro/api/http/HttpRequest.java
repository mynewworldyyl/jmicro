package cn.jmicro.api.http;

import java.util.Map;

import cn.jmicro.api.net.ISession;

public interface HttpRequest {

	String getUri();
	
	String getMethod();
	
	String getContentType();
	
	String getReqParam(String headerName);
	
	Map<String,String> getAllParam();
	
	String getHeaderParam(String headerName);
	
	Map<String,String> getHeaderParams();
	
	//public boolean isSuccess();

	String getTextBody();

	boolean isKv();
	
	String getPath();
	
	String getQryParams();
	
	ISession getSession();
	
	int getContentLen();
	
	Integer getClient();
	
	String getToken();

	//public String getRetMsg();
	
}
