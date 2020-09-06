package cn.jmicro.api.security;

import java.util.Set;

import cn.jmicro.api.annotation.SO;

@SO
public class ActInfo implements Cloneable {

	private String actName;
	private String loginKey;
	private int clientId;
	private String pwd;
	
	private String msg;
	
	private boolean enable;
	
	private long registTime;
	
	private Set<String> pers;
	
	private boolean success = false;
	
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

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
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

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public long getRegistTime() {
		return registTime;
	}

	public void setRegistTime(long registTime) {
		this.registTime = registTime;
	}
	
}
