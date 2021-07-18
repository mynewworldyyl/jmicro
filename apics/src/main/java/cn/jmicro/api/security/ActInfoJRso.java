package cn.jmicro.api.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;

@SO
public class ActInfoJRso {
	
	public static final byte SC_WAIT_ACTIVE = 1;
	
	public static final byte SC_ACTIVED = 2;
	
	//异常账号，系统冻结
	public static final byte SC_FREEZE = 4;
	
	public static final byte TOKEN_INVALID = 0;
	
	public static final byte TOKEN_ACTIVE_ACT = 1;
	
	public static final byte TOKEN_RESET_PWD = 2;
	
	public static final String GUEST = "guest_";
	
	//@BsonId
	private int id;
	
	private int clientId;
	
	private String avatar;
	
	private String actName;
	private String loginKey;
	
	//private int clientId;
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
	
	private Boolean isAdmin = Boolean.FALSE;
	
	private Boolean guest = Boolean.TRUE;
	
	private Set<Integer> roles = new HashSet<>();
	
	private Set<Integer> pers = new HashSet<>();
	
	public ActInfoJRso() {};
	
	public ActInfoJRso(String actName,String pwd,int id) {
		this.actName = actName;
		this.id = id;
		this.pwd = pwd;
	};
	
	public ActInfoJRso(String actName,int id) {
		this.actName = actName;
		this.id = id;
	};
	
	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getActName() {
		return actName;
	}
	public void setActName(String actName) {
		if(this.actName != null && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置账号名称");
		 }
		this.actName = actName;
	}
	public String getLoginKey() {
		return loginKey;
	}
	public void setLoginKey(String loginKey) {
		if(this.loginKey != null && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置登陆键");
		 }
		this.loginKey = loginKey;
	}

	public boolean isGuest() {
		return guest;
	}
	
	private boolean checkPath(String errMsg) {
		 if(!Utils.callPathExistPackage("cn.jmicro.security")) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,errMsg);
		  }
		 return true;
	}

	public void setGuest(boolean guest) {
		if(this.guest != null && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置账号游客标识");
		 }
		this.guest = guest;
	}

	public String getPwd() {
		if(!Utils.formSystemPackagePermission(3)) {
			checkPath("非法获取账号密码");
		 }
		return pwd;
	}

	public void setPwd(String pwd) {
		if(this.pwd != null && !Utils.formSystemPackagePermission(3)) {
			 checkPath("非法设置账号密码");
		 }
		this.pwd = pwd;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	@Override
	public ActInfoJRso clone() throws CloneNotSupportedException {
		ActInfoJRso ai =(ActInfoJRso) super.clone();
		ai.setActName(this.actName);
		ai.setId(ai.getId());
		//ai.setLoginKey(""+LOGIN_KEY.getAndIncrement());
		return ai;
	}

	public Set<Integer> getPers() {
		if(!Utils.formSystemPackagePermission(3)) {
			 return Collections.unmodifiableSet(this.pers);
		 } else {
			 return pers;
		 }
	}

	public void setPers(Set<Integer> pers) {
		if(!this.pers.isEmpty() && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置账号权限");
		 }
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
		if(this.statuCode != 0 && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置账号状态码");
		 }
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
		if(this.token != null && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置Token");
		 }
		this.token = token;
	}

	public byte getTokenType() {
		return tokenType;
	}

	public void setTokenType(byte tokenType) {
		if(this.tokenType != 0 && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置Token类型");
		 }
		this.tokenType = tokenType;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int defaultClientId) {
		this.clientId = defaultClientId;
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	public void setLastActiveTime(long lastActiveTime) {
		this.lastActiveTime = lastActiveTime;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		if(this.id != 0 && !Utils.formSystemPackagePermission(3)) {
			checkPath("非法设置账号ID");
		}
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
		 if( this.isAdmin != null && !Utils.formSystemPackagePermission(3)) {
			 checkPath("非法设置Admin状态");
		 }
		this.isAdmin = isAdmin;
	}
	
	public void addRole(Integer roleId) {
		this.roles.add(roleId);
	}
	
	public void removeRole(Integer roleId) {
		this.roles.remove(roleId);
	}
	
	public Set<Integer> getRoles() {
		return roles;
	}
}
