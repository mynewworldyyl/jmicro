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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.internal.pubsub.IInternalSubRpcJMSrv;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:09:47
 */
public class SubscriberCallbackImpl implements ISubscriberCallback{

	private static final Class<?> TAG = SubscriberCallbackImpl.class;
	
	//接收PSData数组作为参数，同一主题批量数据传输，效率高
	private static final int ARR = 1;
	//接收单个PSData作为参数，效率底
	private static final int SINGLE = 2;
	//接收PSData.data作为参数，如异步RPC
	private static final int DATA = 3;
	//无参数
	private static final int NONE = 4;
	
	private final static Logger logger = LoggerFactory.getLogger(SubscriberCallbackImpl.class);
	
	private ServiceMethodJRso sm = null;
	
	private Object srvProxy = null;
	
	//private Method m = null;
	
	private IObjectFactory of;
	
	//private IRegistry reg;
	
	private IObjectStorage os;
	
	private Map<String,Holder> key2Holder = new HashMap<>();
	
	private IInternalSubRpcJMSrv pubsubServer;
	
	//方法参数模式
	private int type;
	
	public SubscriberCallbackImpl(ServiceMethodJRso sm, Object srv, IObjectFactory of){
		if(sm == null) {
			throw new CommonException("SubCallback service method cannot be null");
		}
		
		if(srv == null) {
			throw new CommonException("SubCallback service cannot be null");
		}
		this.of = of;
		this.os = of.get(IObjectStorage.class);
		this.sm = sm;
		this.srvProxy = srv;
		//this.reg = of.get(IRegistry.class);
		
		this.pubsubServer = of.get(IInternalSubRpcJMSrv.class);
		
	}
	
	@Override
	public IPromise<PSDataJRso[]> onMessage(PSDataJRso[] items) {
		switch(type) {
			case ARR:
				//PSData数组作为参数
				return callAsArra(items);
			case DATA:
			case SINGLE:
			case NONE:
				return callOneByOne(items,type);
		}
		throw new CommonException(0,"onMessage topic:"+sm.getTopic()+", type: " + type +"," + sm.getKey().methodID());
	}
	
	private void notiryResultFail(int code,String msg,Object cxt,List<PSDataJRso> fs,Promise<PSDataJRso[]> p) {

		//List<PSDataJRso> fs = fsPro.getResult();
		PSDataJRso[] items = (PSDataJRso[])cxt;
		if(fs != null && !fs.isEmpty()) {
			PSDataJRso[] failItems = new PSDataJRso[fs.size()];
			fs.toArray(failItems);
			p.setResult(failItems);
			resultItem(failItems,PSDataJRso.RESULT_FAIL_CALLBACK);
			//部份数据回调成功情况
			PSDataJRso[] successItems = new PSDataJRso[items.length-fs.size()];
			int idx = 0;
			if(fs.size() < items.length) {
				//失败数不可能大于发送数
				for(PSDataJRso pd : items) {
					boolean f = false;
					for(PSDataJRso fpd : fs) {
						if(pd.getId() == fpd.getId()) {
							f = true;
							continue;
						}
					}
					if(!f) {
						successItems[idx++] = pd;
					}
				}
				resultItem(successItems,PSDataJRso.RESULT_SUCCCESS);
			}
			
		} else {
			//全部成功
			resultItem(items,PSDataJRso.RESULT_SUCCCESS);
			p.setResult(null);
		}
	}

	private IPromise<PSDataJRso[]> callAsArra(PSDataJRso[] items) {
		Promise<PSDataJRso[]> p = new Promise<>();
		try {
			//多个消息作为整体发送
			PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), items,  new Object[] {items}) //m.invoke(this.srvProxy, new Object[] {items});
			.success((obj,ctx)-> {
				//回调结果通知
				IPromise<List<PSDataJRso>>  fsPro = notifyResult(obj,items,PSDataJRso.PUB_OK);
				fsPro
				.fail((code,msg,cxt)->{
					notiryResultFail(code,msg,cxt,fsPro.getResult(),p);
					p.setFail(code, msg);
					p.done();
				})
				.success((rst,is)->{
					resultItem(items,PSDataJRso.RESULT_SUCCCESS);
					p.done();
				});
			})
			.fail((code,msg,pda)->{
				IPromise<List<PSDataJRso>>  fsPro = notifyResult(new RespJRso(code,msg),items,PSDataJRso.RESULT_FAIL_DISPATCH);
				fsPro
				.fail((code0,msg0,cxt)->{
					//回调成功并不表示此数据发送成功，原始数据转发失败即认为此消息发送失败。回调只是告诉原始发送者此消息发送失败
					logger.error("code: " + code0+",msg: " + msg0);
					notiryResultFail(code,msg,items,Arrays.asList(items),p);
					p.setFail(code, msg);
					p.done();
				})
				.success((rst,is)->{
					resultItem(items,PSDataJRso.RESULT_FAIL_DISPATCH);
					p.setFail(code,msg);
					p.done();
				});
			});
			return p;
		} catch (Throwable e) {
			String msg = "callAsArra topic:"+sm.getTopic()+",mkey:"+sm.getKey().methodID();
			logger.error(msg, e);
			LG.log(MC.LOG_ERROR, TAG,msg,e);
			resultItem(items,PSDataJRso.RESULT_FAIL_DISPATCH);
			p.setResult(items);
			p.setFail(1,msg);
			p.done();
		}
		return p;
	}

	private IPromise<List<PSDataJRso>> notifyResult(Object obj, PSDataJRso[] items,int resultCode) {
		Promise<List<PSDataJRso>> outp = new Promise<>();
		
		List<PSDataJRso> fails = new ArrayList<>();
		
		AtomicInteger cbcnt = new AtomicInteger(0);
		
		for (PSDataJRso pd : items) {
			//需要回调结果的消息才需要计数
			if(StringUtils.isNotEmpty(pd.getCallback())) {
				cbcnt.incrementAndGet();
			}
		}
		
		if(cbcnt.get() == 0) {
			outp.setResult(null);
			outp.done();
		}else {
			for (PSDataJRso pd : items) {
				try {
					if(StringUtils.isNotEmpty(pd.getCallback())) {
						IPromise<PSDataJRso> pro = callback(pd, obj, resultCode);
						pro.then((proData,fail,ctx)->{
							if(proData != null) {
								fails.add(proData);
								logger.error(fail.toString());
							}
							int cnt = cbcnt.decrementAndGet();
							if(cnt <= 0) {
								//全部通知返回结束
								outp.setResult(fails);
								outp.done();
							}
						});
					}
				} catch (Throwable e) {
					String msg = "callOneByOne pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().methodID();
					logger.error(msg, e);
					LG.log(MC.LOG_ERROR, TAG, msg,e);
					fails.add(pd);
					int cnt = cbcnt.decrementAndGet();
					if(cnt <= 0) {
						//全部通知返回结束
						outp.setResult(fails);
						outp.done();
					}
				}
			}
		}
		
		return outp;
	}

	private IPromise<PSDataJRso[]> callOneByOne(PSDataJRso[] items,int type) {
		
		Promise<PSDataJRso[]> p = new Promise<>();
		p.setResult(null);
		
		List<PSDataJRso> fails = new ArrayList<>();
		
		AtomicInteger ai = new AtomicInteger(items.length);
		
		for (PSDataJRso pd : items) {
			try {
				
				IPromise<?> rePromise = null;
				if(type == SINGLE) {
					rePromise = PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), pd,  new Object[] {pd});
				} else if(type == NONE){
					rePromise = PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), pd,  new Object[0]);
				} else if(type == DATA) {
					Object[] args = (Object[]) pd.getData();
					rePromise = PromiseUtils.callService(srvProxy, sm.getKey().getMethod(), pd,  args);
				}
				
				rePromise
				.success((result,pdItem)->{
					PSDataJRso pda = (PSDataJRso)pdItem;
					if(StringUtils.isNotEmpty(pda.getCallback())) {
						resultItem(pda,PSDataJRso.RESULT_SUCCCESS);
					} else {
						IPromise<PSDataJRso> inPro = callback((PSDataJRso)pdItem, result,PubSubManager.PUB_OK);
						inPro.success((iobj,actx)->{
							resultItem(pda,PSDataJRso.RESULT_SUCCCESS);
							int cnt = ai.decrementAndGet();
							if(cnt == 0) {
								if(!fails.isEmpty()) {
									PSDataJRso[] pds = new PSDataJRso[fails.size()];
									fails.toArray(pds);
									p.setResult(pds);
									p.setFail(1, "fail item in result");
								}
								p.done();
							}
						}).fail((code,msg,objItem)->{
							resultItem(pda,PSDataJRso.RESULT_FAIL_CALLBACK);
						});
						
					}
				}).fail((code,msg,pdItem)->{
					PSDataJRso pda = (PSDataJRso)pdItem;
					logger.error("code:" +code + ", msg: " + msg);
					fails.add(pd);
					int cnt = ai.decrementAndGet();
					if(cnt == 0) {
						if(!fails.isEmpty()) {
							PSDataJRso[] pds = new PSDataJRso[fails.size()];
							fails.toArray(pds);
							p.setResult(pds);
							p.setFail(1, "fail item in result");
						}
						p.done();
					}
					resultItem(pda,PSDataJRso.RESULT_FAIL_DISPATCH);
				});
			} catch (Throwable e) {
				String msg = "callOneByOne pd:"+pd.getId()+", topic:"+pd.getTopic()+",mkey:"+sm.getKey().methodID();
				logger.error(msg, e);
				LG.log(MC.LOG_ERROR, TAG,msg,e);
				fails.add(pd);
				
				resultItem(pd,PSDataJRso.RESULT_FAIL_DISPATCH);
				
				int cnt = ai.decrementAndGet();
				
				if(cnt == 0) {
					if(!fails.isEmpty()) {
						PSDataJRso[] pds = new PSDataJRso[fails.size()];
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

	private void resultItem(PSDataJRso pda, byte b) {
		try {
			if(pda.isPersist() || b != PSDataJRso.RESULT_SUCCCESS) {
				Document d = Document.parse(JsonUtils.getIns().toJson(pda));
				d.put("result", b);
				d.put("id", pda.getId());
				this.os.updateOrSaveById(PubSubManager.TABLE_PUBSUB_ITEMS,d, Document.class,
						IObjectStorage.ID,true);
			}
		} catch (Exception e) {
			logger.error("resultItem",e);
		}
	}
	
	private void resultItem(PSDataJRso[] pdas, byte b) {
		for(PSDataJRso pd : pdas) {
			resultItem(pd,b);
		}
	}
	
	public IPromise<PSDataJRso> callback(PSDataJRso item,Object result,int statuCode) {
		Promise<PSDataJRso> p = new Promise<>();
		p.setResult(null);
		
		if (StringUtils.isEmpty(item.getCallback())) {
			//p.setFail(-1, "callback is null");
			p.done();
			return p;
		}
		
		if(item.isCallbackMethod()) {
			return callbackServiceMethod(item,result,statuCode);
		} else {
			PSDataJRso d = new PSDataJRso();
			d.setTopic(item.getCallback());
			d.setData(new Object[] {result,item.getId(),statuCode});
			d.setPersist(true);
			d.setSrcClientId(item.getSrcClientId());
			d.put(PSDataJRso.SRC_PSDATA_ID, item.getId());
			d.setPersist(item.isPersist());
			pubsubServer.publishItem(d);
			p.done();
			return p;
		}
	}

	//异步回调用返回值，如异步RPC时，返回结果给调用者
	public IPromise<PSDataJRso> callbackServiceMethod(PSDataJRso item,Object result,int statuCode) {

		Promise<PSDataJRso> p = new Promise<>();
		p.setResult(null);
		
		if(StringUtils.isEmpty(item.getCallback())) {
			p.setFail(-1, "callback is null");
			p.done();
			return p;
		}
		
		Map<String,Object> cxt = item.getContext();
	
		Long linkId = (Long)cxt.get(JMicroContext.LINKER_ID);
		
		String key = item.getCallback();
		
		try {
			UniqueServiceMethodKeyJRso mkey = UniqueServiceMethodKeyJRso.fromKey(key); 
			Holder h = null;
			//String k = key.toKey(false, false, false);
			if(this.key2Holder.containsKey(key)) {
				h = this.key2Holder.get(key);
			} else {
				h = new Holder();
				h.srv  = of.getRemoteServie(mkey.getServiceName(),mkey.getNamespace(),mkey.getVersion(),null);
				if(h.srv == null) {
					String msg = "Fail to create async service proxy src:" + sm.getKey().toString()+",target:"+ key;
					LG.log(MC.LOG_ERROR,SubscriberCallbackImpl.class,msg);
					//即使返回false重发此条消息，也是同样的错误，没办法回调了，记录日志，只能通过人工处理
					p.setFail(1, msg);
					p.done();
					return p;
				}
				h.key = mkey;
				
				if(Message.is(item.getFlag(), PSDataJRso.FLAG_ASYNC_METHOD)) {
					//异步方法
					h.m = h.srv.getClass().getMethod(AsyncClientUtils.genAsyncMethodName(mkey.getMethod()),result.getClass());
				}else if(Message.is(item.getFlag(), PSDataJRso.FLAG_MESSAGE_CALLBACK)) {
					//消息通知
					h.m = h.srv.getClass().getMethod(AsyncClientUtils.genAsyncMethodName(mkey.getMethod()),Integer.TYPE,Long.TYPE,Map.class);
				}
				
				if(h.m == null) {
					String msg = "Async service method not found: src:" + sm.getKey().toString()+",target:"+ key;
					LG.log(MC.LOG_ERROR,SubscriberCallbackImpl.class, msg);
					//即使返回false重发此条消息，也是同样的错误，没办法回调了，记录日志，只能通过人工处理
					p.setFail(2, msg);
					return p;
				}
				
				key2Holder.put(key, h);
				
			}
			
			//JMicroContext.get().setParam(key, val);
			JMicroContext.get().setLong(JMicroContext.LINKER_ID, linkId);
			//JMicroContext.get().setLong(JMicroContext.REQ_ID, reqId);
			IPromise<?> cp = null;
			if(Message.is(item.getFlag(), PSDataJRso.FLAG_ASYNC_METHOD)) {
				//异步方法
				cp = PromiseUtils.callService(h.srv, h.key.getMethod(), null,  new Object[] {result});
				//cp = (IPromise<?>)h.m.invoke(h.srv, obj);
			}else if(Message.is(item.getFlag(), PSDataJRso.FLAG_MESSAGE_CALLBACK)) {
				//消息通知
				cp = PromiseUtils.callService(h.srv, h.key.getMethod(), null,  
						new Object[] {result,item.getId(),statuCode});
				//cp = (IPromise<?>)h.m.invoke(h.srv, statuCode,item.getId(),item.getContext());
			}
			
			if(cp == null) {
				p.setFail(3, "Invkke error: " + key);
				p.setResult(item);
				p.done();
			} else {
				cp.success((rst,actx)->{
					p.done();
				}).fail((code,msg,actx)->{
					p.setResult(item);
					p.setFail(code,msg);
					p.done();
				});
			}
			return p;
		} catch (Throwable e) {
			String msg = "Fail to callback src service:" + sm.getKey().toString()+ ",c allback: "+ key;
			LG.log(MC.LOG_ERROR,SubscriberCallbackImpl.class, msg,e);
			logger.error("",e);
			p.setResult(item);
			p.setFail(5, msg);
			return p;
		}
	
	}

	public void init() {
		try {
			Class<?>[] argsCls = UniqueServiceMethodKeyJRso.paramsClazzes(sm.getKey().getParamsStr());
			//String method = AsyncClientUtils.genAsyncMethodName(sm.getKey().getMethod());
			//this.m = AsyncClientUtils.getMethod(this.srvProxy.getClass(), method);
			if(argsCls == null || argsCls.length ==0) {
				//无参数
				this.type = NONE;
			}else if(argsCls.length ==1 && argsCls[0] == PSDataJRso.class ) {
				//PSData实例作为单一参数
				this.type = SINGLE;
			}else if(argsCls.length == 1 && argsCls[0] == new PSDataJRso[0].getClass() ) {
				//PSData实例数组作为参数
				this.type = ARR;
			} else {
				//PSDataJRso.data数组作为RPC参数
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
		public UniqueServiceMethodKeyJRso key;
		
	}

	@Override
	public String info() {
		return sm.getKey().methodID();
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

	public ServiceMethodJRso getSm() {
		return sm;
	}

	public void setSm(ServiceMethodJRso sm) {
		this.sm = sm;
	}
	
}
