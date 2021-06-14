package cn.jmicro.api.email;

public interface IEmailSender {
	
	boolean send(String to,String title, String message);
	
}
