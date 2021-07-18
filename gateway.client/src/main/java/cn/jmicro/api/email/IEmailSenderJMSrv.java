package cn.jmicro.api.email;

public interface IEmailSenderJMSrv {
	
	boolean send(String to,String title, String message);
	
}
