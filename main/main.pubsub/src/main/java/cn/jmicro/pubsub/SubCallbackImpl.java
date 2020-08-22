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
package cn.jmicro.pubsub;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.CommonException;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:09:47
 */
public class SubCallbackImpl implements ISubCallback{

	private static final Class<?> TAG = SubCallbackImpl.class;
	
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
		
	}
	
	@Override
	public IPromise<PSData[]> onMessage(PSData[] items) {
		switch(type) {
			case ARR:
				//PSData数组作为参数
				return callAsArra(items);
			case DATA:
			case SINGLE:
			case NONE:
				return callOneByOne(items,type);
		}
		throw new CommonException("onMessage topic:"+sm.getTopic()+", type: " + type, sm.getKey().toKey(false, false, false));
	}

	private IPromise<PSData[]> callAsArra(PSData[] items) {
		PromiseImpl<PSData[]> p = new PromiseImpl<>();
		try {
			//多个消息作为整体发送，没办法实现结果回调通知，因为回调信息放置于PSData.context中，多个items,没办法确定使用那个
			PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), null,  new Object[] {items}) //m.invoke(this.srvProxy, new Object[] {items});
			.then((obj,fail,ctx)-> {
				if(fail == null) {
					IPromise<List<PSData>>  fsPro = notifyResult(obj,items);
					fsPro.then((fs,fa,actx)->{
						if(fs != null && !fs.isEmpty()) {
							PSData[] failItems = new PSData[fs.size()];
							fs.toArray(failItems);
							p.setResult(failItems);
						}else {
							p.setResult(null);
						}
						p.done();
					});
				} else {
					p.setFail(fail);
					p.done();
				}
			});
			return p;
		} catch (Throwable e) {
			String msg = "callAsArra topic:"+sm.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
			logger.error(msg, e);
			SF.eventLog(MC.MT_PUBSUB_LOG,MC.LOG_ERROR, TAG,msg,e);
			p.setResult(items);
			p.setFail(1,msg);
			p.done();
		}
		return p;
	}

	private IPromise<List<PSData>> notifyResult(Object obj, PSData[] items) {
		PromiseImpl<List<PSData>> outp = new PromiseImpl<>();
		
		List<PSData> fails = new ArrayList<>();
		
		AtomicInteger cbcnt = new AtomicInteger(0);
		
		for (PSData pd : items) {
			if(pd.getCallback() != null) {
				cbcnt.incrementAndGet();
			}
		}
		
		if(cbcnt.get() == 0) {
			outp.setResult(null);
			outp.done();
		}else {
			for (PSData pd : items) {
				try {
					if(pd.getCallback() != null) {
						IPromise<PSData> pro = callback(pd, obj, PubSubManager.PUB_OK);
						pro.then((proData,f,ctx)->{
							if(proData != null) {
								fails.add(proData);
								logger.error(f.toString());
							}
							int cnt = cbcnt.decrementAndGet();
							if(cnt == 0) {
								//全部通知返回结束
								outp.setResult(fails);
								outp.setDone(true);
							}
						});
					}
				} catch (Throwable e) {
					String msg = "callOneByOne pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
					logger.error(msg, e);
					SF.eventLog(MC.MT_PUBSUB_LOG,MC.LOG_ERROR, TAG, msg,e);
					fails.add(pd);
				}
			}
		}
		
		return outp;
	}

	private IPromise<PSData[]> callOneByOne(PSData[] items,int type) {
		
		PromiseImpl<PSData[]> p = new PromiseImpl<>();
		p.setResult(null);
		
		List<PSData> fails = new ArrayList<>();
		
		AtomicInteger ai = new AtomicInteger(items.length);
		
		for (PSData pd : items) {
			try {
				
				IPromise<?> rePromise = null;
				if(type == SINGLE) {
					rePromise = PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), null,  new Object[] {pd});
					//rePromise = (IPromise<?>)m.invoke(this.srvProxy, pd);
				} else if(type == NONE){
					rePromise = PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), null,  new Object[0]);
					//rePromise = (IPromise<?>)m.invoke(this.srvProxy, new Object[0]);
				} else if(type == DATA) {
					Object[] args = (Object[]) pd.getData();
					rePromise = PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), null,  args);
					//rePromise = (IPromise<?>)m.invoke(this.srvProxy, args);
				}
				
				rePromise.then((obj,fail,ctx)->{
					if(fail == null && pd.getCallback() != null) {
						IPromise<PSData> inPro = callback(pd, obj,PubSubManager.PUB_OK);
						inPro.then((iobj,ifail,actx)->{
							int cnt = ai.decrementAndGet();
							if(cnt == 0) {
								if(!fails.isEmpty()) {
									PSData[] pds = new PSData[fails.size()];
									fails.toArray(pds);
									p.setResult(pds);
									p.setFail(1, "fail item in result");
								}
								p.done();
							}
						});
					} else {
						logger.error(fail.toString());
						fails.add(pd);
						int cnt = ai.decrementAndGet();
						if(cnt == 0) {
							if(!fails.isEmpty()) {
								PSData[] pds = new PSData[fails.size()];
								fails.toArray(pds);
								p.setResult(pds);
								p.setFail(1, "fail item in result");
							}
							p.done();
						}
					}
				});
			} catch (Throwable e) {
				String msg = "callOneByOne pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().toKey(false, false, false);
				logger.error(msg, e);
				SF.eventLog(MC.MT_PUBSUB_LOG,MC.LOG_ERROR, TAG,msg,e);
				fails.add(pd);
				
				int cnt = ai.decrementAndGet();
				
				if(cnt == 0) {
					if(!fails.isEmpty()) {
						PSData[] pds = new PSData[fails.size()];
						fails.toArray(pds);
						p.setResult(pds);
						p.setFail(1, "fail item in result");
					}
					p.done();
				}
				
			}
		}
		return p;
	}

	//异步回调用返回值，如异步RPC时，返回结果给调用者
	public IPromise<PSData> callback(PSData item,Object obj,int statuCode) {

		PromiseImpl<PSData> p = new PromiseImpl<>();
		p.setResult(null);
		
		if (item.getCallback() == null) {
			p.setFail(-1, "callback is null");
			p.done();
			return p;
		}
		
		Map<String,Object> cxt = item.getContext();
	
		Long linkId = (Long)cxt.get(JMicroContext.LINKER_ID);
		
		UniqueServiceMethodKey key = item.getCallback();
		
		try {
			
			Holder h = null;
			String k = key.toKey(false, false, false);
			if(this.key2Holder.containsKey(k)) {
				h = this.key2Holder.get(k);
			} else {
				h = new Holder();
				h.srv  = of.getRemoteServie(key.getServiceName(),key.getNamespace(),key.getVersion(),null);
				if(h.srv == null) {
					String msg = "Fail to create async service proxy src:" + sm.getKey().toString()+",target:"+ key.toKey(false, false, false);
					SF.eventLog(MC.MT_PUBSUB_LOG,MC.LOG_ERROR,SubCallbackImpl.class,msg);
					//即使返回false重发此条消息，也是同样的错误，没办法回调了，记录日志，只能通过人工处理
					p.setFail(1, msg);
					p.done();
					return p;
				}
				h.key = key;
				
				if(Message.is(item.getFlag(), PSData.FLAG_ASYNC_METHOD)) {
					//异步方法
					h.m = h.srv.getClass().getMethod(AsyncClientUtils.genAsyncMethodName(key.getMethod()),obj.getClass());
				}else if(Message.is(item.getFlag(), PSData.FLAG_MESSAGE_CALLBACK)) {
					//消息通知
					h.m = h.srv.getClass().getMethod(AsyncClientUtils.genAsyncMethodName(key.getMethod()),Integer.TYPE,Long.TYPE,Map.class);
				}
				
				if(h.m == null) {
					String msg = "Async service method not found: src:" + sm.getKey().toString()+",target:"+ key.toKey(false, false, false);
					SF.eventLog(MC.MT_PUBSUB_LOG,MC.LOG_ERROR,SubCallbackImpl.class, msg);
					//即使返回false重发此条消息，也是同样的错误，没办法回调了，记录日志，只能通过人工处理
					p.setFail(2, msg);
					return p;
				}
				
				key2Holder.put(k, h);
				
			}
			
			//JMicroContext.get().setParam(key, val);
			JMicroContext.get().setLong(JMicroContext.LINKER_ID, linkId);
			//JMicroContext.get().setLong(JMicroContext.REQ_ID, reqId);
			IPromise<?> cp = null;
			if(Message.is(item.getFlag(), PSData.FLAG_ASYNC_METHOD)) {
				//异步方法
				cp = PromiseUtils.callService(h.srv, h.key.getMethod(), null,  new Object[] {obj});
				//cp = (IPromise<?>)h.m.invoke(h.srv, obj);
			}else if(Message.is(item.getFlag(), PSData.FLAG_MESSAGE_CALLBACK)) {
				//消息通知
				cp = PromiseUtils.callService(h.srv, h.key.getMethod(), null,  
						new Object[] {statuCode,item.getId(),item.getContext()});
				//cp = (IPromise<?>)h.m.invoke(h.srv, statuCode,item.getId(),item.getContext());
			}
			
			if(cp == null) {
				p.setFail(3, "Invkke error: " + k);
				p.setResult(item);
			} else {
				cp.then((c,fai,actx)->{
					if(fai != null) {
						p.setResult(item);
						p.setFail(fai);
					}
				});
			}
			p.setDone(true);
			return p;
		} catch (Throwable e) {
			String msg = "Fail to callback src service:" + sm.getKey().toString()+ ",c allback: "+ key.toKey(false, false, false);
			SF.eventLog(MC.MT_PUBSUB_LOG,MC.LOG_ERROR,SubCallbackImpl.class, msg,e);
			logger.error("",e);
			p.setResult(item);
			p.setFail(5, msg);
			return p;
		}
	
	}

	public void init() {
		try {
			Class<?>[] argsCls = UniqueServiceMethodKey.paramsClazzes(sm.getKey().getParamsStr());
			String method = AsyncClientUtils.genAsyncMethodName(sm.getKey().getMethod());
			this.m = AsyncClientUtils.getMethod(this.srvProxy.getClass(), method);
			if(argsCls == null || argsCls.length ==0) {
				//无参数
				this.type = NONE;
			}else if(argsCls.length ==1 && argsCls[0] == PSData.class ) {
				//PSData实例作为单一参数
				this.type = SINGLE;
			}else if(argsCls.length == 1 && argsCls[0] == new PSData[0].getClass() ) {
				//PSData实例数组作为参数
				this.type = ARR;
			} else {
				//PSData.data数组作为RPC参数
				this.type = DATA;
			}
		} catch (Throwable e) {
			logger.error("init error: "+sm.getKey() + ", error " + e.getMessage());
			throw new CommonException("",e);
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
