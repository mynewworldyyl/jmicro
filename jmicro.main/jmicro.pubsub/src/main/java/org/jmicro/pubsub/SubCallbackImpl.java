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
package org.jmicro.pubsub;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.Message;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.registry.AsyncConfig;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:09:47
 */
public class SubCallbackImpl implements ISubCallback{

	private static final Class TAG = SubCallbackImpl.class;
	
	//接收PSData数组作为参数，同一主题批量数据传输，效率高
	private static final int ARR = 1;
	//接收单个PSData作为参数，效率底
	private static final int SINGLE = 2;
	//接收PSData.data作为参数，如异步RPC
	private static final int DATA = 3;
	//无参数
	private static final int NONE = 4;
	
	private final static Logger logger = LoggerFactory.getLogger(SubCallbackImpl.class);
	
	private ServiceMethod sm = null;
	
	private Object srvProxy = null;
	
	private Method m = null;
	
	private IObjectFactory of;
	
	private IRegistry reg;
	
	private Map<String,Holder> key2Holder = new HashMap<>();
	
	//方法参数模式
	private int type;
	
	public SubCallbackImpl(ServiceMethod sm,Object srv, IObjectFactory of){
		if(sm == null) {
			throw new CommonException("SubCallback service method cannot be null");
		}
		
		if(srv == null) {
			throw new CommonException("SubCallback service cannot be null");
		}
		this.of = of;
		this.sm = sm;
		this.srvProxy = srv;
		this.reg = of.get(IRegistry.class);
		
		setMt();
	}
	
	@Override
	public PSData[] onMessage(PSData[] items) {
		switch(type) {
		case ARR:
			//PSData数组作为参数
			return callAsArra(items);
		case DATA:
			////以每个PSData.data作为参数调用主题方法，如异步RPC
			return callAsyncRpc(items);
		case SINGLE:
			//接收单个PSData作为参数的RPC，效率底
			return callOneByOne(items);
		case NONE:
			//接收单个PSData作为参数的RPC，效率底
			return callNone(items);
		}
		throw new CommonException("onMessage topic:"+sm.getTopic(),sm.getKey().toKey(false, false, false));
	}

	private PSData[] callAsArra(PSData[] items) {
		PSData[] fails = null;
		try {
			//多个消息作为整体发送，没办法实现结果回调通知，因为回调信息放置于PSData.context中，多个items,没办法确定使用那个
			Object obj = m.invoke(this.srvProxy, new Object[] {items});
			List<PSData> fs = notifyResult(obj,items);
			if(fs != null && !fs.isEmpty()) {
				fails = new PSData[fs.size()];
				fs.toArray(fails);
			}
			return fails;
		} catch (Throwable e) {
			String msg = "callAsArra topic:"+sm.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
			logger.error(msg, e);
			SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, e, msg);
			return items;
		}
	}

	private List<PSData> notifyResult(Object obj, PSData[] items) {
		List<PSData> fails = new ArrayList<>();
		for (PSData pd : items) {
			try {
				if (pd.getCallback() != null) {
					callback(pd, obj,PubSubManager.PUB_OK);
				}
			} catch (Throwable e) {
				String msg = "callOneByOne pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
				logger.error(msg, e);
				SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, e, msg);
				fails.add(pd);
			}
		}
		return fails;
	}

	private PSData[] callOneByOne(PSData[] items) {
		List<PSData> fails = new ArrayList<>();
		for (PSData pd : items) {
			try {
				Object obj = m.invoke(this.srvProxy, pd);;
				callback(pd, obj,PubSubManager.PUB_OK);
			} catch (Throwable e) {
				String msg = "callOneByOne pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
				logger.error(msg, e);
				SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, e, msg);
				fails.add(pd);
			}
		}
		if(!fails.isEmpty()) {
			PSData[] pds = new PSData[fails.size()];
			fails.toArray(pds);
			return pds;
		}
		return null;
	}
	
	private PSData[] callNone(PSData[] items) {

		List<PSData> fails = new ArrayList<>();
		
		for (PSData pd : items) {
			try {
				Object obj = null;
				obj = m.invoke(this.srvProxy, new Object[0]);
				callback(pd, obj,PubSubManager.PUB_OK);
			} catch (Throwable e) {
				String msg = "callAsyncRpc pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
				logger.error(msg, e);
				SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, e, msg);
				fails.add(pd);
			}
		}
		if(!fails.isEmpty()) {
			PSData[] pds = new PSData[fails.size()];
			fails.toArray(pds);
			return pds;
		}
		return null;
	}

	private PSData[] callAsyncRpc(PSData[] items) {

		List<PSData> fails = new ArrayList<>();
		
		for (PSData pd : items) {
			try {
				Object obj = null;
				Object[] args = (Object[]) pd.getData();
				obj = m.invoke(this.srvProxy, args);
				callback(pd, obj,PubSubManager.PUB_OK);
			} catch (Throwable e) {
				String msg = "callAsyncRpc pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
				logger.error(msg, e);
				SF.doBussinessLog(MonitorConstant.LOG_ERROR, TAG, e, msg);
				fails.add(pd);
			}
		}
		if(!fails.isEmpty()) {
			PSData[] pds = new PSData[fails.size()];
			fails.toArray(pds);
			return pds;
		}
		return null;
	}

	//异步回调用返回值，如异步RPC时，返回结果给调用者
	public boolean callback(PSData item,Object obj,int statuCode) {

		if (item.getCallback() == null) {
			return true;
		}
		
		Map<String,Object> cxt = item.getContext();
	
		Long linkId = (Long)cxt.get(JMicroContext.LINKER_ID);
		
		UniqueServiceMethodKey key = item.getCallback();
		
		try {
			
			Holder h = null;
			if(this.key2Holder.containsKey(key)) {
				h = this.key2Holder.get(key);
			} else {
				h = new Holder();
				h.srv  = of.getRemoteServie(key.getServiceName(),key.getNamespace(),key.getVersion(),null,null);
				if(h.srv == null) {
					String msg = "Fail to create async service proxy src:" + sm.getKey().toString()+",target:"+ key.toKey(false, false, false);
					SF.doBussinessLog(MonitorConstant.LOG_ERROR,SubCallbackImpl.class,null, msg);
					//即使返回false重发此条消息，也是同样的错误，没办法回调了，记录日志，只能通过人工处理
					return true;
				}
				h.key = key;
				
				if(Message.is(item.getFlag(), PSData.FLAG_ASYNC_METHOD)) {
					//异步方法
					h.m = h.srv.getClass().getMethod(key.getMethod(),obj.getClass());
				}else if(Message.is(item.getFlag(), PSData.FLAG_MESSAGE_CALLBACK)) {
					//消息通知
					h.m = h.srv.getClass().getMethod(key.getMethod(),Integer.TYPE,Long.TYPE,Map.class);
				}
				
				if(h.m == null) {
					String msg = "Async service method not found: src:" + sm.getKey().toString()+",target:"+ key.toKey(false, false, false);
					SF.doBussinessLog(MonitorConstant.LOG_ERROR,SubCallbackImpl.class,null, msg);
					//即使返回false重发此条消息，也是同样的错误，没办法回调了，记录日志，只能通过人工处理
					return true;
				}
			
			}

			//JMicroContext.get().setParam(key, val);
			JMicroContext.get().setLong(JMicroContext.LINKER_ID, linkId);
			//JMicroContext.get().setLong(JMicroContext.REQ_ID, reqId);
			if(Message.is(item.getFlag(), PSData.FLAG_ASYNC_METHOD)) {
				//异步方法
				h.m.invoke(h.srv, obj);
			}else if(Message.is(item.getFlag(), PSData.FLAG_MESSAGE_CALLBACK)) {
				//消息通知
				h.m.invoke(h.srv, statuCode,item.getId(),item.getContext());
			}
			return true;
		
		} catch (Throwable e) {
			String msg = "Fail to callback src service:" + sm.getKey().toString()+ ",c allback: "+ key.toKey(false, false, false);
			SF.doBussinessLog(MonitorConstant.LOG_ERROR,SubCallbackImpl.class,e, msg);
			logger.error("",e);
			return false;
		}
	
	}

	private void setMt() {
		try {
			Class<?>[] argsCls = UniqueServiceMethodKey.paramsClazzes(sm.getKey().getParamsStr());
			this.m = this.srvProxy.getClass().getMethod(sm.getKey().getMethod(), argsCls);
			if(argsCls == null || argsCls.length ==0) {
				this.type = NONE;
			}else if(argsCls.length ==1 && argsCls[0] == PSData.class ) {
				this.type = SINGLE;
			}else if(argsCls.length == 1 && argsCls[0] == new PSData[0].getClass() ) {
				this.type = ARR;
			}else {
				this.type = DATA;
			}
		} catch (NoSuchMethodException | SecurityException e) {
		}
	}
	
	private class Holder{
		public Object srv;
		public Method m;
		public UniqueServiceMethodKey key;
		
	}

	@Override
	public String info() {
		return sm.getKey().toKey(false, false, false);
	}

	@Override
	public String toString() {
		return info();
	}

	@Override
	public int hashCode() {
		return info().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}

	public ServiceMethod getSm() {
		return sm;
	}

	public void setSm(ServiceMethod sm) {
		this.sm = sm;
	}
	
}
