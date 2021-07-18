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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.utils.TimeUtils;

/**
 * @author yeyulei
 */
@SO
public final class JMStatisItemJRso{
	
	//RPC上下文下账号的id，非RPC上下文，则是运行实例账号ID
	private int clientId = -1;
	
	//RPC上下文下账号的名称，非RPC上下文，则是运行实例账号
	//private String actName = null;
	
	//rpc上下文下方法KEY值
	private String key = null;
	
	//是否是RPC上下文
	private boolean isRpc;
	
	private String localHost = null;
	private String localPort = null;
	
	private String remoteHost = null;
	private String remotePort = null;
	
	//发出此数据的运行实例名称
	private String instanceName = null;
	
	private long submitTime;//数据从客户端开始提交服务器时间 选项数据等待提交时间 = submitTime - 每个选项创建时间
	private long costTime;//RPC消耗时间  curTime-createTime
	
	private Map<Short,List<StatisItemJRso>> typeStatis = new HashMap<>();
	
	private transient long createTime = TimeUtils.getCurTime();
	
	//private transient ServiceMethod sm = null;
	private transient UniqueServiceMethodKeyJRso smKey = null;
	
	public JMStatisItemJRso() {}
	
	/*public Map<Short,StatisItem> getOneItems(Short[] types) {
		if(types == null || types.length == 0) {
			return null;
		}
		Map<Short,StatisItem> rst = new HashMap<>();
		for(Short t : types) {
			if(this.typeStatis.containsKey(t)) {
				rst.put(t, this.typeStatis.get(t));
			}
		}
		return rst;
	}*/
	
	public void addType(StatisItemJRso si) {
		List<StatisItemJRso> sis = typeStatis.get(si.getType());
		if(sis == null) {
			typeStatis.put(si.getType(), new ArrayList<>());
			sis = typeStatis.get(si.getType());
		}
		sis.add(si);
	}
	
	public StatisItemJRso addType(Short type, long val) {
		List<StatisItemJRso> sis = typeStatis.get(type);
		if(sis == null) {
			typeStatis.put(type, new ArrayList<>());
			sis = typeStatis.get(type);
		}
		
		StatisItemJRso si = new StatisItemJRso();
		si.setType(type);
		si.add(val);
		
		sis.add(si);
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

	/*public String getActName() {
		return actName;
	}

	public void setActName(String actName) {
		this.actName = actName;
	}*/

	public UniqueServiceMethodKeyJRso getSmKey() {
		return smKey;
	}

	public void setSmKey(UniqueServiceMethodKeyJRso smKey) {
		this.smKey = smKey;
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

	public long getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(long submitTime) {
		this.submitTime = submitTime;
	}

	public long getCostTime() {
		return costTime;
	}

	public void setCostTime(long costTime) {
		this.costTime = costTime;
	}

	public Map<Short, List<StatisItemJRso>> getTypeStatis() {
		return typeStatis;
	}

	public void setTypeStatis(Map<Short, List<StatisItemJRso>> typeStatis) {
		this.typeStatis = typeStatis;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isRpc() {
		return isRpc;
	}

	public void setRpc(boolean isRpc) {
		this.isRpc = isRpc;
	}
	
	
}
