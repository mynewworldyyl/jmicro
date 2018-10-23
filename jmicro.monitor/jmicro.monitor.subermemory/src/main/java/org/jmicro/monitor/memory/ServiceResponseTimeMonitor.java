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
package org.jmicro.monitor.memory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.degrade.DegradeManager;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.ServiceStatis;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月18日-下午9:39:57
 */
@Component
@Service(version="0.0.1", namespace="serviceResponseTimeMonitor",monitorEnable=0)
public class ServiceResponseTimeMonitor implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(ServiceResponseTimeMonitor.class);
	
	@Inject
	private DegradeManager degradeManager;
	
	@Cfg(value="/ServiceResponseTimeMonitor/openDebug",required=false)
	private boolean openDebug = true;
	
	private volatile Map<Long,AvgResponseTimeItem> reqRespAvgList = new HashMap<>();
	
	private volatile Map<String,Queue<Long>> reqRespAvgs =  new HashMap<String,Queue<Long>>();
	
	//private Map<String,Long> firstResponseTime =  new HashMap<String,Long>();
	
	//private List<ServiceStatis> statis = new ArrayList<>(1000);
	
	private Timer ticker = new Timer("MemoryResponseTimeMonitor",true);
	
	private static class AvgResponseTimeItem {
		public long reqId;
		public String service;
		public long startTime;
		//public long endtime;
	}
	
	@JMethod("init")
	public void init() {
		ticker.schedule(new TimerTask(){
			@Override
			public void run() {
				for(Map.Entry<String, Queue<Long>> e : reqRespAvgs.entrySet()){
					
					String srv = e.getKey();
					Queue<Long> q = e.getValue();
					
					ServiceStatis sts = new ServiceStatis(srv,System.currentTimeMillis(),sum(q)/q.size());
					//statis.add(sts);
					String json = JsonUtils.getIns().toJson(sts);
					if(openDebug) {
						logger.debug("update srv {}, ServiceStatis {}",srv,sts);
					}
					degradeManager.updateAvgResponseTime(srv,json);
				}
			}	
		}, 0, 5000);
	}
	
	protected int sum(Queue<Long> q) {
		int sum = 0;
		Iterator<Long> ite = q.iterator();
		while(ite.hasNext()) {
			sum += ite.next();
		}
		return sum;
	}

	@Override
	@SMethod(needResponse=false)
	public void onSubmit(SubmitItem si) {
		//logger.debug("Service: "+si.getServiceName());
		if(openDebug) {
			logger.debug("onSubmit si: {} ",si);
		}
		if(MonitorConstant.CLIENT_REQ_BEGIN == si.getType()){
			AvgResponseTimeItem i = new AvgResponseTimeItem();
			i.reqId = si.getReqId();
			i.service = ServiceItem.methodKey(ServiceItem.serviceName(si.getServiceName(), si.getNamespace(),
					si.getVersion()), si.getMethod(), si.getReqArgsStr());
			i.startTime = si.getTime();
			reqRespAvgList.put(i.reqId, i);
		}else if(MonitorConstant.CLIENT_REQ_OK == si.getType()
				|| MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS == si.getType()){
			AvgResponseTimeItem i = reqRespAvgList.get(si.getReqId());
			if(i == null){
				return;
			}
			if(!reqRespAvgs.containsKey(i.service)) {
				reqRespAvgs.put(i.service, new LinkedList<Long>());
			}
			
			reqRespAvgList.remove(i.reqId);
			Queue<Long> qtime = reqRespAvgs.get(i.service);
			qtime.offer(si.getTime()-i.startTime);
			if(qtime.size() > 1000){
				//only keep the last 1000 element
				qtime.poll();
			}
		}
	}

	@Override
	public Integer[] intrest() {
		return new Integer[]{MonitorConstant.CLIENT_REQ_BEGIN,
				MonitorConstant.CLIENT_REQ_OK,
				MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS};
	}

}
