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
package org.jmicro.main.monitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.ServiceCounter;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月18日-下午9:39:50
 */
@Component
@Service(version="0.0.1", namespace="serviceExceptionMonitor",monitorEnable=0
,handler=Constants.SPECIAL_INVOCATION_HANDLER)
public class ServiceReqMonitor extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceReqMonitor.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	private final Map<String,Long> timeoutList = new ConcurrentHashMap<>();
	
	@Cfg(value="/ServiceReqMonitor/openDebug")
	private boolean openDebug = true;
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {});
		 Utils.getIns().waitForShutdown();
	}

	@Inject
	private PubSubManager pubSubManager;
	
	private Map<String,ServiceCounter> counters =  new ConcurrentHashMap<>();
	
	private ITickerAction tickerAct = new ITickerAction() {
		public void act(String key,Object attachement) {
			act0(key,attachement);
		}
	};
	
	@JMethod("init")
	public void init() {}
	
	public void act0(String key,Object attachement) {
		ServiceCounter counter = counters.get(key);
		ServiceMethod sm = (ServiceMethod)attachement;
		
		if(timeoutList.containsKey(key) && ( (System.currentTimeMillis()-timeoutList.get(key)) > 60000 )) {
			if(this.openDebug) {
				logger.warn("More than one minutes have no submit data,DELETE it :{} ",key);
			}
			//1分钟没有数据更新，服务在停用状态，不需要再更新其统计数据，直到下一次有数据更新为止
			TimerTicker.getTimer(timers,TimeUtils.getMilliseconds(sm.getCheckInterval(),
					sm.getBaseTimeUnit())).removeListener(key,false);
			return;
		}
		
		//提交数据到ZK
		Map<Integer,Double> data = new HashMap<>();
		for(Integer type : AbstractMonitorDataSubscriber.YTPES) {
			//Double v = new Double(counter.getAvgWithEx(type,TimeUtils.getTimeUnit(sm.getBaseTimeUnit())));
			data.put(type, counter.getTotal(type));
			//degradeManager.updateExceptionCnt(typeKey(key,type),v.toString());
		}
		
		data.put(MonitorConstant.STATIS_TOTAL_RESP, ServiceCounter.getData(counter,MonitorConstant.STATIS_TOTAL_RESP));
		data.put(MonitorConstant.STATIS_QPS,  ServiceCounter.getData(counter,MonitorConstant.STATIS_QPS));
		data.put(MonitorConstant.STATIS_SUCCESS_PERCENT,  ServiceCounter.getData(counter,MonitorConstant.STATIS_SUCCESS_PERCENT));
		data.put(MonitorConstant.STATIS_FAIL_PERCENT,  ServiceCounter.getData(counter,MonitorConstant.STATIS_FAIL_PERCENT));
		
		PSData psData = new PSData();
		psData.setData(data);
		psData.setTopic(MonitorConstant.TEST_SERVICE_METHOD_TOPIC);
		psData.put(Constants.SERVICE_METHOD_KEY, sm);
		
		//将统计数据分发出去
		boolean f = pubSubManager.publish(psData);
		if(!f) {
			logger.warn("Fail to publish topic: {}",psData.getTopic());
		}
		
		if(openDebug) {
			/*
			  System.out.println("======================================================");
			  logger.debug("总请求:{}, 总响应:{}, TO:{}, TOF:{}, QPS:{}",
					counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_BEGIN),
					counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR,MonitorConstant.CLIENT_REQ_OK,MonitorConstant.CLIENT_REQ_EXCEPTION_ERR)
					,counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_TIMEOUT)
					,counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL)
					,counter.getAvg(TimeUnit.SECONDS,MonitorConstant.CLIENT_REQ_OK)
					);*/
	
			/*logger.debug("总请求:{}, 总响应:{}",
					this.getData(key, MonitorConstant.STATIS_TOTAL_REQ),
					this.getData(key, MonitorConstant.STATIS_TOTAL_RESP));*/
		}
	}

	@Override
	@SMethod(needResponse=false)
	public void onSubmit(Set<SubmitItem> sis) {
		
		for(SubmitItem si : sis) {
			ServiceMethod sm = si.getSm();
			if(sm == null) {
				logger.error("Service Method not found: "+si.toString());
				continue;
			}
			
			String key = sm.getKey().toKey(true,true,true);
			timeoutList.put(key, System.currentTimeMillis());
			
			TimerTicker timer = TimerTicker.getTimer(timers,TimeUtils.getMilliseconds(sm.getCheckInterval(),
					sm.getBaseTimeUnit()));
			ServiceCounter counter = counters.get(key);
			if(counter == null) {
				//取常量池中的字符串实例做同步锁,保证基于服务方法标识这一级别的同步
				key = key.intern();
				synchronized(key) {
					counter = counters.get(key);
					if(counter == null) {
						counter = new ServiceCounter(key, YTPES,sm.getTimeWindow(),sm.getSlotSize()
								,TimeUtils.getTimeUnit(sm.getBaseTimeUnit()));
						counters.put(key, counter);
						//BreakRule rule = si.getSm().getBreakingRule();
						//定时收集统计数据
						timer.addListener(key,tickerAct,sm);
						if(this.openDebug) {
							logger.info("Create counter for service {},timewindow[{}],unit[{}],slotSize[{}],checkInterfal[{}]", 
									key,sm.getTimeWindow(),sm.getBaseTimeUnit(),sm.getSlotSize(),sm.getCheckInterval());
						}
					}
				}
			}
			
			if(!timer.container(key)) {
				//有新数据更新,激活统计数据时钟
				timer.addListener(key,tickerAct,sm);
				if(this.openDebug) {
					logger.info("Create counter for service {},timewindow[{}],unit[{}],slotSize[{}],checkInterfal[{}]", 
							key,sm.getTimeWindow(),sm.getBaseTimeUnit(),sm.getSlotSize(),sm.getCheckInterval());
				}
			}
			counter.increment(si.getType());
		}
		
	}

	@Override
	public Double getData(String srvKey,Integer type) {
		ServiceCounter counter = counters.get(srvKey);
		if(counter == null) {
			return 0D;
		}
		Double result = 0D;
		switch(type) {
		case MonitorConstant.STATIS_FAIL_PERCENT:
			Long totalReq = counter.get(MonitorConstant.CLIENT_REQ_BEGIN);
			if(totalReq != 0) {
				Long totalFail = counter.getValueWithEx(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR,MonitorConstant.CLIENT_REQ_TIMEOUT).longValue();
				result = (totalFail*1.0/totalReq)*100;
			}
			break;
		case MonitorConstant.STATIS_TOTAL_REQ:
			result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_BEGIN);		
			break;
		case MonitorConstant.STATIS_TOTAL_RESP:
			result = counter.getValueWithEx(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR,MonitorConstant.CLIENT_REQ_OK,MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
			break;
		case MonitorConstant.STATIS_TOTAL_SUCCESS:
			result =  1.0 * counter.get(MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS)+
					counter.get(MonitorConstant.CLIENT_REQ_OK);
			break;
		case MonitorConstant.STATIS_TOTAL_FAIL:
			result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR)+
			counter.get(MonitorConstant.CLIENT_REQ_TIMEOUT);
			break;
		case MonitorConstant.STATIS_SUCCESS_PERCENT:
			totalReq = counter.get(MonitorConstant.CLIENT_REQ_BEGIN);
			if(totalReq != 0) {
				result =  1.0 * counter.get(MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS)+
						counter.get(MonitorConstant.CLIENT_REQ_OK);
						result = (result*1.0/totalReq)*100;
			}
			break;
		case MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL:
			result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL);
			break;
		case MonitorConstant.STATIS_TIMEOUT_PERCENT:
			totalReq = counter.get(MonitorConstant.CLIENT_REQ_BEGIN);
			if(totalReq != 0) {
				result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL);
				result = (result/totalReq)*100;
			}
			break;
		case MonitorConstant.STATIS_QPS:
			result = counter.getQps(TimeUnit.SECONDS,MonitorConstant.CLIENT_REQ_OK);
		}
		return result;
	}

	@Override
	public Integer[] intrest() {
		return YTPES;
	}

}


