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
package org.jmicro.api.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:22
 */
public abstract class AbstractSession implements ISession{

	private long sessionId=-1L;
	
	private Map<String,Object> params = new ConcurrentHashMap<String,Object>();
	
	private ByteBuffer readBuffer;
	
	private int heardbeatInterval;
	
	private long lastActiveTime = System.currentTimeMillis();
	
	private boolean isClose = false;
	
	private boolean openDebug = false;
	
	public AbstractSession(int bufferSize,int heardbeatInterval){
		readBuffer = ByteBuffer.allocate(bufferSize);
		this.heardbeatInterval = heardbeatInterval;
	}
	
	@Override
	public void active() {
		lastActiveTime = System.currentTimeMillis();
	}

	@Override
	public boolean isActive() {
		return (System.currentTimeMillis() - this.lastActiveTime) < (this.heardbeatInterval * 1000)*5;
	}

	public long getId() {
		return sessionId;
	}

	public boolean isOpenDebug() {
		return openDebug;
	}

	public void setOpenDebug(boolean openDebug) {
		this.openDebug = openDebug;
	}

	public void setId(long sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public int hashCode() {
		return new Long(sessionId).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null){
			return false;
		}
		if(!(obj instanceof AbstractSession)) {
			return false;
		}
		AbstractSession as = (AbstractSession)obj;
		return this.sessionId == as.getId();
	}

	@Override
	public void close(boolean flag) {
		params.clear();
		this.sessionId=-1L;
		this.isClose = true;
	}

	@Override
	public Object getParam(String key) {
		return this.params.get(key);
	}

	@Override
	public void putParam(String key, Object obj) {
		this.params.put(key, obj);
	}

	public ByteBuffer getReadBuffer() {
		return readBuffer;
	}

	public void setReadBuffer(ByteBuffer readBuffer) {
		this.readBuffer = readBuffer;
	}

	@Override
	public boolean isClose() {
		return this.isClose;
	}

	@Override
	public String remoteHost() {
		return this.getRemoteAddress().getHostName();
	}

	@Override
	public int remotePort() {
		return this.getRemoteAddress().getPort();
	}

	@Override
	public String localHost() {
		return this.getLocalAddress().getHostName();
	}

	@Override
	public int localPort() {
		return this.getLocalAddress().getPort();
	}
	
	public abstract InetSocketAddress getLocalAddress();
	
	public abstract InetSocketAddress getRemoteAddress();
}
