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
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.degrade.DegradeManager;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月18日-下午9:39:50
 */
@Component
@Service(version="0.0.1", namespace="serviceExceptionMonitor",monitorEnable=0)
public class ServiceExceptionMonitor implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(ServiceExceptionMonitor.class);
	
	@Inject
	private DegradeManager degradeManager;
	
	private Map<String,Queue<ExceItem>> exceptinErrs =  new HashMap<String,Queue<ExceItem>>();
	private Map<String,Queue<ExceItem>> bussinessErrs =  new HashMap<String,Queue<ExceItem>>();
	
	private Timer ticker = new Timer("ServiceExceptionMonitor",true);
	
	private static class ExceItem{
		public long time = 0;
		public int type;
	}
	
	@JMethod("init")
	public void init() {
		ticker.schedule(new TimerTask(){
			@Override
			public void run() {
				doCheck(exceptinErrs);
				doCheck(bussinessErrs);
			}	
		}, 0, 5000);
	}
	
	private void doCheck(Map<String,Queue<ExceItem>> excepts) {

		long curtime = System.currentTimeMillis();
		long interval = 5*60*1000;
		for(Map.Entry<String, Queue<ExceItem>> e : excepts.entrySet()){
			Queue<ExceItem> q = e.getValue();
			do{
				ExceItem ei = q.peek();
				if( curtime - ei.time > interval) {
					q.poll();
				} else {
					break;
				}
				
			}while(true);
		}
		
		for(Map.Entry<String, Queue<ExceItem>> e : excepts.entrySet()){
			Queue<ExceItem> q = e.getValue();
			degradeManager.updateExceptionCnt(e.getKey(), q.size());
		}
	
	}

	@Override
	@SMethod(needResponse=false)
	public void onSubmit(SubmitItem si) {
		String service = null;
		if(si.getReq() != null) {
			IRequest req = (IRequest)si.getReq();
			service = req.getServiceName() + "|"
					+req.getMethod() + "|" + ServiceMethod.methodParamsKey(req.getArgs());
		}else {
			service = si.getServiceName() + "|" + si.getMethod() + "|" + ServiceMethod.methodParamsKey(si.getReqArgs());
		}
		
		ExceItem ei = new ExceItem();
		ei.time = si.getTime();
		ei.type = si.getType();
		
		if(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR == si.getType()){
			if(!exceptinErrs.containsKey(service)){
				exceptinErrs.put(service, new ConcurrentLinkedQueue<ExceItem>());
			}
			exceptinErrs.get(service).offer(ei);
		} else if(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR == si.getType()){
			if(!bussinessErrs.containsKey(service)){
				bussinessErrs.put(service, new ConcurrentLinkedQueue<ExceItem>());
			}
			bussinessErrs.get(service).offer(ei);
		}
	}

	@Override
	public Integer[] intrest() {
		return new Integer[]{MonitorConstant.CLIENT_REQ_EXCEPTION_ERR,
				MonitorConstant.CLIENT_REQ_BUSSINESS_ERR};
	}

}
