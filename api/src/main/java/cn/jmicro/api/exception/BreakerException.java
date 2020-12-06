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
package cn.jmicro.api.exception;

import java.util.Set;

import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.CommonException;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:49:40
 */
public class BreakerException extends CommonException {
	
	private static final long serialVersionUID = 1983756321377501133L;
	
	private Set<ServiceItem> sis;
	
	public BreakerException(String cause,  Set<ServiceItem>  si){
		super(cause);
		this.sis=si;
	}
	
	public BreakerException(String cause,Throwable exp,  Set<ServiceItem>  si){
		super(cause,exp);
		this.sis = si;
	}
	
	public BreakerException(int code,String cause,  Set<ServiceItem>  si){
		super(code,cause,null);
		this.sis=si;
	}
	
	public BreakerException(int code,String cause,Throwable exp,  Set<ServiceItem>  si){
		super(code,cause,exp);
		this.sis=si;
	}

	public Set<ServiceItem> getSis() {
		return sis;
	}
	
}
