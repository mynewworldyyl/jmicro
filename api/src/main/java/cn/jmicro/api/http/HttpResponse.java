package cn.jmicro.api.http;

import java.io.InputStream;
import java.nio.ByteBuffer;

public interface HttpResponse {

	void write(byte[] content);
	
	void write(String content);
	
	void write(ByteBuffer content);
	
	void flush();
	
	void write(InputStream in,int len);
	
	void setHeader(String key,Object value) ;
	
	void setStatusCode(int statusCode);
	
	//30*重定向
	void redirect(int code,String url);
}
