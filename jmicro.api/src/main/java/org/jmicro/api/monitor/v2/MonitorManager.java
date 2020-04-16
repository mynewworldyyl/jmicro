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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.jmicro.api.IListener;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
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
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
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
	
	@Cfg("/MonitorManager/isMonitorServer")
	private boolean isMonitorServer = false;
	
	@Reference(namespace="monitorServer",version="0.0.1")
	private IMonitorServer monitorServer;
	
	@Inject(required=false)
	private IMonitorServer localMonitorServer;
	
	private AbstractClientServiceProxy msPo;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ILockerManager lockManager;
	
	private Map<String,Set<Short>> mkey2Types = new HashMap<>();
	
	private Set<Short> types = new HashSet<>();
	
	private BasketFactory<MRpcItem> basketFactory = null;
	
	private BasketFactory<MRpcItem> cacheBasket = null;
	
	private Object syncLocker = new Object();
	
	private ExecutorService executor = null;

	public void init() {
		
		this.basketFactory = new BasketFactory<MRpcItem>(5000,1);
		this.cacheBasket = new BasketFactory<MRpcItem>(1000,5);
		
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
		
		msPo = (AbstractClientServiceProxy)((Object)this.monitorServer);
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(10);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("MonitorManager");
		executor = ExecutorFactory.createExecutor(config);
		
		new Thread(this::doWork,Config.getInstanceName()+ "_MonitorManager_Worker").start();
	}
	
	public boolean submit2Cache(MRpcItem item) {

		if(this.cacheBasket == null) {
			logger.error("cacheBasket is NULL");
			return false;
		}
		
		IBasket<MRpcItem> b = cacheBasket.borrowWriteBasket();
		if(b == null) {
			logger.error("borrow write basket fail");
			return false;
		}
		b.add(item);
		
		//没有可写元素时，强制转为读状态
		if(!cacheBasket.returnWriteBasket(b, b.remainding() == 0)) {
			logger.error("readySubmit fail to return this basket");
			return false;
		}
		
		return true;
	
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
		long lastLoopTime = System.currentTimeMillis();
		//long loopCnt = 0;
		
		while(true) {
			try {
				
				IBasket<MRpcItem> readBasket = this.basketFactory.borrowReadSlot();
				if(readBasket == null || msPo == null || !msPo.isUsable()) {
					//超过5秒钟的缓存包，强制提交为读状态
					long beginTime = System.currentTimeMillis();
					IBasket<MRpcItem> wb = this.cacheBasket.borrowWriteBasket();
					if(wb != null && !wb.isEmpty() && System.currentTimeMillis() - wb.firstWriteTime() > 5000) {
						this.cacheBasket.returnWriteBasket(wb, true);
					}
					
					IBasket<MRpcItem> cb = null;
					while((cb = this.cacheBasket.borrowReadSlot()) != null) {
						if(System.currentTimeMillis() - wb.firstWriteTime() > 180000) { //超过3分钟
							MRpcItem[] mrs = new MRpcItem[cb.remainding()];
							cb.getAll(mrs);
							items.addAll(Arrays.asList(mrs));
							cacheBasket.returnReadSlot(cb, true);
						} else {
							//没超过3分钟，不做单独发送，等等下次正常RPC做附带发送，或下次检测超时
							cacheBasket.returnReadSlot(cb, false);
						}
						
					}
					
					//中间耗费的时间要算在睡眠时间里面，如果耗费大于需要睡眠时间，则不需要睡眠了，直接进入下一次循环
					long costTime = System.currentTimeMillis() - beginTime;
					if((costTime = checkInterval-costTime) > 0) {
						synchronized(syncLocker) {
							syncLocker.wait(costTime);
						}
					}
					
					if(items.isEmpty()) {
						continue;
					}
				}
				
				while(readBasket != null) {
					MRpcItem[] mrs = new MRpcItem[readBasket.remainding()];
					readBasket.getAll(mrs);
					items.addAll(Arrays.asList(mrs));
					basketFactory.returnReadSlot(readBasket, true);
					readBasket = this.basketFactory.borrowReadSlot();
				}
				
				if(items.size() == 0) {
					continue;
				}
				
				merge(items);
				
				if(items.size() >= batchSize || (items.size() > 0 && ((System.currentTimeMillis() - lastSentTime) > maxSendInterval))) {
					
					IBasket<MRpcItem> cb = null;
					while((cb = this.cacheBasket.borrowReadSlot()) != null) {
						MRpcItem[] mrs = new MRpcItem[cb.remainding()];
						cb.getAll(mrs);
						items.addAll(Arrays.asList(mrs));
						cacheBasket.returnReadSlot(cb, true);
					}
					
					//loopCnt++;
					if(System.currentTimeMillis() - lastLoopTime < 100) {
						//double v = loopCnt/(System.currentTimeMillis() - lastLoopTime);
						System.out.println("MonitorManager do submit: " + items.size());
					}
					lastLoopTime = System.currentTimeMillis();
					
					MRpcItem[] mrs = new MRpcItem[items.size()];
					items.toArray(mrs);
					//System.out.println("submit: " +mrs.length);
					this.executor.submit(new Worker(mrs));
					items.clear();
					lastSentTime = System.currentTimeMillis();
				}
				
			}catch(Throwable ex) {
				logger.error("MonitorManager doWork"+ex);
			}
		}
	}
	
	
	private void merge(Set<MRpcItem> items) {
		
		Set<MRpcItem> result = new HashSet<>();
		
		//Map<String,OneItem> oneItems = new HashMap<>();
		
		Map<String,MRpcItem> mprcItems = new HashMap<>();
		
		MRpcItem nullSMMRpcItem = null;
		OneItem clientReadOi = null;
		OneItem serverReadOi = null;
		
		for(Iterator<MRpcItem> ite = items.iterator(); ite.hasNext();) {
			MRpcItem mi = ite.next();
			ite.remove();
			
			if(!canCompress(mi) ) {
				result.add(mi);
			}
			
			if(mi.getSm() == null) {
				//非RPC环境下的事件
				if(nullSMMRpcItem == null) {
					nullSMMRpcItem = mi;
					//第一个，不用处理，别的合并到这个选项下面
					continue;
				}
				Iterator<OneItem> oiIte = mi.getItems().iterator();
				for(; oiIte.hasNext(); ) {
					OneItem oi = oiIte.next();
					oiIte.remove();
					if(oi.getType() == MonitorConstant.CLIENT_IOSESSION_READ) {
						if(clientReadOi == null) {
							clientReadOi = oi;
						} else {
							clientReadOi.doAdd(1,oi.getVal());
						}
					} else if(oi.getType() == MonitorConstant.SERVER_IOSESSION_READ) {
						if(serverReadOi == null) {
							serverReadOi = oi;
						} else {
							serverReadOi.doAdd(1,oi.getVal());
						}
					} else {
						nullSMMRpcItem.addOneItem(oi);
					}
				}
			} else {
				String smKey = mi.getSm().getKey().toKey(true, true, true);
				MRpcItem oldMi = mprcItems.get(smKey);
				 if(canDoLog(mi)) {
					//日志记录不合并
					result.add(mi);
				} else if(oldMi == null) {
					oldMi = mi;
					//mi.setLinkId(0L);
					//消息合并后，以下字段都没意义
					mi.setMsg(null);
					mi.setReq(null);
					mi.setResp(null);
					mprcItems.put(smKey,oldMi);
					result.add(oldMi);
				}  else {
					//同一个方法的不同RPC请求数据进行合并
					Iterator<OneItem> oiIte = mi.getItems().iterator();
					for(; oiIte.hasNext(); ) {
						OneItem oi = oiIte.next();
						OneItem ooi = oldMi.getItem(oi.getType());
						if(ooi == null) {
							oldMi.addOneItem(oi);
						} else {
							ooi.doAdd(1,oi.getVal());
						}
					
						/*if(oi.getType() == MonitorConstant.CLIENT_IOSESSION_READ||
								oi.getType() == MonitorConstant.SERVER_IOSESSION_READ) {}else {
							oldMi.addOneItem(oi);
						}*/
					}
				}
			}
			
		}
		
		if(nullSMMRpcItem != null) {
			result.add(nullSMMRpcItem);
			if(clientReadOi != null) {
				nullSMMRpcItem.addOneItem(clientReadOi);
			}
			if(serverReadOi != null) {
				nullSMMRpcItem.addOneItem(serverReadOi);
			}
		}
		
		items.addAll(result);
		if(items.size() < 0) {
			logger.error("Items cannot be NULL after compress");
		}
		//是否有利于JVM做回收操作？
		result.clear();
		
	}


	private boolean canDoLog(MRpcItem mi) {
		Iterator<OneItem> oiIte = mi.getItems().iterator();
		for(; oiIte.hasNext(); ) {
			OneItem oi = oiIte.next();
			if(oi.getType() == MonitorConstant.LINKER_ROUTER_MONITOR) {
				if(mi.getSm().getLogLevel() >= oi.getLevel()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean canCompress(MRpcItem mi) {
		Iterator<OneItem> oiIte = mi.getItems().iterator();
		for(; oiIte.hasNext(); ) {
			if(MonitorConstant.KEY_TYPES.contains(oiIte.next().getType())) {
				return false;
			}
		}
		return true;
	}


	private class Worker implements Runnable {
		
		private MRpcItem[] items = null;
		
		public Worker( MRpcItem[] items) {
			this.items = items;
		}
		
		@Override
		public void run() {
			
			try {
				//不需要监控，也不应该监控，否则数据包将进入死循环永远停不下来
				JMicroContext.get().configMonitor(0, 0);
				//发送消息RPC
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR_MANAGER, true);
					
				if(localMonitorServer != null) {
					//本地包不需要RPC，直接本地调用
					localMonitorServer.submit(items);
				} else if(monitorServer != null) {
					//不能保证百分百发送成功，会有数据丢失
					//为了提供更高的性能，丢失几个监控数据正常情况下可以接受
					//如果监控服务同时部署两个以上，而两个监控服务器同时不可用的机率很低，等同于不可能，从而丢数据的可能性也趋于不可能
					monitorServer.submit(items);
				} else {
					logger.error("Worker Monitor server is NULL");
				}
			} catch (Exception e) {
				logger.error("MonitorManager.worker.run",e);
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
	
	public boolean isServerReady() {
		return monitorServer != null;
	}
	
	public boolean canSubmit(Short t) {
		
		if(/*isMonitorServer ||*/ monitorServer == null) {
			return false;
		}
		
		if(!types.contains(t)) {
			return false;
		}
		
		return msPo.isUsable();
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

	public Map<String, Set<Short>> getMkey2Types() {
		return Collections.unmodifiableMap(mkey2Types);
	}
	
	
	
}
