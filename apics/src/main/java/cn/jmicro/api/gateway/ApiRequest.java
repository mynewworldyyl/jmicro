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
package cn.jmicro.api.gateway;

import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.Message;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:21:55
 *
 */
@SO
public final class ApiRequest implements IReq {
	
	private Map<String,Object> params = new HashMap<>();
	
	//private String serviceName = "";
	
	//private String namespace = "";
	
	//private String version = "";
	
	//private String method = "";
	
	private Object[] args = null;
	
	private Long reqId = -1L;
	
	//private int impCode = 0;
	
	//private transient ServiceMethod sm ;
	
	private transient Message msg = null;
	
	public Map<String, Object> getParams() {
		return params;
	}
	public void setParams(Map<String, Object> params) {
		this.params = params;
	}
	
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	
	public Long getReqId() {
		return reqId;
	}
	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}
	public Message getMsg() {
		return msg;
	}
	public void setMsg(Message msg) {
		this.msg = msg;
	}
	
}
