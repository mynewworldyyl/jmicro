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
package org.jmicro.api.server;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:35
 */
public class ServerError {
	
	public static final int SE_LIMITER = 0xfffffff1;

	private int errorCode;
	private String msg;
	
	public ServerError(){}
	
	public ServerError(int errorCode,String msg) {
		this.errorCode = errorCode;
		this.msg = msg;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "ServerError [errorCode=" + errorCode + ", msg=" + msg + "]";
	}
	
	
}
