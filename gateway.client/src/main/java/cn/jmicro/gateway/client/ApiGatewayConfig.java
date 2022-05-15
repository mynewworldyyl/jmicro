/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jmicro.gateway.client;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月26日 下午8:51:40
 */
public class ApiGatewayConfig {

	private int clientType = Constants.TYPE_SOCKET;
	private String host = null;
	private String port= "9090";
	
	private Integer clientId = 255;
	
	private boolean debug = true;
	
	private boolean upSsl = false;
	private boolean downSsl = false;
	private int encType = 0;
	
	private String myPriKeyFile = null;
	private String myPubKeyFile = null;
	private String apiGwPriKeyFile = null;
	
	private String myPriKeyPwd = null;
	
	private long pwdUpdateInterval = 1000*60*5L;
	
	public ApiGatewayConfig(int clientType) {
		this.setClientType(clientType);
	}
	
	public ApiGatewayConfig(int clientType,String port) {
		this(clientType);
		this.setPort(port);
	}
	
	public ApiGatewayConfig(int clientType,String host,String port) {
		this(clientType,port);
		this.setHost(host);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		if(Utils.isEmpty(host)) {
			throw new CommonException("Host cannot be NULL");
		}
		this.host = host;
	}

	public void setPort(String port) {
		if(Utils.isEmpty(port)) {
			throw new CommonException("port is invalid: "+port);
		}
		this.port = port;
	}

	public int getClientType() {
		return clientType;
	}

	public void setClientType(int clientType) {
		if(clientType == Constants.TYPE_SOCKET || clientType == Constants.TYPE_HTTP
				|| clientType == Constants.TYPE_WEBSOCKET) {
			this.clientType = clientType;
		} else {
			throw new CommonException("Client type not support: "+clientType);
		}
		
	}

	public String getPort() {
		return port;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public long getPwdUpdateInterval() {
		return pwdUpdateInterval;
	}

	public void setPwdUpdateInterval(long pwdUpdateInterval) {
		this.pwdUpdateInterval = pwdUpdateInterval;
	}

	public boolean isUpSsl() {
		return upSsl;
	}

	public void setUpSsl(boolean upSsl) {
		this.upSsl = upSsl;
	}

	public boolean isDownSsl() {
		return downSsl;
	}

	public void setDownSsl(boolean downSsl) {
		this.downSsl = downSsl;
	}

	public int getEncType() {
		return encType;
	}

	public void setEncType(int encType) {
		this.encType = encType;
	}

	public String getMyPriKeyFile() {
		return myPriKeyFile;
	}

	public void setMyPriKeyFile(String myPriKeyFile) {
		this.myPriKeyFile = myPriKeyFile;
	}

	public String getMyPubKeyFile() {
		return myPubKeyFile;
	}

	public void setMyPubKeyFile(String myPubKeyFile) {
		this.myPubKeyFile = myPubKeyFile;
	}

	public String getApiGwPriKeyFile() {
		return apiGwPriKeyFile;
	}

	public void setApiGwPriKeyFile(String apiGwPriKeyFile) {
		this.apiGwPriKeyFile = apiGwPriKeyFile;
	}

	public String getMyPriKeyPwd() {
		return myPriKeyPwd;
	}

	public void setMyPriKeyPwd(String myPriKeyPwd) {
		this.myPriKeyPwd = myPriKeyPwd;
	}

	public Integer getClientId() {
		return clientId;
	}

	public void setClientId(Integer clientId) {
		this.clientId = clientId;
	}
	
	
}
