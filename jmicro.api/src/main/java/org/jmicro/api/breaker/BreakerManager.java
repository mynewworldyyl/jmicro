package org.jmicro.api.breaker;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

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
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.exception.BreakerException;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:49:55
 */
@Component
public class BreakerManager {

	@Inject(required=false)
	private Set<IBreakerHandler> breakerHandler = new HashSet<>();
	
	@Inject
	private IRegistry registry;
	
	public void init(){
		
	}
	
	/**
	 * fuse specify service method
	 * @param service
	 * @param method
	 * @param argTypes
	 */
	public void breakService(ServiceItem serviceItem,ServiceMethod methodIem){
		
		
	}
	
	/**
	 * default request handler for break service
	 * @param method service method
	 * @param args method args
	 * @param items service items witch is in fusing status
	 * @return
	 */
	public Object onBreaking(Method method, Object[] args,Set<ServiceItem> items,BreakerException e){
		for(IBreakerHandler h : this.breakerHandler){
			if(h.canHandle(method, args, items));{
				return h.canHandle(method, args, items);
			}
		}
		throw e;
	}
}
