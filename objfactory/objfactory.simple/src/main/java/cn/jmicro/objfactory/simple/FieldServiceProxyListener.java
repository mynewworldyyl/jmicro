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
package cn.jmicro.objfactory.simple;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.UniqueServiceKey;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月6日-下午12:08:48
 */
class FieldServiceProxyListener implements IServiceListener{

	private final static Logger logger = LoggerFactory.getLogger(FieldServiceProxyListener.class);
	
	private Object srcObj;
	
	private Field refField;
	
	private ClientServiceProxyManager rsm;
	
	private Reference ref = null;
	
	private Class<?> srvType = null;
	
	private IRegistry registry;
	
	/**
	 * 
	 * @param rsm
	 * @param proxy 可以是直接的代理服务对象，也可以是集合对像，当是集合对像时，集合对象的元素必须是代理服务地像
	 * @param srcObj 引用代理对象或集合对象的对象，当代理对象或集合对象里的代理对像发生改变时，将收到通知
	 * @param refField 代理对象或集合对象的类型声明字段，属于srcObj的成员
	 */
	FieldServiceProxyListener(ClientServiceProxyManager rsm,Object srcObj
			,Field refField,IRegistry registry){
		this.rsm = rsm;
		this.srcObj = srcObj;
		this.refField = refField;
		this.ref = refField.getAnnotation(Reference.class);
		srvType = rsm.getEltType(refField);
		this.registry = registry;
	}


	@Override
	public void serviceChanged(int type, ServiceItem item) {
		
		if(!item.getKey().getServiceName().equals(srvType.getName())) {
			return;
		}
		
		if(!UniqueServiceKey.matchVersion(ref.version(),item.getKey().getVersion()) || 
				!UniqueServiceKey.matchNamespace(ref.namespace(),item.getKey().getNamespace())) {
				return;
		}
		
		if(!Set.class.isAssignableFrom(refField.getType()) &&
				!List.class.isAssignableFrom(refField.getType())){
			
			if(IServiceListener.ADD == type){
				AsyncConfig[] acs = this.rsm.getAcs(this.ref);
				boolean bf = refField.isAccessible();
				Object o = null;
				if(!bf) {
					refField.setAccessible(true);
				}
				try {
					o = refField.get(srcObj);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					String msg = "Class ["+srcObj.getClass().getName()+"] field ["+ refField.getName()+"] dependency ["+refField.getType().getName()+"] not found";
					logger.error(msg);
					LG.log(MC.LOG_ERROR, FieldServiceProxyListener.class, msg, e);
					return;
				}
				if(!bf) {
					refField.setAccessible(bf);
				}
				
				AbstractClientServiceProxyHolder p = (AbstractClientServiceProxyHolder)o;
				if(o == null) {
					//代理还不存在，创建之
					 p = (AbstractClientServiceProxyHolder)this.rsm.getRefRemoteService(item, null,acs);
					 if(p != null) {
							SimpleObjectFactory.setObjectVal(srcObj, refField, p);
							notifyChange(p,type);
						} else {
							String msg = "Fail to create service "+item.getKey().toKey(true, true, true)+" for Class ["+srcObj.getClass().getName()+"] field ["+ refField.getName()+"] dependency ["+refField.getType().getName()+"]";
							if(ref.required()) {
								LG.log(MC.LOG_ERROR, FieldServiceProxyListener.class, msg);
								logger.error(msg);
							}else {
								LG.log(MC.LOG_WARN, FieldServiceProxyListener.class, msg);
								logger.warn(msg);
							}
							return;
						}
				} else {
					notifyChange(p,type);
					p.getHolder().setItem(item);
					p.getHolder().setAsyncConfig(acs);
				}
			}else if(IServiceListener.REMOVE == type) {

				if(ref.required()) {
					String msg = "Class ["+srcObj.getClass().getName()+"] field ["+ refField.getName()+"] dependency ["+refField.getType().getName()+"] offline";
					logger.error(msg);
					LG.log(MC.LOG_WARN, FieldServiceProxyListener.class, msg);
				}else {
					String msg = "Class ["+srcObj.getClass().getName()+"] field ["+ refField.getName()+"] dependency ["+refField.getType().getName()+"] offline";
					LG.log(MC.LOG_WARN, FieldServiceProxyListener.class, msg);
					logger.warn(msg);
				}
				notifyChange(null,type);
				SimpleObjectFactory.setObjectVal(srcObj, refField, null);
			}
		}
		
	}
	
	protected void notifyChange(AbstractClientServiceProxyHolder po,int opType) {
		Reference cfg = this.refField.getAnnotation(Reference.class);
		if(cfg == null || cfg.changeListener()== null || cfg.changeListener().trim().equals("")){
			return;
		}
		Method m =  null;
		Class<?> cls = ProxyObject.getTargetCls(this.refField.getDeclaringClass());
		try {
			 m =  cls.getMethod(cfg.changeListener(),new Class[]{AbstractClientServiceProxyHolder.class,Integer.TYPE} );
			 if(m != null){
				 m.invoke(this.srcObj,po,opType);
			 }
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			//System.out.println(e); 
			try {
				m =  cls.getMethod(cfg.changeListener(),new Class[0] );
				if(m != null){
					 m.invoke(this.srcObj);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				//System.out.println(e1);
				logger.error("",e);
				LG.log(MC.LOG_ERROR, RemoteProxyServiceFieldListener.class, "Listener method ["+cfg.changeListener()+"] not found!", e);
			}
		}
		
	}

	
}
