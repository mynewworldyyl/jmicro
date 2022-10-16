package cn.jmicro.api.security;

import lombok.Serial;

@Serial
public class JmicroPublicKeyJRso {

	private long id;

	private int clientId;

	private String instancePrefix;
	
	private String publicKey;
	
	private String priKey;
	
	private long createdTime;
	
	private String creater;
	
	private long createrId;
	
	private boolean enable = false;

	public String getInstancePrefix() {
		return instancePrefix;
	}

	public void setInstancePrefix(String instancePrefix) {
		this.instancePrefix = instancePrefix;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public String getCreater() {
		return creater;
	}

	public void setCreater(String creater) {
		this.creater = creater;
	}

	public long getCreaterId() {
		return createrId;
	}

	public void setCreaterId(long createrId) {
		this.createrId = createrId;
	}

	public String getPriKey() {
		return priKey;
	}

	public void setPriKey(String priKey) {
		this.priKey = priKey;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
	
}
