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

import java.util.LinkedList;
import java.util.List;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.JsonUtils;
import lombok.Data;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:47
 */
@SO
@IDStrategy(100)
@Data
public final class JMLogItem{
	
	public static final String TABLE = "rpc_log";
	
	private long id;
	
	private long linkId;
	
	private long reqId=0;
	
	private long reqParentId = -1L;
	
	private int actClientId = -1;
	
	private int sysClientId = -1;
	
	private String actName;
	
	private IReq req = null;
	
	private IResp resp = null;
	
	private UniqueServiceMethodKey smKey = null;
	
	private boolean nl;
	
	private String implCls;
	
	private String localHost = null;
	private String localPort = null;
	
	private String remoteHost = null;
	private String remotePort = null;
	
	private String instanceName = null;
	
	private boolean provider;
	
	private long createTime = TimeUtils.getCurTime();
	private long inputTime;
	private long costTime;
	
	//private short[] types = null;
	
	private String tag = null;
	
	private String configId = null;
	
	private List<OneLog> items = new LinkedList<>();
	
	private transient Message msg  = null;
	
	public JMLogItem() {}
	
	public JMLogItem copy() {
		JMLogItem mi = new JMLogItem();
		mi.instanceName = this.instanceName;
		mi.linkId = this.linkId;
		mi.localHost = this.localHost;
		mi.localPort = this.localPort;
		mi.remoteHost = this.remoteHost;
		mi.remotePort = this.remotePort;
		mi.req = this.req;
		mi.reqId = this.reqId;
		mi.resp = this.resp;
		mi.smKey = this.smKey;
		mi.provider = this.provider;
		mi.createTime = this.createTime;
		mi.reqParentId = this.reqParentId;
		mi.implCls = this.implCls;
		mi.inputTime = this.inputTime;
		mi.costTime = this.costTime;
		mi.actClientId = this.actClientId;
		mi.sysClientId = this.sysClientId;
		mi.actName = this.actName;
		mi.nl = this.nl;
		mi.configId = this.configId;
		mi.tag = this.tag;
		
		return mi;
	}
	
	public void reset() {
		linkId = 0;
		reqId = 0;
		localHost = null;
		localPort = null;
		remoteHost = null;
		remotePort = null;
		instanceName = null;
		msg  = null;
		req = null;
		resp = null;
		provider = false;
		implCls = null;
		createTime = TimeUtils.getCurTime();
		inputTime = 0;
		this.actClientId = -1;
		this.sysClientId = -1;
		this.actName = null;
		this.smKey = null;
		this.nl = false;
	}
	
	public OneLog addOneItem(OneLog oi) {
		this.items.add(oi);
		return oi;
	}
	
	public OneLog addOneItem(byte level,String tag,String desc) {
		OneLog oi = new OneLog(level,tag,desc);
		oi.setLevel(level);
		this.items.add(oi);
		return oi;
	}
	
	public OneLog addOneItem(byte logLevel,String tag,String desc,long time) {
		OneLog oi = new OneLog(logLevel,tag,desc);
		oi.setLevel(logLevel);
		oi.setTime(time);
		this.items.add(oi);
		return oi;
	}
	
	public OneLog addOneItem(byte logLevel,String tag) {
		OneLog oi = new OneLog(logLevel,tag,"");
		this.items.add(oi);
		return oi;
	}

	@Override
	public String toString() {
		return JsonUtils.getIns().toJson(this);
	}

}
