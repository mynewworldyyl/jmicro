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
package cn.jmicro.main.monitor.v2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcStatisItem;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.StatisItem;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.monitor.api.AbstractMonitorDataSubscriber;


/**
 * @author Yulei Ye
 * @date 2019年7月15日
 */
@Component
@Service(version="0.0.1", namespace="rpcStatisMonitor",monitorEnable=0)
public class ServiceReqMonitor  extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceReqMonitor.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	private final Map<String,Long> timeoutList = new ConcurrentHashMap<>();
	
	private final Map<String,Short> MT_Key2Val = MC.MT_Key2Val;
	
	@Cfg("/Monitor/ServiceReqMonitor/enable")
	private boolean enable = false;
	
	@Cfg(value="/ServiceReqMonitor/openDebug")
	private boolean openDebug = true;
	
	@Inject
	private PubSubManager pubSubManager;
	
	@Inject
	private IDataOperator op;
	
	private Map<String,ServiceCounter> counters =  new ConcurrentHashMap<>();
	
	public void ready() {
		String skey = this.skey("rpcStatisMonitor", "0.0.1");
		registType(op,skey, MC.MT_TYPES_ARR);
	}
	
	public void act0(String key,Object attachement) {
		//ServiceCounter counter = counters.get(key);
		ServiceMethod sm = (ServiceMethod)attachement;
		
		if(timeoutList.containsKey(key) && ((TimeUtils.getCurTime() - timeoutList.get(key)) > 60000 )) {
			if(this.openDebug) {
				logger.warn("More than one minutes have no submit data,DELETE it : {} ",key);
			}
			//1分钟没有数据更新，服务在停用状态，不需要再更新其统计数据，直到下一次有数据更新为止
			TimerTicker.getTimer(timers,TimeUtils.getMilliseconds(sm.getCheckInterval(),
					sm.getBaseTimeUnit())).removeListener(key,false);
			return;
		}
		
		ReportData rd = this.getData(key, MC.STATIS_TYPES_ARR, 
				new String[] {MC.PREFIX_QPS,MC.PREFIX_TOTAL_PERCENT});
		
		PSData psData = new PSData();
		psData.setData(rd);
		psData.setTopic(MC.TEST_SERVICE_METHOD_TOPIC);
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
					);
			*/
	
			/*logger.debug("总请求:{}, 总响应:{}",
					this.getData(key, MonitorConstant.STATIS_TOTAL_REQ),
					this.getData(key, MonitorConstant.STATIS_TOTAL_RESP));
					*/
		}
	}

	@Override
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(MRpcStatisItem[] sis) {
		
		for(MRpcStatisItem si : sis) {
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
			
			
			
			/*if(si.getKey() != null) {
				 UniqueServiceMethodKey sm = UniqueServiceMethodKey.fromKey(si.getKey());
				 key = sm.toKey(true, true, true);
				
				 doStatis(si,key,windowSize,slotInterval,tu);
				
				 //服务数据统计
				 key = sm.getUsk().toSnv();
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
				 
				 if(!Utils.isEmpty(si.getActName())) {
					 doStatis(si,si.getActName(),windowSize,slotInterval,tu);
				 }
				 
			}*/
			
		}
		
	}
	

	private void doStatis(MRpcStatisItem si, String key,long windowSize,long slotInterval,TimeUnit tu) {
		ServiceCounter counter = counters.get(key);
		timeoutList.put(key, TimeUtils.getCurTime());
		
		if(counter == null) {
			//取常量池中的字符串实例做同步锁,保证基于服务方法标识这一级别的同步
			key = key.intern();
			synchronized(key) {
				counter = counters.get(key);
				if(counter == null) {
					counter = new ServiceCounter(key, MC.MT_TYPES_ARR,windowSize,slotInterval,tu);
					counters.put(key, counter);
				}
			}
		}
		
		for(Short type : si.getTypeStatis().keySet()) {
			List<StatisItem> items = si.getTypeStatis().get(type);
			for(StatisItem oi : items) {
			/* if(openDebug) {
					logger.debug("GOT: " + MonitorConstant.MONITOR_VAL_2_KEY.get(oi.getType()) + " , KEY: " 
			 + key + " , Num: " + oi.getNum() + " , Val: " + oi.getVal());
			 }*/
			 counter.add(oi.getType(), oi.getVal());
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
				case MC.PREFIX_QPS:
					result.setQps(this.getQpsData(counter,types));
				    break;
				case MC.PREFIX_TOTAL_PERCENT:
					result.setPercent(this.getPercentData(counter,types));
					break;
				case MC.PREFIX_CUR_PERCENT:
					result.setCurPercent(this.getCurPercentData(counter,types));
					break;
				case MC.PREFIX_TOTAL:
					result.setTotal(this.getTotalData(counter,types));
					break;
				case MC.PREFIX_CUR:
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
			case MC.STATIS_FAIL_PERCENT:
				double totalReq = counter.get(MC.MT_REQ_START);
				if(totalReq > 0) {
					double totalFail = counter.getByTypes(MC.MT_CLIENT_RESPONSE_SERVER_ERROR,MC.MT_REQ_TIMEOUT_FAIL,MC.MT_REQ_ERROR);
					if(totalFail > 0) {
						val = (totalFail/totalReq)*100;
					}
				}
				break;
			
			case MC.STATIS_SUCCESS_PERCENT:
				totalReq = counter.get(MC.MT_REQ_START);
				if(totalReq > 0) {
					val =  1.0 * counter.getByTypes(MC.MT_REQ_SUCCESS,MC.MT_SERVICE_ERROR);
					if(val > 0) {
						val = (val*1.0/totalReq)*100;
					}
				}
				break;
			
			case MC.STATIS_TIMEOUT_PERCENT:
				totalReq = counter.get(MC.MT_REQ_START);
				if(totalReq >= 0) {
					val = 1.0 * counter.getByTypes(MC.MT_REQ_TIMEOUT_FAIL);
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
			case MC.STATIS_FAIL_PERCENT:
				double totalReq = counter.getTotal(MC.MT_REQ_START);
				if(totalReq > 0) {
					double totalFail = counter.getTotal(MC.MT_CLIENT_RESPONSE_SERVER_ERROR,MC.MT_REQ_TIMEOUT_FAIL,MC.MT_REQ_ERROR);
					if(totalFail > 0) {
						val = (totalFail/totalReq)*100;
					}
					
				}
				break;
			
			case MC.STATIS_SUCCESS_PERCENT:
				totalReq = counter.getTotal(MC.MT_REQ_START);
				if(totalReq > 0) {
					val =  1.0 * counter.getTotal(MC.MT_REQ_SUCCESS,MC.MT_SERVICE_ERROR);
					if(val > 0) {
						val = (val*1.0/totalReq)*100;
					}
					
				}
				break;
			
			case MC.STATIS_TIMEOUT_PERCENT:
				totalReq = counter.getTotal(MC.MT_REQ_START);
				if(totalReq > 0) {
					val = 1.0 * counter.getTotal(MC.MT_REQ_TIMEOUT_FAIL);
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


