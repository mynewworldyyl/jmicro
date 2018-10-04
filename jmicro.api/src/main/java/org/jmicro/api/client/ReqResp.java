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
package org.jmicro.api.client;

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.Message;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:36
 */
public class ReqResp {

	public Message msg;
	public IRequest req;
	public IResponseHandler handler;
	private int retryCnt = 0;
	public ReqResp(Message msg,IRequest req,IResponseHandler handler,int retryCnt){
		this.msg = msg;
		this.req = req;
		this.handler = handler;
		this.retryCnt = retryCnt;
	}
	
}
