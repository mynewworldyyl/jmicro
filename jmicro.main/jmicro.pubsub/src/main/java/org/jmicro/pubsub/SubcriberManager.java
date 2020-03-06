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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.classloader.RpcClassLoader;
import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
class SubcriberManager {

	private final static Logger logger = LoggerFactory.getLogger(SubcriberManager.class);

	private boolean openDebug = false;

	/**
	 * 订阅ID到回调之间映射关系
	 */
	private Map<String, ISubCallback> callbacks = new ConcurrentHashMap<>();

	/**
	 * 主题与回调服务关联关系，每个主题可以有0到N个服务
	 */
	private Map<String, Set<ISubCallback>> topic2Callbacks = new ConcurrentHashMap<>();

	private Queue<SubcribeItem> waitingLoadClazz = new ConcurrentLinkedQueue<>();
	private ClassLoadingWorker clWorker = null;
	private Object loadingLock = new Object();

	private RpcClassLoader cl;
	private IRegistry registry;
	private ServiceManager srvManager;

	private IObjectFactory of;

	SubcriberManager(IObjectFactory of, boolean openDebug) {
		this.openDebug = openDebug;
		this.of = of;
		this.cl = of.get(RpcClassLoader.class);
		this.registry = of.get(IRegistry.class);
		this.srvManager = of.get(ServiceManager.class);

		srvManager.addListener(serviceAddedRemoveListener);

		clWorker = new ClassLoadingWorker();
		clWorker.start();
	}

	Set<ISubCallback> getCallback(String topic) {
		return topic2Callbacks.get(topic);
	}

	ISubCallback getCallback(ServiceMethod sm) {
		Set<ISubCallback> calls = topic2Callbacks.get(sm.getTopic());
		if (calls != null && !calls.isEmpty()) {
			for (ISubCallback c : calls) {
				if (c.getSm().equals(sm)) {
					return c;
				}
			}
		}
		return null;

	}
	
	boolean isValidTopic(String topic) {
		if(topic2Callbacks.containsKey(topic)) {
			return !topic2Callbacks.get(topic).isEmpty();
		}
		return false;
	}

	Set<String> topics() {
		return topic2Callbacks.keySet();
	}

	private void serviceDataChange(ServiceItem item) {
		if (item == null || item.getMethods() == null) {
			return;
		}

		for (ServiceMethod sm : item.getMethods()) {
			// 接收异步消息的方法也要注册
			if (StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}

			String k = sm.getKey().toKey(false, false, false);

			if (callbacks.containsKey(k)) {
				// 服务名,版本,名称空 相同即为同一个服务,只需要注册一次即可
				logger.warn("{} have been in the callback list", k);
				continue;
			}

			this.waitingLoadClazz.offer(new SubcribeItem(SubcribeItem.TYPE_UPDATE, sm.getTopic(), sm, null));

			synchronized (loadingLock) {
				loadingLock.notify();
			}

			if (openDebug) {
				logger.debug("Got ont CB: {}", sm.getKey().toKey(true, true, true));
			}
		}
	}

	private void serviceRemoved(ServiceItem item) {

		for (ServiceMethod sm : item.getMethods()) {
			if (StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			unsubcribe(sm.getTopic(), sm, null);
		}
	}

	private void parseServiceAdded(ServiceItem item) {
		if (item == null || item.getMethods() == null) {
			return;
		}

		for (ServiceMethod sm : item.getMethods()) {
			// 接收异步消息的方法也要注册
			if (StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			subcribe(sm.getTopic(), sm, null);
			if (openDebug) {
				logger.debug("Got ont CB: {}", sm.getKey().toKey(true, true, true));
			}
		}

	}

	private IServiceListener serviceAddedRemoveListener = new IServiceListener() {
		@Override
		public void serviceChanged(int type, ServiceItem item) {
			if (type == IServiceListener.ADD) {
				parseServiceAdded(item);
			} else if (type == IServiceListener.REMOVE) {
				serviceRemoved(item);
			} else if (type == IServiceListener.DATA_CHANGE) {
				//serviceDataChange(item);
			} else {
				logger.error(
						"rev invalid Node event type : " + type + ",path: " + item.getKey().toKey(true, true, true));
			}
		}
	};

	boolean subcribe(String topic, ServiceMethod srvMethod, Map<String, String> context) {

		// doSaveSubscribe(null, srvMethod);

		String k = srvMethod.getKey().toKey(false, false, false);

		if (callbacks.containsKey(k)) {
			// 服务名,版本,名称空 相同即为同一个服务,只需要注册一次即可
			logger.warn("{} have been in the callback list", k);
			return true;
		}
		this.waitingLoadClazz.offer(new SubcribeItem(SubcribeItem.TYPE_SUB, topic, srvMethod, context));
		synchronized (loadingLock) {
			loadingLock.notify();
		}
		return true;
	}

	boolean unsubcribe(String topic, ServiceMethod srvMethod, Map<String, String> context) {
		String k = srvMethod.getKey().toKey(false, false, false);
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
		return doSubscribe(sui);
	}

	private boolean doSubscribe(SubcribeItem sui) {

		String k = sui.sm.getKey().toKey(false, false, false);

		if (callbacks.containsKey(k)) {
			logger.warn("{} have been in the callback list", k);
			return true;
		}

		Set<ServiceItem> sis = registry.getServices(sui.sm.getKey().getServiceName(), sui.sm.getKey().getNamespace(),
				sui.sm.getKey().getVersion());

		if (sis == null || sis.isEmpty()) {
			logger.warn("Service Item not found {}", k);
			return false;
		}

		ServiceItem sitem = null;
		for (ServiceItem si : sis) {
			if (si.getKey().getInstanceName().equals(sui.sm.getKey().getInstanceName())) {
				sitem = si;
				break;
			}
		}

		if (sitem == null) {
			logger.warn("Service Item for classloader server not found {}", sui.sm.getKey().toKey(true, true, true));
			return false;
		}

		Object srv = null;
		try {
			PubSubServer.class.getClassLoader().loadClass(sui.sm.getKey().getUsk().getServiceName());
			srv = of.getRemoteServie(sitem, null, null);
		} catch (ClassNotFoundException e) {
			try {
				// JMicroContext.get().setParam(Constants.SERVICE_SPECIFY_ITEM_KEY, sitem);
				JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, sitem);
				Class<?> cls = this.cl.loadClass(sui.sm.getKey().getUsk().getServiceName());
				if (cls != null) {
					srv = of.getRemoteServie(sitem, this.cl, null);
				}
			} catch (ClassNotFoundException e1) {
				logger.warn("Service {} not found.{}", k, e1);
				return false;
			}
		}

		if (srv == null) {
			logger.warn("Servive [" + k + "] not found");
			return false;
		}

		SubCallbackImpl cb = new SubCallbackImpl(sui.sm, srv, this.of);

		callbacks.put(k, cb);

		// sui.topic = sui.topic.intern();
		// synchronized(sui.topic) {
		if (!topic2Callbacks.containsKey(sui.topic)) {
			topic2Callbacks.put(sui.topic, new HashSet<ISubCallback>());
		}
		topic2Callbacks.get(sui.topic).add(cb);
		// }

		if (openDebug) {
			logger.debug("Subcribe:{},topic:{}", k, sui.topic);
		}

		return true;
	}

	private boolean doUnsubcribe(String topic, UniqueServiceMethodKey key, Map<String, String> context) {

		Set<ServiceItem> sis = registry.getServices(key.getServiceName(), key.getNamespace(), key.getVersion());

		if (sis == null || sis.isEmpty()) {

			String k = key.toKey(false, false, false);

			if (openDebug) {
				logger.debug("Unsubscribe CB:{} topic: {}", k, topic);
			}

			ISubCallback cb = callbacks.remove(k);
			Set<ISubCallback> q = topic2Callbacks.get(topic);
			if (q != null) {
				q.remove(cb);
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

		public void submit(Runnable r) {
			tasks.offer(r);
			synchronized (loadingLock) {
				loadingLock.notify();
				;
			}
		}

		public void run() {
			Set<SubcribeItem> failItems = new HashSet<>();
			// Set<SendItem> failPsDatas = new HashSet<>();
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
								/*if (!doUpdateSubscribe(si)) {
									failItems.add(si);
								}*/
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
