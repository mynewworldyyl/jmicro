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

import java.lang.reflect.InvocationHandler;

import org.jmicro.api.registry.ServiceItem;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:01
 */
public abstract class AbstractClientServiceProxy {

	protected InvocationHandler handler = null;
	
	private ServiceItem item = null;

	public InvocationHandler getHandler() {
		return handler;
	}

	public void setHandler(InvocationHandler handler) {
		this.handler = handler;
	}
	
	public abstract String getNamespace();
	public abstract String getVersion();
	public abstract String getServiceName();
	
	//public abstract  boolean enable();
	//public abstract void enable(boolean enable);
	
	public  void setItem(ServiceItem item){
		this.item = item;
	}
	
	public  ServiceItem getItem(){
		return this.item;
	}
	
	
	public String key(){
		return ServiceItem.serviceName(this.getServiceName(), this.getNamespace(), this.getVersion());
	}
	
	@Override
	public int hashCode() {
		return this.key().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof AbstractClientServiceProxy)){
			return false;
		}
		AbstractClientServiceProxy o = (AbstractClientServiceProxy)obj;
		return this.key().equals(o.key());
	}
	
	
	
}
