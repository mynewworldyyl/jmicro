package cn.jmicro.api.email;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IEmailSenderJMSrv {
	
	boolean send(String to,/*String from,*/String title, String message);
	
	//boolean send(String to,String from,String title, String message);
}
