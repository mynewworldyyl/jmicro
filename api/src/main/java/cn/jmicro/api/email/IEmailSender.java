package cn.jmicro.api.email;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IEmailSender {
	
	boolean send(String to,String from,String title, String message);
	
	//boolean send(String to,String from,String title, String message);
}
