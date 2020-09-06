package cn.jmicro.mng.mail;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;

@Component
public class MailSender {

	private final static Logger logger = LoggerFactory.getLogger(MailSender.class);
	
	@Cfg(value="/MailSender/from",defGlobal=true)
	private  String mailFrom = "378862956@qq.com";// 指明邮件的发件人
	
	private  String passwordMailFrom = "";// 指明邮件的发件人登陆密码
	
	@Cfg(value="/MailSender/host",defGlobal=true)
	private  String mailHost ="smtp.qq.com";	// 邮件的服务器域名
	
	@Cfg(value="/MailSender/code",defGlobal=true)
	private String code = "";
	
	public static void main(String[] args) throws Exception {
		/*
		mailFrom = "378862956@qq.com";
		password_mailFrom="test";
		mailTo = "mynewworldyyl@gmail.com";
		mailTittle="节日快乐2！";
		mailText = "这是一个简单的邮件";
		mail_host="smtp.qq.com";
		*/
		
		new MailSender().send("mynewworldyyl@gmail.com", "节日快乐", "测试发送邮件");
	}
	
	public boolean send(String to, String title, String content) {
		Properties prop = new Properties();
		prop.setProperty("mail.host", mailHost);
		prop.setProperty("mail.transport.protocol", "smtp");
		prop.setProperty("mail.smtp.auth", "true");
		try {
			Session session = Session.getDefaultInstance(prop, new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(mailFrom, code);
				}
			});

			// 开启Session的debug模式，这样就可以查看到程序发送Email的运行状态
			session.setDebug(true);
			// 2、通过session得到transport对象

			Transport ts = session.getTransport();
			// 3、使用邮箱的用户名和密码连上邮件服务器，发送邮件时，发件人需要提交邮箱的用户名和密码给smtp服务器，用户名和密码都通过验证之后才能够正常发送邮件给收件人。
			ts.connect(mailHost, mailFrom, passwordMailFrom);
			// 4、创建邮件
			Message message = createSimpleMail(session, mailFrom, to, title, content);
			// 5、发送邮件
			ts.sendMessage(message, message.getAllRecipients());
			ts.close();
		} catch (Exception e) {
			logger.error(to + " : " + title + " : " + content, e);
			return false;
		}

		return true;
	}
 
	/**
	 * @Method: createSimpleMail
	 * @Description: 创建一封只包含文本的邮件
	 */
	private MimeMessage createSimpleMail(Session session, String mailfrom, String mailTo, String mailTittle,
			String mailText) throws Exception {
		// 创建邮件对象
		MimeMessage message = new MimeMessage(session);
		// 指明邮件的发件人
		message.setFrom(new InternetAddress(mailfrom));
		// 指明邮件的收件人，现在发件人和收件人是一样的，那就是自己给自己发
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
		// 邮件的标题
		message.setSubject(mailTittle);
		// 邮件的文本内容
		message.setContent(mailText, "text/html;charset=UTF-8");
		// 返回创建好的邮件对象
		return message;
	}

	
}
