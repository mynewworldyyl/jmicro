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
package cn.jmicro.api.monitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.basket.BasketFactory;
import cn.jmicro.api.basket.IBasket;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.monitor.genclient.ILogMonitorServer$JMAsyncClient;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

/**
 * 
 * @author Yulei Ye
 * @date 2020年4月4日
 */
@Component(level=3)
public class LogMonitorClient {

	private final static Logger logger = LoggerFactory.getLogger(LogMonitorClient.class);
	
	private final Short[] TYPES  = {
			MC.Ms_Fail2BorrowBasket,MC.Ms_SubmitCnt,MC.Ms_FailReturnWriteBasket,
			MC.Ms_CheckLoopCnt,MC.Ms_CheckerSubmitItemCnt,
			MC.Ms_TaskSuccessItemCnt,MC.Ms_TaskFailItemCnt
	};
	
	private String[] typeLabels = null; 
	
	@Reference(namespace="*",version="0.0.1",changeListener="enableWork")
	private ILogMonitorServer$JMAsyncClient monitorServer;
	
    @Cfg(value="/LogMonitorClient/registMonitorThreadService", changeListener="registMonitorThreadStatusChange")
    private boolean registMonitorThreadService = false;
    
    @Cfg(value="/LogMonitorClient/singleItemMaxSize")
    private int singleItemMaxSize = 8192;
    
	private boolean checkerWorking = false;
	
	@Inject(required=false)
	private ILogMonitorServer localMonitorServer;
	
	//private AbstractClientServiceProxyHolder msPo;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private MonitorAndService2TypeRelationshipManager mtManager;
	
	@Inject
	private ServiceLoader sl;
	
	//private Map<String,Boolean> srvMethodMonitorEnable = new HashMap<>();
	
	private BasketFactory<JMLogItem> basketFactory = null;
	
	private BasketFactory<JMLogItem> cacheBasket = null;
	
	private Object syncLocker = new Object();
	
	private ExecutorService executor = null;
	
	private MonitorClientStatusAdapter statusMonitorAdapter;
	
	private ServiceItem monitorServiceItem;
	
	public void init() {
		
		this.basketFactory = new BasketFactory<JMLogItem>(5000,1);
		this.cacheBasket = new BasketFactory<JMLogItem>(1000,5);
		
		/*Set<String> children = op.getChildren(Config.MonitorTypesDir, false);
		if(children != null && !children.isEmpty()) {
			for(String c : children) {
				String data = op.getData(Config.MonitorTypesDir+"/"+c);
				doAddType(c,data);
			}
		}*/
		
	}

	@JMethod("ready")
	public void ready() {
		
		logger.info("Init object :" +this.hashCode());
		
		//msPo = (AbstractClientServiceProxyHolder)((Object)this.monitorServer);
		
		typeLabels = new String[TYPES.length];
		for(int i = 0; i < TYPES.length; i++) {
			typeLabels[i] = MC.MONITOR_VAL_2_KEY.get(TYPES[i]);
		}
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		String group = "LogMonitorClient";
		statusMonitorAdapter = new MonitorClientStatusAdapter(TYPES,typeLabels,
				Config.getInstanceName()+"_MonitorClientStatuCheck",group);
		
		if(sl.hasServer() && !Config.isClientOnly()) {
			monitorServiceItem = sl.createSrvItem(IMonitorAdapter.class, Config.getNamespace()+"."+group, "0.0.1",
					IMonitorAdapter.class.getName(),Config.getClientId());
			of.regist("LogMonitorClientStatuCheckAdapter", statusMonitorAdapter);
		}
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(10);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("LogMonitorClient");
		executor = of.get(ExecutorFactory.class).createExecutor(config);
		
		enableWork(null,IServiceListener.ADD);
		
	}
	
	public void registMonitorThreadStatusChange() {
		if(monitorServiceItem == null || !sl.hasServer() || Config.isClientOnly()) {
			logger.warn("Monitor service not valid: hashServer:" + sl.hasServer()+", isClientOnly: "+ Config.isClientOnly());
			return;
		}
		if(registMonitorThreadService) {
			sl.registService(monitorServiceItem,statusMonitorAdapter);
		} else {
			sl.registService(monitorServiceItem,statusMonitorAdapter);
		}
	}

	
	public boolean submit2Cache(JMLogItem item) {

		if(this.cacheBasket == null || !this.monitorServer.isReady()) {
			logger.error("cacheBasket is NULL");
			return false;
		}
		
		if(checkMaxSize(item)) {
			logger.warn("Too max message: " + item);
			return false;
		}
		
		IBasket<JMLogItem> b = cacheBasket.borrowWriteBasket(true);
		if(b == null) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_Fail2BorrowBasket, 1);
			}
			logger.error("borrow write basket fail");
			return false;
		}
		b.write(item);
		
		//没有可写元素时，强制转为读状态
		if(!cacheBasket.returnWriteBasket(b, b.remainding() == 0)) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_FailReturnWriteBasket, 1);
			}
			logger.error("readySubmit fail to return this basket");
			return false;
		}
		
		if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
			this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_SubmitCnt, 1);
		}
		
		return true;
	
	}
	
	public boolean readySubmit(JMLogItem item) {
		if(!checkerWorking || !this.monitorServer.isReady()) {
			return false;
		}
		
		if(checkMaxSize(item)) {
			logger.warn("Too max message: " + item);
			return false;
		}
		
		IBasket<JMLogItem> b = basketFactory.borrowWriteBasket(true);
		if(b == null) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_Fail2BorrowBasket, 1);
			}
			logger.error("readySubmit fail to borrow write basket");
			return false;
		}
		b.write(item);
		if(!basketFactory.returnWriteBasket(b, true)) {
			if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
				this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_FailReturnWriteBasket, 1);
			}
			logger.error("readySubmit fail to return this item");
			return false;
		}
		
		if(this.statusMonitorAdapter != null && this.statusMonitorAdapter.isMonitoralbe()) {
			this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_SubmitCnt, 1);
		}
		
		/*synchronized(syncLocker) {
			syncLocker.notify();
		}*/
		
		return true;
	}
	
	
	private boolean checkMaxSize(JMLogItem item) {
		return getItemSize(item) > this.singleItemMaxSize;
	}

	public void enableWork(AbstractClientServiceProxyHolder msPo,int opType) {
		if(!checkerWorking && IServiceListener.ADD == opType) {
			logger.warn("Monitor server online and restart submit thread!");
			checkerWorking = true;
			new Thread(this::doWork,Config.getInstanceName()+ "_MonitorClient_Worker").start();
		} else if(checkerWorking && IServiceListener.REMOVE == opType) {
			logger.warn("Monitor server offline and stop submit thread!");;
			checkerWorking = false;
		}
	}
	
	private void doWork() {
		
		logger.info("Minitor manage work start working!");
		
		Set<JMLogItem> items = new HashSet<JMLogItem>();
		int batchSize = 5;
		
		int maxSendInterval = 2000;
		int checkInterval = 5000;
		
		long lastSentTime = TimeUtils.getCurTime();
		//long lastLoopTime = System.currentTimeMillis();
		//long loopCnt = 0;
		
		int packageSize = 0;
		
		int maxPackageSize = 8192;
		
		boolean forceSubmit = false;
		
		while(checkerWorking) {
			try {
				
				if(!this.monitorServer.isReady()) {
					synchronized(syncLocker) {
						syncLocker.wait(checkInterval);
					}
					continue;
				}
				
				forceSubmit = false;
				long beginTime = TimeUtils.getCurTime();
				
				if(this.statusMonitorAdapter.isMonitoralbe()) {
					this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_CheckLoopCnt, 1);
				}
				
				IBasket<JMLogItem> readBasket = this.basketFactory.borrowReadSlot();
				if(readBasket == null) {
					//超过5秒钟的缓存包，强制提交为读状态
					IBasket<JMLogItem> wb = null;
					Iterator<IBasket<JMLogItem>> writeIte = this.cacheBasket.iterator(false);
					while((wb = writeIte.next()) != null) {
						if(!wb.isEmpty() && (beginTime - wb.firstWriteTime()) > 2000) {
							this.cacheBasket.returnWriteBasket(wb, true);//转为读状态
						} else {
							this.cacheBasket.returnWriteBasket(wb, false);
						}
					}
					
					//beginTime = System.currentTimeMillis();
					IBasket<JMLogItem> rb = null;
					Iterator<IBasket<JMLogItem>> readIte = this.cacheBasket.iterator(true);
					while((rb = readIte.next()) != null ) {
						if((beginTime - rb.firstWriteTime() > 10000) && packageSize < maxPackageSize) { //超过10秒
							JMLogItem[] mrs = new JMLogItem[rb.remainding()];
							rb.readAll(mrs);
							cacheBasket.returnReadSlot(rb, true);
							rb = null;
							
							for(JMLogItem mi : mrs) {
								int size = getItemSize(mi);
								if(size > maxPackageSize && mi.getItems().size() > 1) {
									splitMi(mi,maxPackageSize);
									continue;
								}
								
								if(packageSize + size < maxPackageSize) {
									packageSize += size;
									items.add(mi);	
								} else {
									forceSubmit = true;
									readySubmit(mi);//将剩余的放回缓存
								}
							}
							//items.addAll(Arrays.asList(mrs));
							/*if(packageSize >= maxPackageSize) {
								break;
							}*/
						} else {
							//没超过3分钟，不做单独发送，等等下次正常RPC做附带发送，或下次检测超时
							cacheBasket.returnReadSlot(rb, false);
							rb = null;
						}
					}
					
					//检查监听器是否超时
					statusMonitorAdapter.checkTimeout();
					
					//中间耗费的时间要算在睡眠时间里面，如果耗费大于需要睡眠时间，则不需要睡眠了，直接进入下一次循环
					if(items.isEmpty()) {
						synchronized(syncLocker) {
							syncLocker.wait(checkInterval);
						}
						continue;
					}
					
				}
				
				if(!forceSubmit) {
					
					if(readBasket == null) {
						readBasket = this.basketFactory.borrowReadSlot();
					}
					
					while(readBasket != null && packageSize < maxPackageSize) {
						JMLogItem[] mrs = new JMLogItem[readBasket.remainding()];
						readBasket.readAll(mrs);
						
						basketFactory.returnReadSlot(readBasket, true);
						
						for(JMLogItem mi : mrs) {
							
							int size = getItemSize(mi);
							if(size > maxPackageSize && mi.getItems().size() > 1) {
								splitMi(mi,maxPackageSize);
								continue;
							}
							
							if(packageSize + size < maxPackageSize) {
								packageSize += size;
								items.add(mi);	
							} else {
								forceSubmit = true;
								readySubmit(mi);
							}
						}
						
						if(forceSubmit || packageSize >= maxPackageSize) {
							break;
						}
						//items.addAll(Arrays.asList(mrs));
						
						readBasket = this.basketFactory.borrowReadSlot();
					}
				}
				
				
				if(readBasket != null) {
					basketFactory.returnReadSlot(readBasket, readBasket.remainding() <= 0);
				}
				
				if(items.size() == 0) {
					synchronized(syncLocker) {
						syncLocker.wait(checkInterval);
					}
					continue;
				}
				
				if(items.size() > 1) {
					merge(items);
				}
				
				if(forceSubmit || items.size() >= batchSize || (items.size() > 0 && ((beginTime - lastSentTime) > maxSendInterval))) {
					
/*					IBasket<MRpcLogItem> cb = null;
					while((cb = this.cacheBasket.borrowReadSlot()) != null) {
						MRpcLogItem[] mrs = new MRpcLogItem[cb.remainding()];
						cb.readAll(mrs);
						items.addAll(Arrays.asList(mrs));
						cacheBasket.returnReadSlot(cb, true);
					}
*/					
					JMLogItem[] mrs = new JMLogItem[items.size()];
					items.toArray(mrs);
					//System.out.println("submit: " +mrs.length);
					
					if(this.statusMonitorAdapter.isMonitoralbe()) {
						this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_CheckerSubmitItemCnt, items.size());
					}
					
					this.executor.submit(new Worker(mrs));
					items.clear();
					lastSentTime = beginTime;
					packageSize = 0;
				}
				
			}catch(Throwable ex) {
				logger.error("MonitorClient doWork",ex);
			}
		}
		logger.warn("Submit thread exit!");
		//LG.logWithNonRpcContext(MC.LOG_WARN, LogMonitorClient.class, "Submit thread exit!");
	}
	
	//日志过大，需要分包上传
	private void splitMi(JMLogItem mi,int packageSize) {
		
		JMLogItem copy = mi.copy();
		
		int size = 0;
		for(OneLog ol : mi.getItems()) {
			int len = 0;
			if(!Utils.isEmpty(ol.getDesc())) {
				len += ol.getDesc().length();
			}
			if(!Utils.isEmpty(ol.getEx())) {
				len += ol.getEx().length();
			}
			
			if(size +len >=  packageSize) {
				 readySubmit(copy);
				 copy = mi.copy();
				 copy.addOneItem(ol);
				 size = len;
			} else {
				copy.addOneItem(ol);
				size += len;
			}
		}
		
		if(copy.getItems().size() > 0) {
			readySubmit(copy);
		}
		
	}

	private int getItemSize(JMLogItem mi) {
		int size = 13;
		for(OneLog ol : mi.getItems()) {
			if(!Utils.isEmpty(ol.getDesc())) {
				size += ol.getDesc().length();
			}
			if(!Utils.isEmpty(ol.getEx())) {
				size += ol.getEx().length();
			}
			if(!Utils.isEmpty(ol.getTag())) {
				size += ol.getTag().length();
			}
			if(!Utils.isEmpty(ol.getFileName())) {
				size += ol.getFileName().length();
			}
		}
		
		/*Object[] args = null;
		if(mi.getReq() != null) {
			
		}*/
		
		return size;
	}

	private void merge(Set<JMLogItem> items) {
		
		Set<JMLogItem> result = new HashSet<>();
		JMLogItem nullSMMRpcItem = null;
		
		for(Iterator<JMLogItem> ite = items.iterator(); ite.hasNext();) {
			JMLogItem mi = ite.next();
			ite.remove();
			if(mi.getSmKey() == null || (mi.getSmKey() != null && mi.getReq() == null)) {
				//非RPC环境下的事件
				if(nullSMMRpcItem == null) {
					nullSMMRpcItem = mi;
					//第一个，不用处理，别的合并到这个选项下面
					continue;
				}
				Iterator<OneLog> oiIte = mi.getItems().iterator();
				for(; oiIte.hasNext(); ) {
					nullSMMRpcItem.addOneItem(oiIte.next());
				}
			} else/* if(canDoLog(mi)) */{
				//日志记录不合并
				result.add(mi);
			}
		}
		
		if(nullSMMRpcItem != null) {
			result.add(nullSMMRpcItem);
		}
		
		items.addAll(result);
		if(items.size() < 0) {
			logger.error("Items cannot be NULL after compress");
		}
		//是否有利于JVM做回收操作？
		result.clear();
		
	}


	private class Worker implements Runnable {
		
		private JMLogItem[] items = null;
		
		public Worker( JMLogItem[] items) {
			this.items = items;
		}
		
		public void onresult(Object rst, AsyncFailResult fail,Object cxt) {
			if(fail != null) {
				logger.warn(fail.toString());
			}
		}
		
		@Override
		public void run() {
			
			try {
				//不需要监控，也不应该监控，否则数据包将进入死循环永远停不下来
				//JMicroContext.get().configMonitor(0, 0);
				//发送消息RPC
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR_CLIENT, true);
					
				if(localMonitorServer != null) {
					//本地包不需要RPC，直接本地调用
					localMonitorServer.submit(items);
					if(statusMonitorAdapter.isMonitoralbe()) {
						statusMonitorAdapter.getServiceCounter().add(MC.Ms_TaskSuccessItemCnt, items.length);
					}
				} else if(monitorServer != null) {
					//不能保证百分百发送成功，会有数据丢失
					//为了提供更高的性能，丢失几个监控数据正常情况下可以接受
					//如果监控服务同时部署两个以上，而两个监控服务器同时不可用的机率很低，等同于不可能，从而丢数据的可能性也趋于不可能
					/*logger.info("==========================================================");
					for(MRpcLogItem mi: items) {
						logger.info("lid:" +mi.getLinkId() +", reqId: " + mi.getReqId()+", parentId: " + mi.getReqParentId());
					}*/
					
					monitorServer.submitJMAsync(items).then(this::onresult);
					if(statusMonitorAdapter.isMonitoralbe()) {
						statusMonitorAdapter.getServiceCounter().add(MC.Ms_TaskSuccessItemCnt, items.length);
					}
				} else {
					if(statusMonitorAdapter.isMonitoralbe()) {
						statusMonitorAdapter.getServiceCounter().add(MC.Ms_TaskFailItemCnt, items.length);
					}
					logger.error("Worker Monitor server is NULL");
				}
			} catch (Exception e) {
				if(statusMonitorAdapter.isMonitoralbe()) {
					statusMonitorAdapter.getServiceCounter().add(MC.Ms_TaskFailItemCnt, items.length);
					//statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_TaskExceptionCount, items.length);
				}
				logger.error("MonitorClient.worker.run",e);
			}
		}
	}
	
	public boolean canSubmit(ServiceMethod sm, Short t) {
		if(!this.checkerWorking || monitorServer == null || !monitorServer.isReady()) {
			return false;
		}
		return this.mtManager.canSubmit(sm,t);
	}

}
