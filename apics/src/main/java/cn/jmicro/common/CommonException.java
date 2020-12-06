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
package cn.jmicro.common;

import java.io.Serializable;

import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.security.ActInfo;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:02:33
 */
public class CommonException extends RuntimeException implements Serializable{
	 
	private static final long serialVersionUID = 13434325523L;
	
	private int key = 0;
	
	private String others = "";
	
	private IReq req;
	
	private IResp resp;
	
	private ActInfo ai;

	public CommonException(String cause){
		super(cause);
	}
	
	public CommonException(String cause,Throwable exp){
		super(cause,exp);
	}
	
	public CommonException(String cause,Throwable exp,IReq req){
		super(cause,exp);
		this.req = req;
	}
	
	public CommonException(String cause,Throwable exp,IResp resp){
		super(cause,exp);
		this.resp = resp;
	}
	
	public CommonException(int key,String cause){
		this(key,cause,null);
	}
	
	public CommonException(int key,String cause,Throwable exp){
		super("code:" + key + ": "+cause,exp);
		this.key= key;
	}

	@Override
	public String getMessage() {
		return super.getMessage()+", " + others;
	}

	public String getOthers() {
		return others;
	}

	public void setOthers(String others) {
		this.others = others;
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

	public ActInfo getAi() {
		return ai;
	}

	public void setAi(ActInfo ai) {
		this.ai = ai;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}
	
	
}
