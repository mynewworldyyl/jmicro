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
package org.jmicro.api.monitor;

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
public final class SubmitItem{
	
	//消息类型
	private short type = -1;
	
	//日志实例
	private LogEntry log = null;
	
	private Long linkId = null;
	
	private String instanceName = null;
	
	private String localHost = null;
	private String localPort = null;
	private String remoteHost = null;
	private String remotePort = null;
	
	private String[] others = new String[0];
	
	private String desc;
	
	private Message msg  = null;
	
	private IReq req = null;
	
	private IResp resp = null;
	
	private ServiceMethod sm = null;
	
	private long time = 0;
	
	public void reset() {
		log = null;
		type = -1;
		
		linkId = null;

		localHost = null;
		localPort = null;
		remoteHost = null;
		remotePort = null;
		instanceName = null;

		others = new String[0];
		msg  = null;
		req = null;
		resp = null;
		desc = null;
		
	}
	
	public SubmitItem() {
		this.time = System.currentTimeMillis();
	}
	
	public SubmitItem(LogEntry log) {
		this.log = log;
	}
	

	public SubmitItem(short type,long linkId,String... others) {
		this(type);
		this.linkId = linkId;
		if(others.length > 0) {
			this.others = others;
		}
	}
	
	public SubmitItem(short type,Message msg,String... others) {
		this(type,msg.getLinkId(),others);
		this.msg = msg;
	}
	
	public SubmitItem(short type,long linkId,IReq req,String... others) {
		this(type,linkId,others);
		this.req = req;
	}
	
	public SubmitItem(short type,long linkId,IResp resp,String... others) {
		this(type,linkId,others);
		this.resp = resp;
	}
	
	public SubmitItem(short type) {
		this();
		this.type = type;
	}
	
	public SubmitItem(short type,Message msg) {
		this(type);
		this.msg = msg;
	}
	
	public SubmitItem(short type,IReq req) {
		this(type);
		this.req = req;
	}
	
	public SubmitItem(short type,IResp resp) {
		this(type);
		this.resp = resp;
	}
	
	public SubmitItem(short type,IReq req,IResp resp) {
		this(type);
		this.resp = resp;
		this.req = req;
	}
	
	public SubmitItem(short type,Message msg,IReq req,IResp resp) {
		this(type);
		this.resp = resp;
		this.req = req;
		this.msg = msg;
	}
	
	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getLocalPort() {
		return localPort;
	}

	public void setLocalPort(String port) {
		this.localPort = port;
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

	public void setLinkId(Long linkId) {
		this.linkId = linkId;
	}

	public void appendOther(String msg) {
		String[] arr = new String[this.others.length+1];
		System.arraycopy(this.others, 0, arr, 0, this.others.length);
		arr[arr.length-1] = msg;
		this.others = arr;
	}
	
	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public long getLinkId() {
		return linkId;
	}

	public void setLinkId(long linkId) {
		this.linkId = linkId;
	}

	public String[] getOthers() {
		return others;
	}

	public void setOthers(String[] others) {
		this.others = others;
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

	public String getLocalHost() {
		return localHost;
	}

	public void setLocalHost(String host) {
		this.localHost = host;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public LogEntry getLog() {
		return log;
	}

	public void setLog(LogEntry log) {
		this.log = log;
	}

	public ServiceMethod getSm() {
		return sm;
	}

	public void setSm(ServiceMethod sm) {
		this.sm = sm;
	}

}
