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
package cn.jmicro.main.monitor.v1;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.degrade.DegradeManager;
import cn.jmicro.api.monitor.v1.AbstractMonitorDataSubscriber;
import cn.jmicro.api.monitor.v1.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.v1.MonitorConstant;
import cn.jmicro.api.monitor.v1.ServiceStatis;
import cn.jmicro.api.monitor.v1.SubmitItem;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月18日-下午9:39:57
 */
//@Component
//@Service(version="0.0.1", namespace="serviceResponseTimeMonitor",monitorEnable=0,handler=Constants.SPECIAL_INVOCATION_HANDLER)
public class ServiceResponseTimeMonitor extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(ServiceResponseTimeMonitor.class);
	
	@Cfg("/Monitor/ServiceResponseTimeMonitor/enable")
	private boolean enable = false;
	
	@Inject
	private DegradeManager degradeManager;
	
	@Cfg(value="/ServiceResponseTimeMonitor/openDebug",required=false)
	private boolean openDebug = true;
	
	private volatile Map<Long,AvgResponseTimeItem> reqRespAvgList = new HashMap<>();
	
	private volatile Map<String,Queue<Long>> reqRespAvgs =  new HashMap<String,Queue<Long>>();
	
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
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(SubmitItem[] sis) {
		for(SubmitItem si : sis) {
			//logger.debug("Service: "+si.getServiceName());
			if(openDebug) {
				logger.debug("onSubmit si: {} ",si);
			}
			if(MonitorConstant.REQ_START == si.getType()){
				AvgResponseTimeItem i = new AvgResponseTimeItem();
				IRequest req = (IRequest)si.getReq();
				i.reqId = req.getRequestId();
				i.service = si.getSm().getKey().toKey(false,false,false);
				i.startTime = si.getTime();
				reqRespAvgList.put(i.reqId, i);
			}else if(MonitorConstant.REQ_SUCCESS == si.getType()
					/*|| MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS == si.getType()*/){
				IRequest req = (IRequest)si.getReq();
				AvgResponseTimeItem i = reqRespAvgList.get(req.getRequestId());
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
		
	}

	@Override
	public Short[] intrest() {
		return new Short[]{MonitorConstant.REQ_START,
				MonitorConstant.REQ_SUCCESS/*,
				MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS*/};
	}

}
