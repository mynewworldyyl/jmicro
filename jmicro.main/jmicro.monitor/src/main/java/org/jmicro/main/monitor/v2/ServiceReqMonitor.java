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
package org.jmicro.main.monitor.v2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.mng.ReportData;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.ServiceCounter;
import org.jmicro.api.monitor.v2.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.v2.IMonitorDataSubscriber;
import org.jmicro.api.monitor.v2.MRpcItem;
import org.jmicro.api.monitor.v2.MonitorClient;
import org.jmicro.api.monitor.v2.OneItem;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Yulei Ye
 * @date 2019年7月15日
 */
@Component
@Service(version="0.0.1", namespace="rpcStatisMonitor",monitorEnable=0,handler=Constants.SPECIAL_INVOCATION_HANDLER)
public class ServiceReqMonitor  extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceReqMonitor.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	private final Map<String,Long> timeoutList = new ConcurrentHashMap<>();
	
	@Cfg("/Monitor/ServiceReqMonitor/enable")
	private boolean enable = false;
	
	@Cfg(value="/ServiceReqMonitor/openDebug")
	private boolean openDebug = true;
	
	@Inject
	private PubSubManager pubSubManager;
	
	@Inject
	private MonitorClient mm;
	
	private Map<String,ServiceCounter> counters =  new ConcurrentHashMap<>();
	
	@JMethod("init")
	public void init() {
		String skey = this.skey("rpcStatisMonitor", "0.0.1");
		mm.registType(skey, MonitorConstant.STATIS_TYPES);
	}
	
	public void act0(String key,Object attachement) {
		//ServiceCounter counter = counters.get(key);
		ServiceMethod sm = (ServiceMethod)attachement;
		
		if(timeoutList.containsKey(key) && ((System.currentTimeMillis() - timeoutList.get(key)) > 60000 )) {
			if(this.openDebug) {
				logger.warn("More than one minutes have no submit data,DELETE it : {} ",key);
			}
			//1分钟没有数据更新，服务在停用状态，不需要再更新其统计数据，直到下一次有数据更新为止
			TimerTicker.getTimer(timers,TimeUtils.getMilliseconds(sm.getCheckInterval(),
					sm.getBaseTimeUnit())).removeListener(key,false);
			return;
		}
		
		ReportData rd = this.getData(key, MonitorConstant.STATIS_TYPES, 
				new String[] {MonitorConstant.PREFIX_QPS,MonitorConstant.PREFIX_TOTAL_PERCENT});
		
		PSData psData = new PSData();
		psData.setData(rd);
		psData.setTopic(MonitorConstant.TEST_SERVICE_METHOD_TOPIC);
		psData.put(Constants.SERVICE_METHOD_KEY, sm);
		
		//将统计数据分发出去
		long f = pubSubManager.publish(psData);
		if(f != PubSubManager.PUB_OK) {
			logger.warn("Fail to publish topic: {},result:{}",psData.getTopic(),f);
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
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(MRpcItem[] sis) {
		
		for(MRpcItem si : sis) {
			if(openDebug) {
				log(si);
			}
			long windowSize = 3000; //5分钟
			long slotInterval = 1;
			TimeUnit tu = TimeUnit.SECONDS;
			String key = null;
			
			if(StringUtils.isNotEmpty(si.getInstanceName())) {
				doStatis(si,si.getInstanceName(),windowSize,slotInterval,tu);
			}
			
			/*if(StringUtils.isNotEmpty(si.getRemoteHost()) && StringUtils.isNotEmpty(si.getRemotePort())) {
				 key = si.getRemoteHost()+":"+si.getRemotePort();
				 doStatis(si,key,windowSize,slotSize,tu);
			}*/
			
			ServiceMethod sm = si.getSm();
			
			if(sm != null) {
				
				 key = sm.getKey().getUsk().toKey(true, true, true);
				
				 doStatis(si,key,windowSize,slotInterval,tu);
				
				 //服务数据统计
				 key = sm.getKey().getUsk().toSnv();
				 doStatis(si,key,windowSize,slotInterval,tu);
				 
				 windowSize = sm.getTimeWindow();
				 slotInterval = sm.getSlotInterval();
				 tu = TimeUtils.getTimeUnit(sm.getBaseTimeUnit());
				 
				 //服务方法数据统计
				 key = sm.getKey().toSnvm();
				 doStatis(si,key,windowSize,slotInterval,tu);
				 
				 //服务实例方法
				 key = sm.getKey().toKey(true,true,true);
				 doStatis(si,key,windowSize,slotInterval,tu);
			}
		}
		
	}

	private void log(MRpcItem si) {
		for(OneItem oi : si.getItems()) {
			StringBuffer sb = new StringBuffer();
			sb.append("GOT: " + MonitorConstant.MONITOR_VAL_2_KEY.get(oi.getType()));
			if(si.getSm() != null) {
				sb.append(", SM: ").append(si.getSm().getKey().getMethod());
			}
			sb.append(", reqId: ").append(si.getReqId());
			logger.debug(sb.toString()); 
		}
		
	}

	private void doStatis(MRpcItem si, String key,long windowSize,long slotInterval,TimeUnit tu) {
		ServiceCounter counter = counters.get(key);
		timeoutList.put(key, System.currentTimeMillis());
		
		if(counter == null) {
			//取常量池中的字符串实例做同步锁,保证基于服务方法标识这一级别的同步
			key = key.intern();
			synchronized(key) {
				counter = counters.get(key);
				if(counter == null) {
					counter = new ServiceCounter(key, MonitorConstant.STATIS_TYPES,windowSize,slotInterval,tu);
					counters.put(key, counter);
				}
			}
		}
		
		for(OneItem oi : si.getItems()) {
			/* if(openDebug) {
					logger.debug("GOT: " + MonitorConstant.MONITOR_VAL_2_KEY.get(oi.getType()) + " , KEY: " 
			 + key + " , Num: " + oi.getNum() + " , Val: " + oi.getVal());
			 }*/
			 if(MonitorConstant.CLIENT_IOSESSION_READ == oi.getType()
				|| MonitorConstant.SERVER_IOSESSION_READ == oi.getType()) {
				 counter.add(oi.getType(), oi.getVal());
			 } else {
				 counter.add(oi.getType(), oi.getNum());
			 }
		}
		
	}

	public ReportData getData(String srvKey, Short[] types, String[] dataType) {
		ReportData result = new ReportData();
		if(StringUtils.isEmpty(srvKey)) {
			return result; 
		}
		
		ServiceCounter counter = counters.get(srvKey);
		if(counter == null || types == null ||types.length == 0 
				|| dataType == null || dataType.length == 0) {
			return result;
		}
		
		for(String dt : dataType) {
			switch(dt) {
				case MonitorConstant.PREFIX_QPS:
					result.setQps(this.getQpsData(counter,types));	
				    break;
				case MonitorConstant.PREFIX_TOTAL_PERCENT:
					result.setPercent(this.getPercentData(counter,types));
					break;
				case MonitorConstant.PREFIX_CUR_PERCENT:
					result.setCurPercent(this.getCurPercentData(counter,types));
					break;
				case MonitorConstant.PREFIX_TOTAL:
					result.setTotal(this.getTotalData(counter,types));
					break;
				case MonitorConstant.PREFIX_CUR:
					result.setCur(this.getCurData(counter,types));
					break;
			}
		}
		return result;
	}

	private Double[] getCurPercentData(ServiceCounter counter, Short[] types) {

		Double[] datas = new Double[types.length];
		
		for(int i = 0; i < types.length; i++) {
			Double val = 0D;
			if(types[i] == null || !counter.existType(types[i])) {
				datas[i] = 0D;
				continue;
			}
			
			switch(types[i]) {
			case MonitorConstant.STATIS_FAIL_PERCENT:
				double totalReq = counter.get(MonitorConstant.REQ_START);
				if(totalReq > 0) {
					double totalFail = counter.getByTypes(MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR,MonitorConstant.REQ_TIMEOUT_FAIL,MonitorConstant.REQ_ERROR);
					if(totalFail > 0) {
						val = (totalFail/totalReq)*100;
					}
				}
				break;
			
			case MonitorConstant.STATIS_SUCCESS_PERCENT:
				totalReq = counter.get(MonitorConstant.REQ_START);
				if(totalReq > 0) {
					val =  1.0 * counter.getByTypes(MonitorConstant.REQ_SUCCESS,MonitorConstant.CLIENT_SERVICE_ERROR);
					if(val > 0) {
						val = (val*1.0/totalReq)*100;
					}
				}
				break;
			
			case MonitorConstant.STATIS_TIMEOUT_PERCENT:
				totalReq = counter.get(MonitorConstant.REQ_START);
				if(totalReq >= 0) {
					val = 1.0 * counter.getByTypes(MonitorConstant.REQ_TIMEOUT_FAIL);
					if(val > 0) {
						val = (val/totalReq)*100;
					}
				}
				break;
			default:
				val = ServiceCounter.takePercent(counter, types[i]);
			}
			
			datas[i] = val;
		}
		
		return datas;
	}

	private Long[] getCurData(ServiceCounter counter, Short[] types) {
		Long[] datas = new Long[types.length];
		for(int i = 0; i < types.length; i++) {
			if(types[i] != null && counter.existType(types[i])) {
				datas[i] = counter.get(types[i]);
			}else {
				datas[i] = 0L;
			}
		}
		return datas;
	}

	private Double[] getPercentData(ServiceCounter counter, Short[] types) {
		Double[] datas = new Double[types.length];
		
		for(int i = 0; i < types.length; i++) {
			Double val = 0D;
			if(types[i] == null || !counter.existType(types[i])) {
				datas[i] = 0D;
				continue;
			}
			
			switch(types[i]) {
			case MonitorConstant.STATIS_FAIL_PERCENT:
				double totalReq = counter.getTotal(MonitorConstant.REQ_START);
				if(totalReq > 0) {
					double totalFail = counter.getTotal(MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR,MonitorConstant.REQ_TIMEOUT_FAIL,MonitorConstant.REQ_ERROR);
					if(totalFail > 0) {
						val = (totalFail/totalReq)*100;
					}
					
				}
				break;
			
			case MonitorConstant.STATIS_SUCCESS_PERCENT:
				totalReq = counter.getTotal(MonitorConstant.REQ_START);
				if(totalReq > 0) {
					val =  1.0 * counter.getTotal(MonitorConstant.REQ_SUCCESS,MonitorConstant.CLIENT_SERVICE_ERROR);
					if(val > 0) {
						val = (val*1.0/totalReq)*100;
					}
					
				}
				break;
			
			case MonitorConstant.STATIS_TIMEOUT_PERCENT:
				totalReq = counter.getTotal(MonitorConstant.REQ_START);
				if(totalReq > 0) {
					val = 1.0 * counter.getTotal(MonitorConstant.REQ_TIMEOUT_FAIL);
					if(val > 0) {
						val = (val/totalReq)*100;
					}
				}
				break;
			default:
				val = ServiceCounter.takePercent(counter, types[i]);
			}
			
			datas[i] = val;
		}
		
		return datas;
	}

	private Double[] getQpsData(ServiceCounter counter, Short[] types) {
		Double[] datas = new Double[types.length];
		for(int i = 0; i < types.length; i++) {
			if(types[i] != null && counter.existType(types[i])) {
				datas[i] = counter.getQps(TimeUnit.SECONDS, types[i]);
			} else {
				datas[i] = 0D;
			}
			
		}
		return datas;
	}

	private Long[] getTotalData(ServiceCounter counter, Short[] types) {
		Long[] datas = new Long[types.length];
		for(int i = 0; i < types.length; i++) {
			if(types[i] != null && counter.existType(types[i])) {
				datas[i] = counter.getTotal(types[i]);
			}else {
				datas[i] = 0L;
			}
		}
		return datas;
	}

}


