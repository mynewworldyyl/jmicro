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
package cn.jmicro.api.monitor;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.registry.ServiceMethod;

/**
 * 
 * @author yeyulei
 *
 */
@SO
public final class MRpcStatisItem{
	
	private int clientId = -1;
	
	private String actName=null;
	
	private transient ServiceMethod sm = null;
	
	private String key = null;
	
	private String localHost = null;
	private String localPort = null;
	private String remoteHost = null;
	private String remotePort = null;
	private String instanceName = null;
	
	private long inputTime;
	private long costTime;
	
	private transient long createTime;
	
	private Map<Short,StatisItem> typeStatis = new HashMap<>();
	
	public MRpcStatisItem() {}
	
	public MRpcStatisItem copy() {
		MRpcStatisItem mi = new MRpcStatisItem();
		mi.instanceName = this.instanceName;
		mi.localHost = this.localHost;
		mi.localPort = this.localPort;
		mi.remoteHost = this.remoteHost;
		mi.remotePort = this.remotePort;
		mi.sm = this.sm;
		mi.inputTime = this.inputTime;
		mi.costTime = this.costTime;
		mi.clientId = this.clientId;
		mi.actName = this.actName;
		mi.key = this.key;
		mi.createTime = this.createTime;
		return mi;
	}
	
	public void reset() {
		localHost = null;
		localPort = null;
		remoteHost = null;
		remotePort = null;
		instanceName = null;
		inputTime = 0;
		this.clientId = -1;
		this.actName = null;
		this.key = null;
		this.sm = null;
		this.costTime = 0;
		this.createTime = 0;
	}
	
	public Map<Short,StatisItem> getOneItems(Short[] types) {
		if(types == null || types.length == 0) {
			return null;
		}
		Map<Short,StatisItem> rst = new HashMap<>();
		for(Short t : types) {
			rst.put(t, this.typeStatis.get(t));
		}
		return rst;
	}
	
	public StatisItem addType(Short type,int cnt, double sum) {
		StatisItem si = typeStatis.get(type);
		if(si == null) {
			si = new StatisItem();
			typeStatis.put(type, si);
		}
		si.add(cnt, sum);
		return si;
	}

	public long getCreateTime() {
		return createTime;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public String getActName() {
		return actName;
	}

	public void setActName(String actName) {
		this.actName = actName;
	}

	public ServiceMethod getSm() {
		return sm;
	}

	public void setSm(ServiceMethod sm) {
		this.sm = sm;
	}

	public String getLocalHost() {
		return localHost;
	}

	public void setLocalHost(String localHost) {
		this.localHost = localHost;
	}

	public String getLocalPort() {
		return localPort;
	}

	public void setLocalPort(String localPort) {
		this.localPort = localPort;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public String getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(String remotePort) {
		this.remotePort = remotePort;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public long getInputTime() {
		return inputTime;
	}

	public void setInputTime(long inputTime) {
		this.inputTime = inputTime;
	}

	public long getCostTime() {
		return costTime;
	}

	public void setCostTime(long costTime) {
		this.costTime = costTime;
	}

	public Map<Short, StatisItem> getTypeStatis() {
		return typeStatis;
	}

	public void setTypeStatis(Map<Short, StatisItem> typeStatis) {
		this.typeStatis = typeStatis;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	
}
