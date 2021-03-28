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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.basket.BasketFactory;
import cn.jmicro.api.basket.IBasket;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.monitor.genclient.IStatisMonitorServer$JMAsyncClient;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2020年4月4日
 */
@Component(level=3)
public class StatisMonitorClient {
	
	private final static Logger logger = LoggerFactory.getLogger(StatisMonitorClient.class);
	
	private final Short[] TYPES  = {
			MC.Ms_Fail2BorrowBasket,MC.Ms_SubmitCnt,MC.Ms_FailReturnWriteBasket,
			MC.Ms_CheckLoopCnt,MC.Ms_CheckerSubmitItemCnt,
			MC.Ms_TaskSuccessItemCnt,MC.Ms_TaskFailItemCnt
	};
	
	private String[] typeLabels = null; 
	
	@Reference(namespace="*",version="0.0.1",changeListener="enableWork")
	private IStatisMonitorServer$JMAsyncClient monitorServer;
	
	private boolean checkerWorking = false;
	
	@Inject(required=false)
	private IStatisMonitorServer localMonitorServer;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private MonitorStatisConfigManager mscm;
	
	//private MonitorAndService2TypeRelationshipManager mtManager;
	//private MonitorStatisConfigManager mtManager;
	
	//private Map<String,Boolean> srvMethodMonitorEnable = new HashMap<>();
	
	private BasketFactory<JMStatisItem> basketFactory = null;
	
	private BasketFactory<JMStatisItem> cacheBasket = null;
	
	private Object syncLocker = new Object();
	
	private ExecutorService executor = null;
	
	private MonitorClientStatusAdapter statusMonitorAdapter;
	
	public void init() {
		
		this.basketFactory = new BasketFactory<JMStatisItem>(5000,1);
		this.cacheBasket = new BasketFactory<JMStatisItem>(1000,5);
		
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
		
		typeLabels = new String[TYPES.length];
		for(int i = 0; i < TYPES.length; i++) {
			typeLabels[i] = MC.MONITOR_VAL_2_KEY.get(TYPES[i]);
		}
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		String group = "StatisMonitorClient";
		statusMonitorAdapter = new MonitorClientStatusAdapter(TYPES,typeLabels,
				Config.getInstanceName()+"_MonitorClientStatuCheck",group);
		
		if(sl.hasServer() && !Config.isClientOnly()) {
			ServiceItem si = sl.createSrvItem(IMonitorAdapter.class, Config.getNamespace()+"."+group,
					"0.0.1", IMonitorAdapter.class.getName(),Config.getClientId());
			of.regist("StatisMonitorClientStatuCheckAdapter", statusMonitorAdapter);
			sl.registService(si,statusMonitorAdapter);
		}
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(10);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("StatisMonitorClient");
		executor = of.get(ExecutorFactory.class).createExecutor(config);
		
		enableWork(null,IServiceListener.ADD);
		
	}
	
	public boolean submit2Cache(JMStatisItem item) {

		if(this.cacheBasket == null || !this.monitorServer.isReady()) {
			logger.error("cacheBasket is NULL");
			return false;
		}
		
		IBasket<JMStatisItem> b = cacheBasket.borrowWriteBasket(true);
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
	
	public boolean readySubmit(JMStatisItem item) {
		if(!checkerWorking || !this.monitorServer.isReady()) {
			return false;
		}
		IBasket<JMStatisItem> b = basketFactory.borrowWriteBasket(true);
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
	
	
	public void enableWork(AbstractClientServiceProxyHolder msPo, int opType) {
		if(!checkerWorking && IServiceListener.ADD == opType) {
			logger.warn("Monitor thread started");
			checkerWorking = true;
			new Thread(this::doWork,Config.getInstanceName()+ "_MonitorClient_Worker").start();
		} else if(checkerWorking && IServiceListener.REMOVE == opType) {
			logger.warn("Monitor thread stop by monitor server offline!");
			checkerWorking = false;
		}
	}
	
	private void doWork() {
		
		logger.info("Minitor manage work start working!");
		
		Set<JMStatisItem> items = new HashSet<JMStatisItem>();
		int batchSize = 5;
		
		int maxSendInterval = 2000;
		int checkInterval = 5000;
		
		long lastSentTime = TimeUtils.getCurTime();
		//long lastLoopTime = System.currentTimeMillis();
		//long loopCnt = 0;
		
		while(checkerWorking) {
			try {
				
				long beginTime = TimeUtils.getCurTime();
				
				if(this.statusMonitorAdapter.isMonitoralbe()) {
					this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_CheckLoopCnt, 1);
				}
				
				IBasket<JMStatisItem> readBasket = this.basketFactory.borrowReadSlot();
				if(readBasket == null) {
					//超过5秒钟的缓存包，强制提交为读状态
					IBasket<JMStatisItem> wb = null;
					Iterator<IBasket<JMStatisItem>> writeIte = this.cacheBasket.iterator(false);
					while((wb = writeIte.next()) != null) {
						if(!wb.isEmpty() && (beginTime - wb.firstWriteTime()) > 5000) {
							this.cacheBasket.returnWriteBasket(wb, true);
						} else {
							this.cacheBasket.returnWriteBasket(wb, false);
						}
					}
					
					//beginTime = System.currentTimeMillis();
					IBasket<JMStatisItem> rb = null;
					Iterator<IBasket<JMStatisItem>> readIte = this.cacheBasket.iterator(true);
					while((rb = readIte.next()) != null) {
						if(beginTime - rb.firstWriteTime() > 10000) { 
							//超过10秒
							JMStatisItem[] mrs = new JMStatisItem[rb.remainding()];
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
					long costTime = TimeUtils.getCurTime() - beginTime;
					if(items.isEmpty() && (costTime = (checkInterval-costTime)) > 0) {
						synchronized(syncLocker) {
							syncLocker.wait(costTime);
						}
						continue;
					}
					
				}
				
				while(readBasket != null) {
					JMStatisItem[] mrs = new JMStatisItem[readBasket.remainding()];
					readBasket.readAll(mrs);
					items.addAll(Arrays.asList(mrs));
					basketFactory.returnReadSlot(readBasket, true);
					readBasket = basketFactory.borrowReadSlot();
				}
				
				if(items.isEmpty()) {
					synchronized(syncLocker) {
						syncLocker.wait(checkInterval);
					}
					continue;
				}
				
				merge(items);
				
				if(items.size() >= batchSize || (items.size() > 0 && ((TimeUtils.getCurTime() - lastSentTime) > maxSendInterval))) {
					
					IBasket<JMStatisItem> cb = null;
					while((cb = this.cacheBasket.borrowReadSlot()) != null) {
						JMStatisItem[] mrs = new JMStatisItem[cb.remainding()];
						cb.readAll(mrs);
						items.addAll(Arrays.asList(mrs));
						cacheBasket.returnReadSlot(cb, true);
					}
					
					JMStatisItem[] mrs = new JMStatisItem[items.size()];
					items.toArray(mrs);
					//System.out.println("submit: " +mrs.length);
					
					if(this.statusMonitorAdapter.isMonitoralbe()) {
						this.statusMonitorAdapter.getServiceCounter().add(MC.Ms_CheckerSubmitItemCnt, items.size());
					}
					
					this.executor.submit(new Worker(mrs));
					items.clear();
					lastSentTime = TimeUtils.getCurTime();
				} else {
					synchronized(syncLocker) {
						syncLocker.wait(checkInterval);
					}
					continue;
				}
				
			}catch(Throwable ex) {
				logger.error("MonitorClient doWork",ex);
			}
		}
	}
	
	private void merge(Set<JMStatisItem> items) {
		
		Set<JMStatisItem> result = new HashSet<>();
		
		//Map<String,OneItem> oneItems = new HashMap<>();
		
		Map<String,JMStatisItem> mprcItems = new HashMap<>();
		
		JMStatisItem nullSMMRpcItem = null;
		
		for(Iterator<JMStatisItem> ite = items.iterator(); ite.hasNext();) {
			JMStatisItem mi = ite.next();
			ite.remove();
			
			if(mi.getSmKey() == null) {
				//非RPC环境下的事件
				if(nullSMMRpcItem == null) {
					nullSMMRpcItem = mi;
					//第一个，不用处理，别的合并到这个选项下面
					continue;
				}
				
				Set<Short> types = mi.getTypeStatis().keySet();
				for(Short t : types) {
					Iterator<StatisItem> oiIte = mi.getTypeStatis().get(t).iterator();
					for(; oiIte.hasNext(); ) {
						StatisItem oi = oiIte.next();
						nullSMMRpcItem.addType(oi);
					}
				}
				
			} else {
				//合并同一个服务方法的统计参数
				//result.add(mi);
				
				JMStatisItem emi = mprcItems.get(mi.getKey());
				if(emi == null) {
					mprcItems.put(mi.getKey(),mi);
					result.add(mi);
				} else {
					Set<Short> types = mi.getTypeStatis().keySet();
					for(Short t : types) {
						Iterator<StatisItem> oiIte = mi.getTypeStatis().get(t).iterator();
						for(; oiIte.hasNext(); ) {
							StatisItem oi = oiIte.next();
							emi.addType(oi);
						}
					}
				}
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
		
		private JMStatisItem[] items = null;
		
		public Worker(JMStatisItem[] items) {
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
				
				//用于计算数据从创建到提交时间差，服务器以时间差为标准计算数据时间
				long curTime = TimeUtils.getCurTime();
				for(JMStatisItem i : items) {
					//logger.info("KEY:{}",i.getKey());
					/*for(StatisItem si : i.getTypeStatis().values()) {
						logger.info("T{}, V{}, TI{}",si.getType(),si.getVal(),si.getTime());
					}*/
					i.setSubmitTime(curTime);
				}
				
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
					/*
					logger.info("==========================================================");
					for(MRpcStatisItem mi: items) {
						logger.info("lid:" + mi.getLinkId() +", reqId: " + mi.getReqId()+", parentId: " + mi.getReqParentId());
					}
					*/
					
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
			} catch (Throwable e) {
				if(e instanceof CommonException) {
					CommonException ce = (CommonException)e;
					if(ce.getKey() == MC.MT_PACKET_TOO_MAX) {
						logger.warn("Resend items one by one");
						for(JMStatisItem item : this.items) {
							monitorServer.submitJMAsync(new JMStatisItem[] {item},item)
							.fail((code,msg,faiItem)-> {
								logger.error("fail to resend item:" + faiItem.toString(),"code:" + code+",msg="+msg);
							});
						}
						return;
					}
				}
				if(statusMonitorAdapter.isMonitoralbe()) {
					statusMonitorAdapter.getServiceCounter().add(MC.Ms_TaskFailItemCnt, items.length);
					//statusMonitorAdapter.getServiceCounter().add(MonitorConstant.Ms_TaskExceptionCount, items.length);
				}
				logger.error("MonitorClient.worker.run",e);
			}
		}
	}
	
	public boolean isServerReady() {
		return monitorServer != null && this.monitorServer.isReady();
	}
	
	public boolean canSubmit(ServiceMethod sm, Short t,String actName) {
		if(!this.checkerWorking || monitorServer == null || !this.monitorServer.isReady()) {
			return false;
		}
		return this.mscm.canSubmit(sm,t,actName);
	}

}
