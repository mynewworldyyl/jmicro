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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.classloader.RpcClassLoader;
import org.jmicro.api.config.Config;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.IInternalSubRpc;
import org.jmicro.api.pubsub.ISubCallback;
import org.jmicro.api.pubsub.ISubsListener;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.pubsub.SubCallbackImpl;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceManager;
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
namespace=Constants.DEFAULT_PUBSUB,version="0.0.1")
@Component(level=5)
public class PubSubServer implements IInternalSubRpc{
	
	private final static Logger logger = LoggerFactory.getLogger(PubSubServer.class);
	
	@Cfg(value="/PubSubServer/openDebug")
	private boolean openDebug = false;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private PubSubManager pubsubManager;
	
	@Inject
	private RpcClassLoader cl;
	
	@Inject
	private ServiceManager srvManager;
	
	/**
	 *  订阅ID到回调之间映射关系
	 */
	private Map<String,ISubCallback> callbacks = new ConcurrentHashMap<>();	
	
	/**
	 * 主题与回调服务关联关系，每个主题可以有0到N个服务
	 */
	private Map<String,Set<ISubCallback>> topic2Callbacks = new ConcurrentHashMap<>();
	
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

		if(!pubsubManager.isEnable()) {
			logger.warn("/PubSubManager/enable must be true for pubsub server");
			return;
		}
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsCoreSize(1);
		config.setMsMaxSize(30);
		config.setTaskQueueSize(1000);
		config.setThreadNamePrefix("PublishExecurot");
		executor = ExecutorFactory.createExecutor(config);
		pubsubManager.addSubsListener(subListener);
		
		
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
			return true;
		}
		
		ServiceItem si = srvManager.getItem(key.getUsk().path(Config.ServiceRegistDir, true, true, true));
		
		if(si == null) {
			throw new CommonException("Service Item not found {}",
					key.getUsk().path(Config.ServiceConfigDir, true, true, true));
		}
		
		Object srv = null;
		try {
			PubSubServer.class.getClassLoader().loadClass(key.getUsk().getServiceName());
			srv = of.getRemoteServie(si,null);
		} catch (ClassNotFoundException e) {
			try {
				Class<?> cls = this.cl.loadClass(key.getUsk().getServiceName());
				if(cls != null) {
					srv = of.getRemoteServie(si,this.cl);
				}
			} catch (ClassNotFoundException e1) {
				logger.warn("Service {} not found.",k);
				return false;
			}
		}
		
		if(srv == null) {
			logger.warn("Servive [" + k + "] not found");
			return false;
		}
		
		SubCallbackImpl cb = new SubCallbackImpl(key,srv);
		
		callbacks.put(k, cb);
		
		topic = topic.intern();
		
		synchronized(topic) {
			if(!topic2Callbacks.containsKey(topic)) {
				topic2Callbacks.put(topic, new HashSet<ISubCallback>());
			}
			topic2Callbacks.get(topic).add(cb);
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
			Set<ISubCallback> q = topic2Callbacks.get(k);
			if(q != null) {
				q.remove(cb);
			}
			return cb != null;
		}
	}
	
	public boolean publishString(String topic,String content) {
		executor.submit(()->{
			PSData item = new PSData();
			item.setTopic(topic);
			item.setData(content);
			doPublish(item);
		});
		return true;
	}
	
	public boolean publishData(PSData item) {
		executor.submit(()->{
			doPublish(item);
		});
		return true;
	}
	
	private void doPublish(PSData item) {
		String topic = item.getTopic().intern();
		if(openDebug) {
			logger.debug("Got topic: {}",item.getTopic());
		}
		synchronized(topic) {
			Set<ISubCallback> q = topic2Callbacks.get(topic);
			for(ISubCallback cb : q) {
				if(openDebug) {
					logger.debug("Publish topic: {}, cb {}",item.getTopic(),cb.info());
				}
				cb.onMessage(item);
			}
		}
	}

}
