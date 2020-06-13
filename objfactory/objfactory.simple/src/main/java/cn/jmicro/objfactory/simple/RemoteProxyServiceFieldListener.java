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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxy;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.common.CommonException;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月6日-下午12:08:48
 */
class RemoteProxyServiceFieldListener implements IServiceListener{

	private final static Logger logger = LoggerFactory.getLogger(RemoteProxyServiceFieldListener.class);
	
	private Object srcObj;
	
	private Object refFieldVal;
	
	private Field refField;
	
	private ClientServiceProxyManager rsm;
	
	private Reference ref = null;
	
	private Class<?> srvType = null;
	
	private IRegistry registry = null;
	
	/**
	 * 
	 * @param rsm
	 * @param proxy 可以是直接的代理服务对象，也可以是集合对像，当是集合对像时，集合对象的元素必须是代理服务地像
	 * @param srcObj 引用代理对象或集合对象的对象，当代理对象或集合对象里的代理对像发生改变时，将收到通知
	 * @param refField 代理对象或集合对象的类型声明字段，属于srcObj的成员
	 */
	RemoteProxyServiceFieldListener(ClientServiceProxyManager rsm,Object valProxy,Object srcObj
			,Field refField,IRegistry registry){
		if(valProxy== null){
			throw new CommonException("Proxy object cannot be null: "+ refField.getDeclaringClass().getName()+",field: " + refField.getName());
		}
		this.rsm = rsm;
		this.refFieldVal = valProxy;
		
		this.srcObj = srcObj;
		this.refField = refField;
		this.ref = refField.getAnnotation(Reference.class);
		srvType = rsm.getEltType(refField);
		this.registry = registry;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void serviceChanged(int type, ServiceItem item) {
		
		if(!item.getKey().getServiceName().equals(srvType.getName())) {
			return;
		}
		
		if(!UniqueServiceKey.matchVersion(ref.version(),item.getKey().getVersion()) || 
				!UniqueServiceKey.matchNamespace(ref.namespace(),item.getKey().getNamespace())) {
				return;
		}
		
		if(Set.class.isAssignableFrom(refField.getType()) 
				|| List.class.isAssignableFrom(refField.getType())){
			Collection<Object> set = (Collection<Object>)this.refFieldVal;
			
			if(IServiceListener.ADD == type){
				boolean direct = "ins".equals(ref.type());
				
				if(direct) {
					String ekey = item.getKey().toKey(true, true, true);
					Iterator<Object> ite = set.iterator();
					for(;ite.hasNext();){
						AbstractClientServiceProxy p = (AbstractClientServiceProxy)ite.next();
						if(p.getItem() == null) {
							ite.remove();
						} else {
							if(p.getItem().getKey().toKey(true, true, true).equals(ekey)){
								//服务代理已经存在,不需要重新创建
								return;
							}
						}
					}
				} else {
					String ekey = item.serviceKey();
					for(Object o: set){
						AbstractClientServiceProxy p = (AbstractClientServiceProxy)o;
						if(p.serviceKey().equals(ekey)){
							//服务代理已经存在,不需要重新创建
							return;
						}
					}
				}
				
				AsyncConfig[] acs = this.rsm.getAcs(this.ref);
				
				//代理还不存在，创建之
				AbstractClientServiceProxy p = (AbstractClientServiceProxy)this.rsm.getRefRemoteService(item, null,acs);
				if(p!=null){
					p.setDirect(direct);
					set.add(p);
					logger.debug("Add proxy for,Size:{} Field:{},Item:{}",set.size(),
							refField.toString(),item.getKey().toKey(false, false, false));
					//通知组件服务元素增加
					notifyChange(p,type);
				} else {
					String msg = "Fail to create item proxy: " + item.getKey().toKey(true, true, true);
					SF.doBussinessLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, RemoteProxyServiceFieldListener.class, null, msg);
					logger.error("Fail to create item proxy :{}",msg);
				}
				
			}else if(IServiceListener.REMOVE == type) {
				
				boolean direct = "ins".equals(ref.type());
				if(!direct) {
					boolean exist = registry.isExists(item.getKey().getServiceName(), item.getKey().getNamespace(), item.getKey().getVersion());
					if(!exist) {
						//服务已经不存在
						AbstractClientServiceProxy po = null;
						for(Object o: set){
							AbstractClientServiceProxy p = (AbstractClientServiceProxy)o;
							if(p.serviceKey().equals(item.serviceKey())){
								po = p;
								break;
							}
						}
						if(po != null){
							set.remove(po);
							logger.debug("Remove proxy for,Size:{}, Field:{},Item:{}",set.size(),refField.toString(),
									item.getKey().toKey(false, false, false));
							//通知组件服务元素删除
							notifyChange(po,type);
						}
					}
				} else {
					//服务已经不存在
					AbstractClientServiceProxy po = null;
					String k= item.getKey().toKey(true, true, true);
					for(Object o: set){
						AbstractClientServiceProxy p = (AbstractClientServiceProxy)o;
						if(p.getItem() != null && k.equals(p.getItem().getKey().toKey(true, true, true))){
							po = p;
						}
					}
					if(po != null){
						set.remove(po);
						logger.debug("Remove proxy for,Size:{}, Field:{},Item:{}",set.size(),refField.toString(),
								item.getKey().toKey(false, false, false));
						//通知组件服务元素删除
						notifyChange(po,type);
					}
				
				}
				
			}
		}
	}
	
	protected void notifyChange(AbstractClientServiceProxy po,int opType) {
		Reference cfg = this.refField.getAnnotation(Reference.class);
		if(cfg == null || cfg.changeListener()== null || cfg.changeListener().trim().equals("")){
			return;
		}
		Method m =  null;
		Class<?> cls = ProxyObject.getTargetCls(this.refField.getDeclaringClass());
		try {
			 m =  cls.getMethod(cfg.changeListener(),new Class[]{AbstractClientServiceProxy.class,Integer.TYPE} );
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
				SF.doBussinessLog(MC.MT_PLATFORM_LOG,MC.LOG_ERROR, RemoteProxyServiceFieldListener.class, e, "Listener method ["+cfg.changeListener()+"] not found!");
			}
		}
		
	}

	
}
