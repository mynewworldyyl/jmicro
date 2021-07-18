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
package cn.jmicro.api.choreography;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.sysstatis.SystemStatisJRso;

@IDStrategy(1)
@SO
public final class AgentInfoJRso {

	//每个Agent每次运行分配唯一ID
	private String id;
	
	private int clientId;
	
	//JMicro instance name
	//private String name;
	
	private String host;
	
	private String initDepIds;
	
	//yyyy-MM-dd:hh:mm:ss
	private long startTime;
	
	private long assignTime;
	
	private boolean active;
	
	private SystemStatisJRso ss;
	
	private boolean privat = false;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public long getAssignTime() {
		return assignTime;
	}

	public void setAssignTime(long assignTime) {
		this.assignTime = assignTime;
	}

	public SystemStatisJRso getSs() {
		return ss;
	}

	public void setSs(SystemStatisJRso ss) {
		this.ss = ss;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isPrivat() {
		return privat;
	}

	public void setPrivat(boolean privat) {
		this.privat = privat;
	}

	public String getInitDepIds() {
		return initDepIds;
	}

	public void setInitDepIds(String initDepIds) {
		this.initDepIds = initDepIds;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	@Override
	public String toString() {
		return "AgentInfo [id=" + id + ", host=" + host + ", initDepIds=" + initDepIds
				+ ", startTime=" + startTime + ", assignTime=" + assignTime + ", active=" + active + ", privat="
				+ privat + "]";
	}


}
