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
package org.jmicro.objfactory.simple;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.jmicro.api.client.AbstractServiceProxy;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月6日-下午12:08:48
 */
class RemoteProxyServiceListener implements IServiceListener{

	private Object belongTo;
	private Object proxy;
	private Field refField;
	
	private RemoteServiceManager rsm;
	
	RemoteProxyServiceListener(RemoteServiceManager rsm,Object proxy,Object belongTo,Field refField){
		if(proxy== null){
			throw new CommonException("Proxy object cannot be null: "+ belongTo.getClass().getName()+",field: " + refField.getName());
		}
		this.rsm = rsm;
		this.proxy = proxy;
		this.belongTo = belongTo;
		this.refField = refField;
	}

	@Override
	public void serviceChanged(int type, ServiceItem item) {
		
		if(proxy instanceof AbstractServiceProxy) {
			AbstractServiceProxy p = (AbstractServiceProxy)proxy;
			if(!p.key().equals(item.serviceName())){
				throw new CommonException("Service listener give error service item:"+ 
			belongTo.getClass().getName()+"],field: " + refField.getName()+" item:"+item.val());
			}
			if(IServiceListener.SERVICE_ADD == type){
				p.enable(true);
			}else if(IServiceListener.SERVICE_REMOVE == type) {
				p.enable(false);
			}
		}else if(Set.class.isAssignableFrom(refField.getType())){
			Set<Object> set = (Set<Object>)this.proxy;
			
			if(IServiceListener.SERVICE_ADD == type){
				for(Object o: set){
					AbstractServiceProxy p = (AbstractServiceProxy)o;
					if(p.key().equals(item.serviceName())){
						p.enable(true);
						return;
					}
				}
				Object o = this.rsm.createProxyService(refField, item.getNamespace(),item.getVersion(),true);
				if(o!=null){
					set.add(o);
				}
			}else if(IServiceListener.SERVICE_REMOVE == type) {
				AbstractServiceProxy po = null;
				for(Object o: set){
					AbstractServiceProxy p = (AbstractServiceProxy)o;
					if(p.key().equals(item.serviceName())){
						po = p;
						break;
					}
				}
				if(po!= null){
					po.enable(false);
				}
			}
		}else if(List.class.isAssignableFrom(refField.getType())){
			List<Object> set = (List<Object>)this.proxy;
			
			if(IServiceListener.SERVICE_ADD == type){
				for(Object o: set){
					AbstractServiceProxy p = (AbstractServiceProxy)o;
					if(p.key().equals(item.serviceName())){
						p.enable(true);
						return;
					}
				}
				Object o = this.rsm.createProxyService(refField, item.getNamespace(),item.getVersion(),true);
				if(o!=null){
					set.add(o);
				}
			}else if(IServiceListener.SERVICE_REMOVE == type) {
				AbstractServiceProxy po = null;
				for(Object o: set){
					AbstractServiceProxy p = (AbstractServiceProxy)o;
					if(p.key().equals(item.serviceName())){
						po = p;
						break;
					}
				}
				if(po!= null){
					po.enable(false);
				}
				
			}
		}
		
		
	}
	
	
}
