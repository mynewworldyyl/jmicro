package cn.jmicro.api.security;

import java.util.HashSet;

import cn.jmicro.api.annotation.SO;

@SO
public class ActInfo implements Cloneable {

	public static final byte SC_WAIT_ACTIVE = 1;
	
	public static final byte SC_ACTIVED = 2;
	
	//异常账号，系统冻结
	public static final byte SC_FREEZE = 4;
	
	public static final byte TOKEN_INVALID = 0;
	
	public static final byte TOKEN_ACTIVE_ACT = 1;
	
	public static final byte TOKEN_RESET_PWD = 2;
	
	//@BsonId
	private long id;
	
	private String actName;
	private String loginKey;
	private int clientId;
	private String pwd;
	private String email;
	private String mobile;
	
	private String token;
	private byte tokenType;
	
	private long registTime;
	
	private byte statuCode;
	
	private long lastActiveTime;
	
	//最后一次登陆时间
	private long lastLoginTime;
	
	//登陆次数
	private long loginNum;
	
	private boolean isAdmin = false;
	
	private HashSet<String> pers = new HashSet<>();
	
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

	public HashSet<String> getPers() {
		return pers;
	}

	public void setPers(HashSet<String> pers) {
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public byte getTokenType() {
		return tokenType;
	}

	public void setTokenType(byte tokenType) {
		this.tokenType = tokenType;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public void setLastActiveTime(long lastActiveTime) {
		this.lastActiveTime = lastActiveTime;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(long lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public long getLoginNum() {
		return loginNum;
	}

	public void setLoginNum(long loginNum) {
		this.loginNum = loginNum;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	
}
