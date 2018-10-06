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

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:47
 */
public class SubmitItem{

	private int type = -1;
	private boolean finish = true;
	private IRequest req = null;
	private IResponse resp = null;
	private Object[] args  = null;
	private long time;
	
	public boolean isFinish() {
		return finish;
	}
	public void setFinish(boolean finish) {
		this.finish = finish;
	}
	public IRequest getReq() {
		return req;
	}
	public void setReq(IRequest req) {
		this.req = req;
	}
	public IResponse getResp() {
		return resp;
	}
	public void setResp(IResponse resp) {
		this.resp = resp;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	
}
