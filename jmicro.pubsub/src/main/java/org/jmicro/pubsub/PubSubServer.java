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

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.IInternalSubRpc;
import org.jmicro.api.pubsub.ISubCallback;
import org.jmicro.api.pubsub.ISubsListener;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.pubsub.SubCallbackImpl;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:21
 */
//@Component(value=Constants.DEFAULT_PUBSUB,limit2Packages="org.jmicro.api.pubsub.PubSubManager")
@Service(limit2Packages="org.jmicro.api.pubsub.PubSubManager",
namespace="org.jmicro.pubsub.DefaultPubSubServer",version="0.0.1")
public class PubSubServer implements IInternalSubRpc{
	
	private final static Logger logger = LoggerFactory.getLogger(PubSubServer.class);
	
	@Cfg("/PubSubServer/enable")
	private boolean enable = false;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private PubSubManager pubsubManager;
	
	/**
	 *  订阅ID到回调之间映射关系
	 */
	private Map<String,ISubCallback> callbacks = new ConcurrentHashMap<>();	
	
	/**
	 * 主题与回调服务关联关系，每个主题可以有0到N个服务
	 */
	private Map<String,Queue<ISubCallback>> topic2Callbacks = new ConcurrentHashMap<>();
	
	private ExecutorService executor = null;
	
	private boolean running = false;
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=PubSubServer"});
		 Utils.getIns().waitForShutdown();
	}
	
	private ISubsListener subListener = new ISubsListener() {
		@Override
		public void on(byte type, String topic, UniqueServiceMethodKey smKey, 
				Map<String, String> context) {
			if(type == ISubsListener.SUB_ADD) {
				subcribe(topic,smKey,context);
			}else if(type == ISubsListener.SUB_REMOVE) {
				unsubcribe(topic,smKey,context);
			}
		}
	};
	
	public void init() {

		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(5);
		config.setTaskQueueSize(100);
		executor = ExecutorFactory.createExecutor(config);
		pubsubManager.addSubsListener(subListener);
		start();
	}
	
	
	public void start() {
		if(running) {
			return;
		}
		running = true;
		logger.debug("start pubsub server: {}", PubSubServer.class.getName());
	}
	
	public void stop() {
		
	}
	
	public boolean subcribe(String topic,UniqueServiceMethodKey key,Map<String, String> context) {
		
		String k = key.toKey(false, false, false);
		
		if(callbacks.containsKey(k)) {
			logger.warn("{} have been in the callback list",k);
		}
		
		Object srv = of.getServie(key.getUsk().getServiceName(), key.getUsk().getNamespace()
				, key.getUsk().getVersion());
		
		if(srv == null) {
			throw new CommonException("Servive [" + k + "] not found");
		}
		
		SubCallbackImpl cb = new SubCallbackImpl(key,srv);
		
		callbacks.put(k, cb);
		
		topic = topic.intern();
		
		synchronized(topic) {
			
			if(topic2Callbacks.containsKey(topic)) {
				topic2Callbacks.get(topic).offer(cb);
				return true;
			}
			
			topic2Callbacks.put(topic, new ConcurrentLinkedQueue<ISubCallback>());
			
			topic2Callbacks.get(topic).offer(cb);
		}
		
		return true;
	}
	
	public boolean unsubcribe(String topic,UniqueServiceMethodKey key,Map<String, String> context) {
		String k = key.toKey(false, false, false);
		if(!callbacks.containsKey(k)) {
			return true;
		}
		
		topic = topic.intern();
		
		synchronized(topic) {
			ISubCallback cb = callbacks.remove(k);
			Queue<ISubCallback> q = topic2Callbacks.get(k);
			if(q != null) {
				q.remove(cb);
			}
			return cb != null;
		}
	}
	
	public boolean publish(String topic,String content) {
		executor.submit(()->{
			PSData item = new PSData();
			item.setTopic(topic);
			try {
				item.setData(content.getBytes(Constants.CHARSET));
			} catch (UnsupportedEncodingException e) {
				logger.error("topic:"+topic,e);
			}
			doPublish(item);
		});
		return true;
	}
	
	public boolean publish(PSData item) {
		executor.submit(()->{
			doPublish(item);
		});
		return true;
	}
	
	private void doPublish(PSData item) {
		String topic = item.getTopic().intern();
		synchronized(topic) {
			Queue<ISubCallback> q = topic2Callbacks.get(topic);
			for(ISubCallback cb : q) {
				cb.onMessage(item);
			}
		}
	}

}
