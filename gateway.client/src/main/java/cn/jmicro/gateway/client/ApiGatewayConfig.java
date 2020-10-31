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
	private int port= 9090;
	
	private boolean debug = true;
	
	private boolean sslEnable = false;
	private long pwdUpdateInterval = 1000*60*5L;
	
	public ApiGatewayConfig(int clientType) {
		this.setClientType(clientType);
	}
	
	public ApiGatewayConfig(int clientType,int port) {
		this(clientType);
		this.setPort(port);
	}
	
	public ApiGatewayConfig(int clientType,String host,int port) {
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

	public void setPort(int port) {
		if(port <= 0) {
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

	public int getPort() {
		return port;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isSslEnable() {
		return sslEnable;
	}

	public void setSslEnable(boolean sslEnable) {
		this.sslEnable = sslEnable;
	}

	public long getPwdUpdateInterval() {
		return pwdUpdateInterval;
	}

	public void setPwdUpdateInterval(long pwdUpdateInterval) {
		this.pwdUpdateInterval = pwdUpdateInterval;
	}
	
	
}
