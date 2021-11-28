package cn.jmicro.api.callback;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ICallbackServiceJMSrv {
	
	public static final int TYPE_LOGIN = 1;
	public static final int TYPE_LOGOUT = 2;

	IPromise<RespJRso<String>> notify(Integer type,String msg);
}
