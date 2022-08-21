package cn.jmicro.api.http;

import cn.jmicro.common.Constants;

public interface IHttpRequestHandler {
	
	public static final String HANDLER_KEY = Constants.DEFAULT_PREFIX + "__hh__";
	public static final String HANDLER_METHOD = Constants.DEFAULT_PREFIX + "_HTTP_METHOD";
	
	boolean handle(HttpRequest req, HttpResponse resp);
	
	boolean match(HttpRequest req);
	
}
