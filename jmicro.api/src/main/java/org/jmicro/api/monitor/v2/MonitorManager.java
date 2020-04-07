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
package org.jmicro.api.monitor.v2;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jmicro.api.IListener;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.basket.BasketFactory;
import org.jmicro.api.basket.IBasket;
import org.jmicro.api.cache.lock.ILocker;
import org.jmicro.api.cache.lock.ILockerManager;
import org.jmicro.api.config.Config;
import org.jmicro.api.executor.ExecutorConfig;
import org.jmicro.api.executor.ExecutorFactory;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2020年4月4日
 */
@Component(level=0)
public class MonitorManager {
	
	private final static Logger logger = LoggerFactory.getLogger(MonitorManager.class);
	
	private static final String TYPES_PATH = Config.BASE_DIR+"/monitorTypes";
	
	private static final String TYPE_SPERATOR = ",";
	
	//private static final String TYPES_LOCKER = TYPES_PATH;
	
	@Reference(namespace="monitorServer",version="0.0.1")
	private IMonitorServer monitorServer;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ILockerManager lockManager;
	
	private Map<String,Set<Short>> mkey2Types = new HashMap<>();
	
	private Set<Short> types = new HashSet<>();
	
	private BasketFactory<MRpcItem> basketFactory = null;
	
	private Object syncLocker = new Object();
	
	private ExecutorService executor = null;

	public void init() {
		this.basketFactory = new BasketFactory<MRpcItem>(100,1);
		op.addChildrenListener(TYPES_PATH, (type,parentDir,skey,data)->{
			if(type == IListener.ADD) {
				doAddType(skey,data);
			}else if(type == IListener.DATA_CHANGE) {
				doUpdateType(skey,data);
			}else if(type == IListener.REMOVE) {
				doDeleteType(skey,data);
			}
		}) ;
		
		Set<String> children = op.getChildren(TYPES_PATH, false);
		if(children != null && !children.isEmpty()) {
			for(String c : children) {
				String data = op.getData(TYPES_PATH+"/"+c);
				doAddType(c,data);
			}
		}
		
	}

	@JMethod("ready")
	public void ready() {
		logger.info("Init object :" +this.hashCode());
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(10);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("SubmitItemHolderManager");
		executor = ExecutorFactory.createExecutor(config);
		
		new Thread(this::doWork,Config.getInstanceName()+ "_MonitorManager_Worker").start();
	}
	
	public boolean readySubmit(MRpcItem item) {
		IBasket<MRpcItem> b = basketFactory.borrowWriteBasket();
		if(b == null) {
			logger.error("readySubmit fail to borrow write basket");
			return false;
		}
		b.add(item);
		if(!basketFactory.returnWriteBasket(b, true)) {
			logger.error("readySubmit fail to return this item");
			return false;
		}
		
		synchronized(syncLocker) {
			syncLocker.notify();
		}
		return true;
	}
	
	private void doWork() {
		
		Set<MRpcItem> items = new HashSet<MRpcItem>();
		int batchSize = 5;
		int maxSendInterval = 2000;
		int checkInterval = 5000;
		long lastSentTime = System.currentTimeMillis();
		
		while(true) {
			try {
				
				IBasket<MRpcItem> b = this.basketFactory.borrowReadSlot();
				if(b == null && items.size() == 0) {
					synchronized(syncLocker) {
						syncLocker.wait(checkInterval);
					}
					continue;
				}
				
				while(b != null) {
					MRpcItem[] mrs = new MRpcItem[b.remainding()];
					b.getAll(mrs);
					items.addAll(Arrays.asList(mrs));
					basketFactory.returnReadSlot(b, true);
					b = this.basketFactory.borrowReadSlot();
				}
				
				if(items.size() == 0) {
					continue;
				}
				
				if(items.size() >= batchSize || (System.currentTimeMillis() - lastSentTime) > maxSendInterval) {
					MRpcItem[] mrs = new MRpcItem[items.size()];
					items.toArray(mrs);
					this.executor.submit(new Worker(mrs));
					items.clear();
					lastSentTime = System.currentTimeMillis();
				}
				
			}catch(Throwable ex) {
				logger.error("MonitorManager doWork"+ex);
			}
		}
	}
	
	
	private class Worker implements Runnable {
		
		private MRpcItem[] items = null;
		
		public Worker( MRpcItem[] items) {
			this.items = items;
		}
		
		@Override
		public void run() {
			
			//不需要监控
			JMicroContext.get().configMonitor(0, 0);
			//发送消息RPC
			JMicroContext.get().setBoolean(Constants.FROM_MONITOR_MANAGER, true);
				
			if(monitorServer != null) {
				monitorServer.submit(items);
			}else {
				logger.error("Worker Monitor server is NULL");
			}
			
		}
	}
	
	public void registType(String srvKey,Short[] typess) {

		if(typess == null || typess.length == 0) {
			logger.error(srvKey+" types is NULL");
			return;
		}
		StringBuilder sb = new StringBuilder();
		for(Short s : typess) {
			sb.append(s).append(TYPE_SPERATOR);
		}
		sb.delete(sb.length()-1, sb.length());
		
		ILocker l = null;
		try {
			String path = TYPES_PATH+"/"+srvKey;
			l = lockManager.getLocker(path);
			if(l.tryLock(10000)) {
				if(op.exist(path)) {
					String ts = op.getData(path);
					if(StringUtils.isEmpty(ts)) {
						ts = sb.toString();
					} else {
						String[] tsArr = ts.split(TYPE_SPERATOR);
						for(short sv : typess) {
							boolean f = false;
							for(String v : tsArr) {
								short ssv = Short.parseShort(v);
								if(ssv == sv) {
									f = true;
									break;
								}
							}
							
							if(!f) {
								ts = ts + TYPE_SPERATOR + sv;
							}
						}
					}
					op.setData(path, ts);
				} else {
					op.createNode(path, sb.toString(), true);
				}
			} else {
				logger.warn("Fail to get locker to regist types for:" + srvKey+", types:" + sb.toString());
			}
		}finally {
			if(l != null) {
				l.unLock();
			}
		}
	}
	
	public Set<Short> intrest(String skey) {
		return this.mkey2Types.get(skey);
	}
	
	public boolean needType(Short t) {
		synchronized(types) {
			return types.contains(t);
		}
	}
	
	private void doDeleteType(String skey, String data) {
		Set<Short> ts = mkey2Types.get(skey);
		mkey2Types.remove(skey);
		
		for(Short t : ts) {
			boolean need = false;
			for(Set<Short> ss : mkey2Types.values()) {
				if(ss.contains(t)) {
					need = true;
					break;
				}
			}
			if(!need) {
				types.remove(t);
			}
		}
	}

	private void doUpdateType(String skey, String data) {

		if(StringUtils.isEmpty(data)) {
			doDeleteType(skey,data);
			return;
		}
		
		Set<Short> oldTs = mkey2Types.get(skey);
		if(oldTs == null || oldTs.isEmpty()) {
			doAddType(skey,data);
			return;
		}
		
		Set<Short> newTs = new HashSet<Short>();
		String[] tsArr = data.split(TYPE_SPERATOR);
		for(String t : tsArr) {
			Short v = Short.parseShort(t);
			newTs.add(v);
		}
		
		Set<Short> delTs0 = new HashSet<Short>();
		delTs0.addAll(oldTs);
		
		//计算被删除的类型，delTs0中剩下的就是被删除元素
		delTs0.removeAll(newTs);
		
		//计算新增的类型，newTs中剩下都是新增类型 
		newTs.removeAll(oldTs);
		
		//作为新增元素加到集合中
		oldTs.addAll(newTs);
		oldTs.removeAll(delTs0);
		
		//利用集合自动重
		types.addAll(newTs);
		
		for(Short t : delTs0) {
			boolean need = false;
			for(Set<Short> ss : mkey2Types.values()) {
				if(ss.contains(t)) {
					need = true;
					break;
				}
			}
			if(!need) {
				types.remove(t);
			}
		}
	}

	private void doAddType(String skey, String data) {
		if(StringUtils.isEmpty(data)) {
			mkey2Types.put(skey, new HashSet<Short>());
			return;
		}
		
		Set<Short> ts = new HashSet<Short>();
		String[] tsArr = data.split(TYPE_SPERATOR);
		for(String t : tsArr) {
			Short v = Short.parseShort(t);
			ts.add(v);
			types.add(v);
		}
		mkey2Types.put(skey, ts);
	}
	
}
