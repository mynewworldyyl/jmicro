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
package org.jmicro.api.monitor.v2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jmicro.api.annotation.SO;
import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.ServiceMethod;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:47
 */
@SO
public final class MRpcItem{
	
	private long linkId;
	
	private long reqId;
	
	private transient Message msg  = null;
	
	private IReq req = null;
	
	private IResp resp = null;
	
	private ServiceMethod sm = null;
	
	private String localHost = null;
	private String localPort = null;
	private String remoteHost = null;
	private String remotePort = null;
	private String instanceName = null;
	
	private List<OneItem> items = new LinkedList<>();
	
	public MRpcItem() {}
	
	public MRpcItem(long lid,long reqId) {
		this.linkId = lid;
		this.reqId = reqId;
	}
	
	@SuppressWarnings("unchecked")
	public List<OneItem> getOneItems(Short[] types) {
		if(types == null || types.length == 0) {
			return null;
		}
		List<OneItem> rst = new ArrayList<>();
		for(Short t : types) {
			for(OneItem oi : items) {
				if(t == oi.getType()) {
					rst.add(oi);
				}
			}
		}
		return rst;
	}
	
	public MRpcItem copy() {
		MRpcItem mi = new MRpcItem();
		mi.instanceName = this.instanceName;
		mi.linkId = this.linkId;
		mi.localHost = this.localHost;
		mi.localPort = this.localPort;
		mi.remoteHost = this.remoteHost;
		mi.remotePort = this.remotePort;
		mi.req = this.req;
		mi.reqId = this.reqId;
		mi.resp = this.resp;
		mi.sm = this.sm;
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
	}
	
	public OneItem addOneItem(OneItem oi) {
		this.items.add(oi);
		return oi;
	}
	
	public OneItem addOneItem(short type,String tag) {
		OneItem oi = new OneItem(type,tag,"");
		this.items.add(oi);
		return oi;
	}
	
	public OneItem addOneItem(short type,String tag,String desc) {
		OneItem oi = new OneItem(type,tag,desc);
		this.items.add(oi);
		return oi;
	}
	
	public OneItem addOneItem(short type,String tag,String desc,Object[] others) {
		OneItem oi = new OneItem(type,tag,desc);
		oi.setOthers(others);
		this.items.add(oi);
		return oi;
	}
	
	public OneItem addOneItem(short type,String tag,String desc,long time) {
		OneItem oi = new OneItem(type,tag,desc);
		oi.setTime(time);
		this.items.add(oi);
		return oi;
	}
	
	public OneItem addOneItem(short type,String tag,Throwable ex) {
		OneItem oi = new OneItem(type,tag,"");
		oi.setEx(ex);
		this.items.add(oi);
		return oi;
	}
	

	public long getLinkId() {
		return linkId;
	}

	public void setLinkId(long linkId) {
		this.linkId = linkId;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public IReq getReq() {
		return req;
	}

	public void setReq(IReq req) {
		this.req = req;
	}

	public IResp getResp() {
		return resp;
	}

	public void setResp(IResp resp) {
		this.resp = resp;
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

	public List<OneItem> getItems() {
		return items;
	}

	public void setItems(List<OneItem> items) {
		this.items = items;
	}

	public long getReqId() {
		return reqId;
	}

	public void setReqId(long reqId) {
		this.reqId = reqId;
	}

}
