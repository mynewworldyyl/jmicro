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
package org.jmicro.api.gateway;

import org.jmicro.api.net.Message;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:02
 *
 */
public final class ApiResponse {
	private Long id = -1L;
	private transient Message msg = null;
	private Long reqId =  -1L;
	private Object result = null;
	private boolean success = true;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Message getMsg() {
		return msg;
	}
	public void setMsg(Message msg) {
		this.msg = msg;
	}
	public Long getReqId() {
		return reqId;
	}
	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}
	public Object getResult() {
		return result;
	}
	public void setResult(Object result) {
		this.result = result;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
}
