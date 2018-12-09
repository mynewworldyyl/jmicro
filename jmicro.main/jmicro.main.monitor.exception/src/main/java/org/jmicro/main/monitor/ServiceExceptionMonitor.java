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

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.breaker.BreakerManager;
import org.jmicro.api.degrade.DegradeManager;
import org.jmicro.api.monitor.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.ServiceCounter;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.registry.BreakRule;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceKey;
import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月18日-下午9:39:50
 */
@Component
@Service(version="0.0.1", namespace="serviceExceptionMonitor",monitorEnable=0)
public class ServiceExceptionMonitor extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber,ITickerAction{

	//private final static Logger logger = LoggerFactory.getLogger(ServiceExceptionMonitor.class);
	
	private final Integer[] YTPES = new Integer[]{
		//服务器发生错误,返回ServerError异常
		MonitorConstant.CLIENT_REQ_EXCEPTION_ERR,
		//业务错误,success=false,此时接口调用正常
		//MonitorConstant.CLIENT_REQ_BUSSINESS_ERR,
		//请求超时
		MonitorConstant.CLIENT_REQ_TIMEOUT,
		//请求开始
		MonitorConstant.CLIENT_REQ_BEGIN,
		//异步请求成功确认包
		MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS,
		//同步请求成功
		MonitorConstant.CLIENT_REQ_OK
	};
	
	@Inject
	private DegradeManager degradeManager;
	
	@Inject
	private BreakerManager breakerManager;
	
	private Map<String,ServiceCounter> counters =  new HashMap<>();
	
	@JMethod("init")
	public void init() {
	}
	
	private String typeKey(String key, Integer type) {
		return key+ UniqueServiceKey.SEP + type ;
	}
	
	public void act(String key,Object attachement) {
		ServiceCounter counter = counters.get(key);
		
		ServiceMethod sm = (ServiceMethod)attachement;
		BreakRule rule = ((ServiceMethod)attachement).getBreakingRule();
		if(rule.isEnable()) {
			Double failPercent = getData(key, MonitorConstant.FAIL_PERCENT);
			if(failPercent > rule.getPercent()) {
				breakerManager.breakService(key,sm);
			}
		}
		
		for(Integer type : YTPES) {
			degradeManager.updateExceptionCnt(typeKey(key,type),
					new Double(counter.getAvg(type)).intValue());
		}
	}

	@Override
	@SMethod(needResponse=false)
	public void onSubmit(SubmitItem si) {
		
		String key = si.getSm().getKey().toKey(true,true,false);
		
		ServiceCounter counter = counters.get(key);
		if(counter == null) {
			//取常量池中的字符串实例做同步锁，保证基于服务方法标识这一级别的同步
			key = key.intern();
			synchronized(key) {
				counter = counters.get(key);
				if(counter == null) {
					counter = new ServiceCounter(key, YTPES,si.getSm().getTimeWindowInMillis());
					counters.put(key, counter);
					BreakRule rule = si.getSm().getBreakingRule();
					if(rule.isEnable()) {
						//开启了熔断机制，按熔断窗口的两倍时间做监听
						TimerTicker.getTimer(rule.getTimeInMilliseconds()*2).addListener(key, this,si.getSm());
					}
				}
			}
		}
		counter.increment(si.getType());
		
		/*BreakRule rule = si.getSm().getBreakingRule();
		if(!rule.isEnable()) {
			return;
		}
		
		if(si.getType() == MonitorConstant.CLIENT_REQ_TIMEOUT
				|| si.getType() == MonitorConstant.CLIENT_REQ_EXCEPTION_ERR) {
			updateBreaker(key,si);
		}*/
	}

	@Override
	public Double getData(String srvKey,Integer type) {
		ServiceCounter counter = counters.get(srvKey);
		if(counter == null) {
			return 0D;
		}
		Double result = 0D;
		switch(type) {
		case MonitorConstant.FAIL_PERCENT:
			Long totalReq = counter.get(MonitorConstant.CLIENT_REQ_BEGIN);
			if(totalReq != 0) {
				Long totalFail = counter.get(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR)+
						counter.get(MonitorConstant.CLIENT_REQ_TIMEOUT);
				result = (totalFail*1.0/totalReq)*100;
			}
			break;
		case MonitorConstant.TOTAL_REQ:
			result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_BEGIN);		
			break;
		case MonitorConstant.TOTAL_SUCCESS:
			result =  1.0 * counter.get(MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS)+
					counter.get(MonitorConstant.CLIENT_REQ_OK);
			break;
		case MonitorConstant.TOTAL_FAIL:
			result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR)+
			counter.get(MonitorConstant.CLIENT_REQ_TIMEOUT);
			break;
		case MonitorConstant.SUCCESS_PERCENT:
			totalReq = counter.get(MonitorConstant.CLIENT_REQ_BEGIN);
			if(totalReq != 0) {
				result =  1.0 * counter.get(MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS)+
						counter.get(MonitorConstant.CLIENT_REQ_OK);
						result = (result*1.0/totalReq)*100;
			}
			break;
		case MonitorConstant.TOTAL_TIMEOUT:
			result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_TIMEOUT);
			break;
		case MonitorConstant.TIMEOUT_PERCENT:
			totalReq = counter.get(MonitorConstant.CLIENT_REQ_BEGIN);
			if(totalReq != 0) {
				result = 1.0 * counter.get(MonitorConstant.CLIENT_REQ_TIMEOUT);
				result = (result/totalReq)*100;
			}
			break;
		}
		return result;
	}

	@Override
	public Integer[] intrest() {
		return YTPES;
	}

}


