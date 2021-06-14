package cn.jmicro.mng.mail;

import java.security.Security;
import java.util.Date;
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
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.email.IEmailSender;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;

@Component
@Service(version="0.0.1",retryCnt=0,external=true,debugMode=1,showFront=false)
public class MailSender implements IEmailSender{

	private final static Logger logger = LoggerFactory.getLogger(MailSender.class);
	
	@Cfg(value="/MailSender/from",defGlobal=true)
	private  String mailFrom = "378862956@qq.com";// 指明邮件的发件人
	
	// 指明邮件的发件人登陆密码
	private  String passwordMailFrom = "";
	
	@Cfg(value="/MailSender/host",defGlobal=true)
	private  String mailHost ="smtp.qq.com";	// 邮件的服务器域名
	
	@Cfg(value="/MailSender/code",defGlobal=true)
	private String code = "";
	
	public static void main(String[] args) throws Exception {
		//new MailSender().send("mynewworldyyl@gmail.com", "节日快乐", "测试发送邮件");
		new MailSender().send("xiaoruanfang@sina.com", "节日快乐", "测试发送邮件");
	}
	
	public boolean send0(String to, String title, String content) {
		logger.info("code:" + this.code + ", mail from: " + this.mailFrom);
		Properties prop = new Properties();
		prop.setProperty("mail.host", mailHost);
		prop.setProperty("mail.port", "465");
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
	
	
	public boolean send(String to,String title, String message) {
        try {
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
            //设置邮件会话参数
            Properties props = new Properties();
            //邮箱的发送服务器地址
            props.setProperty("mail.smtp.host", this.mailHost);
            props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
            //邮箱发送服务器端口,这里设置为465端口
            props.setProperty("mail.smtp.port", "465");
            props.setProperty("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.auth", "true");
            //获取到邮箱会话,利用匿名内部类的方式,将发送者邮箱用户名和密码授权给jvm
            Session session = Session.getDefaultInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailFrom, code);
                }
            });
        	//session.setDebug(true);
            //通过会话,得到一个邮件,用于发送
            Message msg = new MimeMessage(session);
            //设置发件人
            msg.setFrom(new InternetAddress(mailFrom));
            //设置收件人,to为收件人,cc为抄送,bcc为密送
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(to, false));
            msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(to, false));
            msg.setSubject(title);
            //设置邮件消息
            msg.setText(message);
            //设置发送的日期
            msg.setSentDate(new Date());
            
            //调用Transport的send方法去发送邮件
            Transport.send(msg);
            return true;
        } catch (Exception e) {
        	String errMsg = "to: " + to+" title: "+title+" message: "+ message;
            LG.log(MC.LOG_ERROR, MailSender.class, errMsg,e);
            logger.error(errMsg,e);
            return false;
        }

    }

	
}
