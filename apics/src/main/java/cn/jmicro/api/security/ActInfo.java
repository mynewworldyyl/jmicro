package cn.jmicro.api.security;

import java.util.Set;

import cn.jmicro.api.annotation.SO;

@SO
public class ActInfo implements Cloneable {

	public static final byte SC_WAIT_ACTIVE = 1;
	
	public static final byte SC_ACTIVED = 2;
	
	//异常账号，系统冻结
	public static final byte SC_FREEZE = 4;
	
	private String actName;
	private String loginKey;
	private int clientId;
	private String pwd;
	private String mail;
	private String mobile;
	
	private String token;
	
	private long registTime;
	
	private byte statuCode;
	
	private Set<String> pers;
	
	public ActInfo() {};
	
	public ActInfo(String actName,String pwd,int clientId) {
		this.actName = actName;
		this.clientId = clientId;
		this.pwd = pwd;
	};
	
	public ActInfo(String actName,int clientId) {
		this.actName = actName;
		this.clientId = clientId;
	};
	
	public String getActName() {
		return actName;
	}
	public void setActName(String actName) {
		this.actName = actName;
	}
	public String getLoginKey() {
		return loginKey;
	}
	public void setLoginKey(String loginKey) {
		this.loginKey = loginKey;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	@Override
	public ActInfo clone() throws CloneNotSupportedException {
		ActInfo ai =(ActInfo) super.clone();
		ai.setActName(this.actName);
		ai.setClientId(ai.getClientId());
		
		//ai.setLoginKey(""+LOGIN_KEY.getAndIncrement());
		return ai;
	}

	public Set<String> getPers() {
		return pers;
	}

	public void setPers(Set<String> pers) {
		this.pers = pers;
	}

	public long getRegistTime() {
		return registTime;
	}

	public void setRegistTime(long registTime) {
		this.registTime = registTime;
	}

	public byte getStatuCode() {
		return statuCode;
	}

	public void setStatuCode(byte statuCode) {
		this.statuCode = statuCode;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
}
