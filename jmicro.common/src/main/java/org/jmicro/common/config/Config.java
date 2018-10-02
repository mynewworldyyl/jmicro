package org.jmicro.common.config;

import org.apache.dubbo.common.URL;

public class Config {
	
	private String[] basePackages = {"org.jmicro"};

	private URL registryUrl = new URL("zookeeper","localhost",2180);
	
	private String bindIp="127.0.0.1";
	
	private int port = 9999;
	
	public String[]  getBasePackages() {
		return basePackages;
	}

	public void setBasePackages(String[]  basePackages) {
		this.basePackages = basePackages;
	}

	public URL getRegistryUrl() {
		return registryUrl;
	}

	public void setRegistryUrl(URL registryUrl) {
		this.registryUrl = registryUrl;
	}

	public String getBindIp() {
		return bindIp;
	}

	public void setBindIp(String bindIp) {
		this.bindIp = bindIp;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	
}
