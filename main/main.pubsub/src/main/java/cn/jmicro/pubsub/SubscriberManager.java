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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.classloader.IClassloaderRpcJMSrv;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/**
 * 负责消息订阅逻辑 The directory of the structure PubSubDir is the root directory Topic
 * is pubsub topic and sub node is the listener of service method
 * 
 * | |--L2 |----topic1--|--L1 | |--L3 | | |--L1 | |--L2 |----topic2--|--L3 |
 * |--L4 PubSubDir--| |--L5 | | |--L1 | |--L2 | |--L3 |----topic3--|--L4 | |--L5
 * |--L6
 * 
 * @author Yulei Ye
 * @date 2020年1月16日
 */
class SubscriberManager {

	private final static Logger logger = LoggerFactory.getLogger(SubscriberManager.class);

	private boolean openDebug = false;

	/**
	 * 订阅ID到回调之间映射关系
	 */
	private Map<String, ISubscriberCallback> callbacks = new ConcurrentHashMap<>();

	/**
	 * 主题与回调服务关联关系，每个主题可以有0到N个服务
	 */
	private Map<String, Set<ISubscriberCallback>> topic2Callbacks = new ConcurrentHashMap<>();

	private Queue<SubcribeItem> waitingLoadClazz = new ConcurrentLinkedQueue<>();
	private ClassLoadingWorker clWorker = null;
	private Object loadingLock = new Object();

	private RpcClassLoader cl;
	private IRegistry registry;
	private ServiceManager srvManager;

	private IObjectFactory of;

	SubscriberManager(IObjectFactory of, boolean openDebug) {
		this.openDebug = openDebug;
		this.of = of;
		this.cl = of.get(RpcClassLoader.class);
		this.registry = of.get(IRegistry.class);
		this.srvManager = of.get(ServiceManager.class);

		srvManager.addListener(serviceAddedRemoveListener);

		clWorker = new ClassLoadingWorker();
		clWorker.start();
	}

	Set<ISubscriberCallback> getCallback(String topic) {
		return topic2Callbacks.get(topic);
	}

	ISubscriberCallback getCallback(ServiceMethodJRso sm) {
		Set<ISubscriberCallback> calls = topic2Callbacks.get(sm.getTopic());
		if (calls != null && !calls.isEmpty()) {
			for (ISubscriberCallback c : calls) {
				if (c.getSm().equals(sm)) {
					return c;
				}
			}
		}
		return null;

	}
	
	boolean isValidTopic(String topic) {
		return topic2Callbacks.get(topic) != null && !topic2Callbacks.get(topic).isEmpty();
	}

	Set<String> topics() {
		return topic2Callbacks.keySet();
	}

	//有可能是增加了主题，也有可能是减少了主题
	private void serviceDataChange(UniqueServiceKeyJRso siKey ,ServiceItemJRso item) {
		if(item == null) {
			item = this.srvManager.getServiceByKey(siKey.fullStringKey());
		}
		if (item == null || item.getMethods() == null) {
			return;
		}

		boolean doUpdate = false;
		
		for (ServiceMethodJRso sm : item.getMethods()) {
			// 接收异步消息的方法也要注册
			/*if (StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}*/

			String k = sm.getKey().methodID();

			if (callbacks.containsKey(k) || (!callbacks.containsKey(k) 
					&& StringUtils.isNotEmpty(sm.getTopic()))) {
				this.waitingLoadClazz.offer(new SubcribeItem(SubcribeItem.TYPE_UPDATE, sm.getTopic(), sm, null));
				doUpdate = true;
				if (openDebug) {
					logger.debug("Got one CB: {}", sm.getKey().fullStringKey());
				}
			}
		}
		
		if(doUpdate) {
			synchronized (loadingLock) {
				loadingLock.notify();
			}
		}
	}

	private void serviceRemoved(UniqueServiceKeyJRso siKey ,ServiceItemJRso item) {
		for (ServiceMethodJRso sm : item.getMethods()) {
			if (StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			unsubcribe(sm.getTopic(), sm, null);
		}
	}

	private void parseServiceAdded(UniqueServiceKeyJRso siKey ,ServiceItemJRso item) {
		if(item == null) {
			item = this.srvManager.getServiceByKey(siKey.fullStringKey());
		}
		if (item == null || item.getMethods() == null) {
			return;
		}

		for(ServiceMethodJRso sm : item.getMethods()) {
			//接收异步消息的方法也要注册
			if (StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			subcribe(sm.getTopic(), sm, null);
			if (openDebug) {
				logger.debug("Got ont CB: {}", sm.getKey().fullStringKey());
			}
		}

	}

	private IServiceListener serviceAddedRemoveListener = new IServiceListener() {
		@Override
		public void serviceChanged(int type, UniqueServiceKeyJRso siKey ,ServiceItemJRso item) {
			if (type == IServiceListener.ADD) {
				parseServiceAdded(siKey,item);
			} else if (type == IServiceListener.REMOVE) {
				serviceRemoved(siKey,item);
			} else if (type == IServiceListener.DATA_CHANGE) {
				//普通RPC方法可以动态更新为异步方法
				serviceDataChange(siKey,item);
			} else {
				logger.error(
						"rev invalid Node event type : " + type + ",path: " + item.fullStringKey());
			}
		}
	};

	boolean subcribe(String topic, ServiceMethodJRso srvMethod, Map<String, String> context) {

		if(StringUtils.isEmpty(topic)) {
			return false;
		}

		String k = srvMethod.getKey().methodID();
		
		ISubscriberCallback cb = callbacks.get(k);
		String[] ts = topic.split(Constants.TOPIC_SEPERATOR);
		
		boolean flag = false;
		for(String t : ts) {
			if(this.topic2Callbacks.get(t) == null || !this.topic2Callbacks.get(t).contains(cb)) {
				//此主题没有注册，进入注册流程
				flag = true;
				break;
			}
		}

		if (flag) {
			this.waitingLoadClazz.offer(new SubcribeItem(SubcribeItem.TYPE_SUB, topic, srvMethod, context));
			synchronized (loadingLock) {
				loadingLock.notify();
			}
			return true;
		}
		
		return false;
		
	}

	boolean unsubcribe(String topic, ServiceMethodJRso srvMethod, Map<String, String> context) {
		String k = srvMethod.getKey().methodID();
		if (!callbacks.containsKey(k)) {
			return true;
		}

		this.waitingLoadClazz.offer(new SubcribeItem(SubcribeItem.TYPE_REMOVE, topic, srvMethod, context));
		synchronized (loadingLock) {
			loadingLock.notify();
		}
		return true;
	}

	private boolean doUpdateSubscribe(SubcribeItem sui) {

		String k = sui.sm.getKey().methodID();
		
		SubscriberCallbackImpl cb = (SubscriberCallbackImpl)callbacks.get(k);
		
		if(cb == null) {

			Set<UniqueServiceKeyJRso> sis = registry.getServices(sui.sm.getKey().getServiceName(), sui.sm.getKey().getNamespace(),
					sui.sm.getKey().getVersion());

			if (sis == null || sis.isEmpty()) {
				logger.warn("Service Item not found {}", k);
				return false;
			}

			UniqueServiceKeyJRso sitem = null;
			for (UniqueServiceKeyJRso si : sis) {
				if (si.getInstanceName().equals(sui.sm.getKey().getInstanceName())) {
					sitem = si;
					break;
				}
			}

			if (sitem == null) {
				logger.warn("Service Item for classloader server not found {}", sui.sm.getKey().fullStringKey());
				//服务已经下线，直接剔除
				return true;
			}

			ServiceItemJRso item = this.srvManager.getServiceByKey(sitem.fullStringKey());
			
			Object srv = null;
			try {
				PubSubServer.class.getClassLoader().loadClass(sui.sm.getKey().getUsk().getServiceName());
				srv = of.getRemoteServie(item, null);
			} catch (ClassNotFoundException e) {
				srv = this.getRemoteService(sui,item);
			}

			if (srv == null) {
				logger.warn("Servive [" + k + "] not found");
				return false;
			}
			
			cb = new SubscriberCallbackImpl(sui.sm, srv, this.of);
			cb.init();

			callbacks.put(k, cb);
		
		}
		
		String[] curTs = null;
		if(StringUtils.isNotEmpty(sui.topic)) {
			curTs = sui.topic.split(Constants.TOPIC_SEPERATOR);
		}
		
		Set<String> oldTs = new HashSet<>();
		for(Map.Entry<String, Set<ISubscriberCallback>> e: this.topic2Callbacks.entrySet()) {
			//查找出当前服务方法所订阅的全部主题
			if(e.getValue().contains(cb)) {
				oldTs.add(e.getKey());
			}
		}
		
		Set<String> newTs = new HashSet<>();
		if(curTs != null) {
			newTs.addAll(Arrays.asList(curTs));
		}
		
		//newTs剩下的主题就是新增的
		newTs.removeAll(oldTs);
	    if(!newTs.isEmpty()) {
	    	for(String t : newTs) {
				if (!topic2Callbacks.containsKey(t)) {
					topic2Callbacks.put(t, new HashSet<ISubscriberCallback>());
				}
				if(!topic2Callbacks.get(t).contains(cb)) {
					topic2Callbacks.get(t).add(cb);
					if (openDebug) {
						logger.debug("subcribe:{},topic:{}", k, t);
					}
				}
			}
	    }
	    
	    newTs.clear();
	    if(curTs != null) {
	    	newTs.addAll(Arrays.asList(curTs));
	    }
	    
	    oldTs.removeAll(newTs);

	    for(String t : oldTs) {
			if (!topic2Callbacks.containsKey(t)) {
				continue;
			}
			if(topic2Callbacks.get(t).contains(cb)) {
				topic2Callbacks.get(t).remove(cb);
				if (openDebug) {
					logger.debug("unsubcribe:{},topic:{}", k, t);
				}
			}
		}

		return true;
	
	}

	private boolean doSubscribe(SubcribeItem sui) {

		String k = sui.sm.getKey().methodID();
		
		SubscriberCallbackImpl cb = (SubscriberCallbackImpl)callbacks.get(k);
		if (cb == null) {
			Set<UniqueServiceKeyJRso> sis = registry.getServices(sui.sm.getKey().getServiceName(), sui.sm.getKey().getNamespace(),
					sui.sm.getKey().getVersion());

			if (sis == null || sis.isEmpty()) {
				logger.warn("Service Item not found {}", k);
				return false;
			}

			UniqueServiceKeyJRso sitem = null;
			for (UniqueServiceKeyJRso si : sis) {
				if (si.getInstanceName().equals(sui.sm.getKey().getInstanceName())) {
					sitem = si;
					break;
				}
			}

			if (sitem == null) {
				logger.warn("Service Item for classloader server not found {}", sui.sm.getKey().fullStringKey());
				return false;
			}

			ServiceItemJRso item = this.srvManager.getServiceByKey(sitem.fullStringKey());
			
			Object srv = null;
			try {
				//优先本地加载
				PubSubServer.class.getClassLoader().loadClass(sui.sm.getKey().getUsk().getServiceName());
				srv = of.getRemoteServie(item, null);
			} catch (ClassNotFoundException e) {
				//本地没找到，远程加载
				srv = this.getRemoteService(sui,item);
			}

			if (srv == null) {
				logger.warn("Servive [" + k + "] not found");
				return false;
			}
			
			cb = new SubscriberCallbackImpl(sui.sm, srv, this.of);
			cb.init();

			callbacks.put(k, cb);
		}
		
		String[] ts = sui.topic.split(Constants.TOPIC_SEPERATOR);
		for(String t : ts) {
			if (!topic2Callbacks.containsKey(t)) {
				topic2Callbacks.put(t, new HashSet<ISubscriberCallback>());
			}
			if(!topic2Callbacks.get(t).contains(cb)) {
				topic2Callbacks.get(t).add(cb);
			}
			if (openDebug) {
				logger.debug("Subcribe:{},topic:{}", k, t);
			}
		}
		return true;
	}
	
	private UniqueServiceKeyJRso getClassLoaderItemByInstanceName(String instanceName) {
    	Set<UniqueServiceKeyJRso> items = this.registry.getServices(IClassloaderRpcJMSrv.class.getName());
		for (UniqueServiceKeyJRso si : items) {
			if (si.getInstanceName().equals(instanceName)) {
				return si;
			}
		}
		return null;
    }
	
	
	private Object getRemoteService(SubcribeItem sui,ServiceItemJRso sitem) {

		Object srv = null;
		boolean setDirectServiceItem = false;
		ServiceItemJRso oldItem = null;
		try {
			//Set<ServiceItemJRso> items = this.registry.getServices(IClassloaderRpc.class.getName());
			UniqueServiceKeyJRso clsLoadItem = getClassLoaderItemByInstanceName(sitem.getKey().getInstanceName());
			if(clsLoadItem != null) {
				ServiceItemJRso item = this.srvManager.getServiceByKey(clsLoadItem.fullStringKey());
				oldItem = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null);
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, item);
				setDirectServiceItem = true;
				Class<?> cls = this.cl.loadClass(sui.sm.getKey().getUsk().getServiceName());
				if (cls != null) {
					srv = of.getRemoteServie(sitem,null);
				}
			}
		} catch (ClassNotFoundException e1) {
			String k = sui.sm.getKey().methodID();
			logger.warn("Service {} not found.{}", k, e1);
			LG.log(MC.LOG_ERROR, this.getClass(), k,e1);
		}finally {
			if(setDirectServiceItem) {
				if(oldItem == null) {
					JMicroContext.get().removeParam(Constants.DIRECT_SERVICE_ITEM);
				} else {
					JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, oldItem);
				}
				
			}
		}
	return srv;
	}

	private boolean doUnsubcribe(String topic, UniqueServiceMethodKeyJRso key, Map<String, String> context) {

		Set<UniqueServiceKeyJRso> sis = registry.getServices(key.getServiceName(), key.getNamespace(), key.getVersion());

		if (sis == null || sis.isEmpty()) {

			//已经没有服务在线，将注册的回调删除
			String k = key.methodID();

			if (openDebug) {
				logger.debug("Unsubscribe CB:{} topic: {}", k, topic);
			}

			ISubscriberCallback cb = callbacks.remove(k);
			
			String[] ts = topic.split(Constants.TOPIC_SEPERATOR);
			for(String t : ts) {
				Set<ISubscriberCallback> q = topic2Callbacks.get(t);
				if (q != null) {
					q.remove(cb);
				}
			}
			
			return cb != null;
		}

		return true;
	}

	private final class ClassLoadingWorker extends Thread {

		private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

		// 存在发送失败需要重新发的项目
		// 只对doPublish中失败的消息负责
		// publishString及publishData返回false的消息，由客户端处理
		public ClassLoadingWorker() {
			super("JMicro-" + Config.getInstanceName() + "-ClassLoadingWorker");
		}

		public void run() {
			Set<SubcribeItem> failItems = new HashSet<>();
			this.setContextClassLoader(cl);
			while (true) {
				try {
					if (tasks.isEmpty() && waitingLoadClazz.isEmpty()) {
						synchronized (loadingLock) {
							loadingLock.wait();
						}
					}

					for (SubcribeItem si = waitingLoadClazz.poll(); si != null; si = waitingLoadClazz.poll()) {
						try {
							switch (si.type) {
							case SubcribeItem.TYPE_SUB:
								if (!doSubscribe(si)) {
									failItems.add(si);
								}
								break;
							case SubcribeItem.TYPE_REMOVE:
								if (!doUnsubcribe(si.topic, si.sm.getKey(), si.context)) {
									failItems.add(si);
								}
								break;
							case SubcribeItem.TYPE_UPDATE:
								if (!doUpdateSubscribe(si)) {
									failItems.add(si);
								}
								break;
							}
						} catch (Throwable e) {
							failItems.add(si);
							throw e;
						}
					}

					/*
					 * for(SendItem psd = psitems.poll(); psd != null; psd = psitems.poll() ) { try
					 * { psd.cb.onMessage(psd.item); } catch (Throwable e1) { psd.retryCnt++;
					 * if(psd.retryCnt > 3) {
					 * logger.error("Fail Item:{},retryCnt:{}",psd.item,psd.retryCnt); } else {
					 * failPsDatas.add(psd); } } }
					 */

					for (Runnable psd = tasks.poll(); psd != null; psd = tasks.poll()) {
						psd.run();
					}

				} catch (Throwable e) {
					logger.error("", e);
				} finally {
					boolean needSleep = false;
					if (!failItems.isEmpty()) {
						waitingLoadClazz.addAll(failItems);
						needSleep = true;
						failItems.clear();
					}

					/*
					 * if(!failPsDatas.isEmpty()) { psitems.addAll(failPsDatas); needSleep = true;
					 * failPsDatas.clear(); }
					 */

					if (needSleep) {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							logger.error("", e);
						}
					}

				}
			}
		}
	}

}
