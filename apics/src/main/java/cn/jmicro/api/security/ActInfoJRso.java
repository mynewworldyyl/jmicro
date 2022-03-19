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
	
    private String avatarUrl;
    private String city;
    private String province;
    private String country;
    private String language;
    private byte gender;
    private int shareUserId;
    
	//@BsonId
	private int id;
	
	private int defClientId;//默认ClientId
	
	private int clientId;//当前登录的client
	
	private String actName;
	private String loginKey;
	
	//private int clientId;
	private String pwd;
	private String email;
	private String mobile;
	
	//实名
	private String realname;
	
	//身份证号
	private String idNo;
	
	//性别
	private String sex;
	
	//账号分数
	private int levelScore;
	
	//账号等级名称
	private String levelName;
	
	private String token;
	private byte tokenType;
	
	private String birthday;
	
	private String wxOpenId;
	
	private String sessionKey;
	
	private String lastLoginIp;
	
	private String nickName;
	
	private long registTime;
	
	private byte statuCode;
	
	private long lastActiveTime;
	
	//最后一次登陆时间
	private long lastLoginTime;
	
	//登陆次数
	private long loginNum;
	
	private Boolean admin = Boolean.FALSE;
	
	private Boolean guest = Boolean.TRUE;
	
	private Set<Integer> roles = new HashSet<>();
	
	private Set<Integer> pers = new HashSet<>();
	
	private Set<Integer> clientIds = new HashSet<>();
	
	private Set<String> tags = new HashSet<>();
	
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
	
	public Set<Integer> getClients() {
		return Collections.unmodifiableSet(this.clientIds);
	}
	
	public String getSex() {
		return sex;
	}

	public void setSex(String sex) {
		this.sex = sex;
	}

	public int minClient() {
		if(this.clientIds.isEmpty()) return 0;
		int cid = 0;
		for(Integer c : this.clientIds) {
			if(cid > c) {
				cid = c;
			}
		}
		return cid;
	}
	
	public void addClient(Integer cid) {
		clientIds.add(cid);
	}
	
	public int getShareUserId() {
		return shareUserId;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public String getRealname() {
		return realname;
	}

	public void setRealname(String realname) {
		this.realname = realname;
	}

	public String getIdNo() {
		return idNo;
	}

	public void setIdNo(String idNo) {
		this.idNo = idNo;
	}

	public int getLevelScore() {
		return levelScore;
	}

	public void setLevelScore(int levelScore) {
		this.levelScore = levelScore;
	}

	public String getLevelName() {
		return levelName;
	}

	public void setLevelName(String levelName) {
		this.levelName = levelName;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	public void setShareUserId(int shareUserId) {
		this.shareUserId = shareUserId;
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

	public Boolean getAdmin() {
		return admin;
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

	public void setAdmin(boolean admin) {
		 if( this.admin != null && !Utils.formSystemPackagePermission(3)) {
			 checkPath("非法设置Admin状态");
		 }
		this.admin = admin;
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

	public int getDefClientId() {
		return defClientId;
	}

	public void setDefClientId(int defClient) {
		this.defClientId = defClient;
	}

	public byte getGender() {
		return gender;
	}

	public void setGender(byte gender) {
		this.gender = gender;
	}

	public String getBirthday() {
		return birthday;
	}

	public void setBirthday(String birthday) {
		this.birthday = birthday;
	}

	public String getWxOpenId() {
		return wxOpenId;
	}

	public void setWxOpenId(String wxOpenId) {
		this.wxOpenId = wxOpenId;
	}

	public String getLastLoginIp() {
		return lastLoginIp;
	}

	public void setLastLoginIp(String lastLoginIp) {
		this.lastLoginIp = lastLoginIp;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public Boolean getGuest() {
		return guest;
	}

	public void setGuest(Boolean guest) {
		this.guest = guest;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setGender(Byte gender) {
		this.gender = gender;
	}

	public static enum Tag {
		Mobile,Email,RealName
	}
}
