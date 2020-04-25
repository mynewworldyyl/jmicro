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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.objectfactory.AbstractClientServiceProxy;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.service.ServiceLoader;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2020年4月4日
 */
@Component(level=2)
public class MonitorManager {
	
	private final static Logger logger = LoggerFactory.getLogger(MonitorManager.class);
	
	private static final String TYPES_PATH = Config.BASE_DIR+"/monitorTypes";
	
	private static final String TYPE_SPERATOR = ",";
	
	private final Short[] TYPES  = {
			MonitorConstant.Ms_Fail2BorrowBasket,MonitorConstant.Ms_SubmitCnt,MonitorConstant.Ms_FailReturnWriteBasket,
			MonitorConstant.Ms_CheckLoopCnt,MonitorConstant.Ms_CheckerSubmitItemCnt,
			MonitorConstant.Ms_TaskSuccessItemCnt,MonitorConstant.Ms_TaskFailItemCnt
	};
	
	private String[] typeLabels = null; 
	
	//private static final String TYPES_LOCKER = TYPES_PATH;
	
	/*@Cfg("/MonitorManager/isMonitorServer")
	private boolean isMonitorServer = false;*/
	
	@Reference(namespace="monitorServer",version="0.0.1",changeListener="enableWork")
	private IMonitorServer monitorServer;
	
	private boolean checkerWorking = false;
	
	@Inject(required=false)
	private IMonitorServer localMonitorServer;
	
	private AbstractClientServiceProxy msPo;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ILockerManager lockManager;
	
	private Map<String,Set<Short>> mkey2Types = new HashMap<>();
	
	private List<Short> types = new ArrayList<>();
	
	private BasketFactory<MRpcItem> basketFactory = null;
	
	private BasketFactory<MRpcItem> cacheBasket = null;
	
	private Object syncLocker = new Object();
	
	private ExecutorService executor = null;
	
	private MonitorManagerStatusAdapter statusMonitorAdapter;
	
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
		
		typeLabels = new String[TYPES.length];
		for(int i = 0; i < TYPES.length; i++) {
			typeLabels[i] = MonitorConstant.MONITOR_VAL_2_KEY.get(TYPES[i]);
		}
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		String group = "MonitorManager";
		statusMonitorAdapter = new MonitorManagerStatusAdapter(TYPES,typeLabels,
				Config.getInstanceName()+"_MonitorManagerStatuCheck",group);
		
		if(sl.hashServer()) {
			ServiceItem si = sl.createSrvItem(IMonitorAdapter.class, Config.getInstanceName()+"."+group, "0.0.1", IMonitorAdapter.class.getName());
			of.regist("PubsubServerStatuCheckAdapter", statusMonitorAdapter);
			sl.registService(si,statusMonitorAdapter);
		}
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(10);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("MonitorManager");
		executor = ExecutorFactory.createExecutor(config);
		
		enableWork(msPo,IServiceListener.ADD);
		
	}
	
	public boolean submit2Cache(MRpcItem item) {

		if(this.cacheBasket == null) {
			logger.error("cacheBasket is NULL");
			return false;
		}
		
		IBasket<MRpcItem> b = cacheBasket.borrowWriteBasket(true);
		if(b == null) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_Fail2BorrowBasket, 1);
			}
			logger.error("borrow write basket fail");
			return false;
		}
		b.write(item);
		
		//没有可写元素时，强制转为读状态
		if(!cacheBasket.returnWriteBasket(b, b.remainding() == 0)) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_FailReturnWriteBasket, 1);
			}
			logger.error("readySubmit fail to return this basket");
			return false;
		}
		
		if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
			this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_SubmitCnt, 1);
		}
		
		return true;
	
	}
	
	public boolean readySubmit(MRpcItem item) {
		if(!checkerWorking) {
			return false;
		}
		IBasket<MRpcItem> b = basketFactory.borrowWriteBasket(true);
		if(b == null) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_Fail2BorrowBasket, 1);
			}
			logger.error("readySubmit fail to borrow write basket");
			return false;
		}
		b.write(item);
		if(!basketFactory.returnWriteBasket(b, true)) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_FailReturnWriteBasket, 1);
			}
			logger.error("readySubmit fail to return this item");
			return false;
		}
		
		if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
			this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_SubmitCnt, 1);
		}
		
		/*synchronized(syncLocker) {
			syncLocker.notify();
		}*/
		
		return true;
	}
	
	
	public void enableWork(AbstractClientServiceProxy msPo, int opType) {
		if(!checkerWorking && IServiceListener.ADD == opType) {
			if(this.msPo != null && msPo.isUsable()) {
				checkerWorking = true;
				new Thread(this::doWork,Config.getInstanceName()+ "_MonitorManager_Worker").start();
			}
		} else if(checkerWorking && IServiceListener.REMOVE == opType) {
			checkerWorking = false;
		}
	}
	
	private void doWork() {
		
		logger.info("Minitor manage work start working!");
		
		Set<MRpcItem> items = new HashSet<MRpcItem>();
		int batchSize = 5;
		
		int maxSendInterval = 2000;
		int checkInterval = 5000;
		
		long lastSentTime = System.currentTimeMillis();
		//long lastLoopTime = System.currentTimeMillis();
		//long loopCnt = 0;
		
		while(checkerWorking) {
			try {
				
				if(this.statusMonitorAdapter.isMonitoralbe()) {
					this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_CheckLoopCnt, 1);
				}
				
				IBasket<MRpcItem> readBasket = this.basketFactory.borrowReadSlot();
				if(readBasket == null) {
					//超过5秒钟的缓存包，强制提交为读状态
					long beginTime = System.currentTimeMillis();
					IBasket<MRpcItem> wb = null;
					Iterator<IBasket<MRpcItem>> writeIte = this.cacheBasket.iterator(false);
					while((wb = writeIte.next()) != null) {
						if(!wb.isEmpty() && (beginTime - wb.firstWriteTime()) > 5000) {
							this.cacheBasket.returnWriteBasket(wb, true);
						} else {
							this.cacheBasket.returnWriteBasket(wb, false);
						}
					}
					
					//beginTime = System.currentTimeMillis();
					IBasket<MRpcItem> rb = null;
					Iterator<IBasket<MRpcItem>> readIte = this.cacheBasket.iterator(true);
					while((rb = readIte.next()) != null) {
						if(beginTime - rb.firstWriteTime() > 10000) { //超过10秒
							MRpcItem[] mrs = new MRpcItem[rb.remainding()];
							rb.readAll(mrs);
							items.addAll(Arrays.asList(mrs));
							cacheBasket.returnReadSlot(rb, true);
						} else {
							//没超过3分钟，不做单独发送，等等下次正常RPC做附带发送，或下次检测超时
							cacheBasket.returnReadSlot(rb, false);
						}
					}
					
					//检查监听器是否超时
					statusMonitorAdapter.checkTimeout();
					
					//中间耗费的时间要算在睡眠时间里面，如果耗费大于需要睡眠时间，则不需要睡眠了，直接进入下一次循环
					long costTime = System.currentTimeMillis() - beginTime;
					if((costTime = (checkInterval-costTime)) > 0) {
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
					readBasket.readAll(mrs);
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
						cb.readAll(mrs);
						items.addAll(Arrays.asList(mrs));
						cacheBasket.returnReadSlot(cb, true);
					}
					
					MRpcItem[] mrs = new MRpcItem[items.size()];
					items.toArray(mrs);
					//System.out.println("submit: " +mrs.length);
					
					if(this.statusMonitorAdapter.isMonitoralbe()) {
						this.statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_CheckerSubmitItemCnt, items.size());
					}
					
					this.executor.submit(new Worker(mrs));
					items.clear();
					lastSentTime = System.currentTimeMillis();
				}
				
			}catch(Throwable ex) {
				logger.error("MonitorManager doWork",ex);
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
				//JMicroContext.get().configMonitor(0, 0);
				//发送消息RPC
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR_MANAGER, true);
					
				if(localMonitorServer != null) {
					//本地包不需要RPC，直接本地调用
					localMonitorServer.submit(items);
					if(statusMonitorAdapter.isMonitoralbe()) {
						statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_TaskSuccessItemCnt, items.length);
					}
				} else if(monitorServer != null) {
					//不能保证百分百发送成功，会有数据丢失
					//为了提供更高的性能，丢失几个监控数据正常情况下可以接受
					//如果监控服务同时部署两个以上，而两个监控服务器同时不可用的机率很低，等同于不可能，从而丢数据的可能性也趋于不可能
					monitorServer.submit(items);
					if(statusMonitorAdapter.isMonitoralbe()) {
						statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_TaskSuccessItemCnt, items.length);
					}
				} else {
					if(statusMonitorAdapter.isMonitoralbe()) {
						statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_TaskFailItemCnt, items.length);
					}
					logger.error("Worker Monitor server is NULL");
				}
			} catch (Exception e) {
				if(statusMonitorAdapter.isMonitoralbe()) {
					statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_TaskFailItemCnt, items.length);
					//statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_TaskExceptionCount, items.length);
				}
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
		
		if(!this.checkerWorking || monitorServer == null) {
			return false;
		}
		
		if(!types.contains(t)) {
			return false;
		}
		
		return this.checkerWorking;
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
	
	public Short[] getTypes() {
		if(types.isEmpty()) {
			return null;
		}
		
		Short[] ts = new Short[types.size()];
		types.toArray(ts);
		
		return ts;
	}
	
}