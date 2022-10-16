package cn.jmicro.api.security;

import lombok.Serial;

@Serial
public class SecretDataJRso {

	private String data;
	
	private String nonce;
	
	private String timestamp;
	
	private String sign;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}
	
	
}
