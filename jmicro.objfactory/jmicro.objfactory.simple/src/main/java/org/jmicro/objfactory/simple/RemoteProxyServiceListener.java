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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jmicro.api.annotation.Reference;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.CommonException;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月6日-下午12:08:48
 */
class RemoteProxyServiceListener implements IServiceListener{

	private Object srcObj;
	private Object proxy;
	private Field refField;
	
	private ClientServiceProxyManager rsm;
	
	RemoteProxyServiceListener(ClientServiceProxyManager rsm,Object proxy,Object srcObj,Field refField){
		if(proxy== null){
			throw new CommonException("Proxy object cannot be null: "+ refField.getDeclaringClass().getName()+",field: " + refField.getName());
		}
		this.rsm = rsm;
		this.proxy = proxy;
		this.srcObj = srcObj;
		this.refField = refField;
	}

	@Override
	public void serviceChanged(int type, ServiceItem item) {
		
		if(proxy instanceof AbstractClientServiceProxy) {
			AbstractClientServiceProxy p = (AbstractClientServiceProxy)proxy;
			if(!p.key().equals(item.serviceName())){
				throw new CommonException("Service listener give error service item:"+ 
						refField.getDeclaringClass().getName()+"],field: " + refField.getName()+" item:"+item.val());
			}
			if(IServiceListener.SERVICE_ADD == type){
				//p.enable(true);
				p.setItem(item);
			}else if(IServiceListener.SERVICE_REMOVE == type) {
				p.setItem(null);
			}else if(IServiceListener.SERVICE_DATA_CHANGE == type) {
				p.setItem(item);
			}
			notifyChange();
		}else if(Set.class.isAssignableFrom(refField.getType()) 
				|| List.class.isAssignableFrom(refField.getType())){
			Collection<Object> set = (Collection<Object>)this.proxy;
			
			if(IServiceListener.SERVICE_ADD == type){
				for(Object o: set){
					AbstractClientServiceProxy p = (AbstractClientServiceProxy)o;
					if(p.key().equals(item.serviceName())){
						//p.enable(true);
						p.setItem(item);
						return;
					}
				}
				Object o = this.rsm.createOrGetProxyService(refField, item,this.srcObj);
				if(o!=null){
					set.add(o);
				}
			}else if(IServiceListener.SERVICE_REMOVE == type) {
				AbstractClientServiceProxy po = null;
				for(Object o: set){
					AbstractClientServiceProxy p = (AbstractClientServiceProxy)o;
					if(p.key().equals(item.serviceName())){
						po = p;
						break;
					}
				}
				if(po != null){
					po.setItem(null);
					set.remove(po);
				}
			}else if(IServiceListener.SERVICE_DATA_CHANGE == type) {
				AbstractClientServiceProxy po = null;
				for(Object o: set){
					AbstractClientServiceProxy p = (AbstractClientServiceProxy)o;
					if(p.key().equals(item.serviceName())){
						po = p;
						break;
					}
				}
				if(po!= null){
					//po.enable(true);
					po.setItem(item);
				}
			}
			notifyChange();
		}
	}
	
	protected void notifyChange() {
		Reference cfg = this.refField.getAnnotation(Reference.class);
		if(cfg == null || cfg.changeListener()== null || cfg.changeListener().trim().equals("")){
			return;
		}
		Method m =  null;
		Class<?> cls = ProxyObject.getTargetCls(this.refField.getDeclaringClass());
		try {
			 m =  cls.getMethod(cfg.changeListener(),new Class[]{String.class} );
			 if(m != null){
				 m.invoke(this.srcObj,refField.getName());
			 }
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			System.out.println(e); 
			try {
				m =  cls.getMethod(cfg.changeListener(),new Class[0] );
				if(m != null){
					 m.invoke(this.srcObj);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				System.out.println(e1);
			}
		}
		
	}

	
}
