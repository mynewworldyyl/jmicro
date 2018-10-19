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
package org.jmicro.transport.mina;

import java.net.InetSocketAddress;

import org.jmicro.api.net.AbstractSession;
import org.jmicro.api.net.ISession;

import com.sun.net.httpserver.HttpExchange;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:27
 */
@SuppressWarnings("restriction")
public abstract class AbstractHttpSession extends AbstractSession implements ISession{

	
	protected HttpExchange exchange;
	
	private boolean isclose = false;
	
	public AbstractHttpSession(HttpExchange exchange,int readBufferSize,int heardbeatInterval) {
		super(readBufferSize,heardbeatInterval);
		this.exchange = exchange;
	}
	
	@Override
	public void close(boolean flag) {
		this.exchange.close();
		isclose=true;
	}
	
	public boolean isClose(){
		return isclose;
	}

	@Override
	public String remoteHost() {
		return getAddress().getHostString();
	}

	@Override
	public int remotePort() {
		return getAddress().getPort();
	}

	@Override
	public String localHost() {
		return getAddress().getHostName();
	}

	@Override
	public int localPort() {
		return getAddress().getPort();
	}
	
	private InetSocketAddress getAddress(){
		return (InetSocketAddress)exchange.getLocalAddress();
	}
	
}
