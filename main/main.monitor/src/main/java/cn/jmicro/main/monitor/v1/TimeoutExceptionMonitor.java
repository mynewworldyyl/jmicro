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
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import cn.jmicro.api.monitor.v1.SubmitItem;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.registry.UniqueServiceMethodKey;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月18日-下午9:39:50	
 */
//@Component(active=false)
//@Service(version="0.0.1", namespace="timeoutExceptionMonitor",monitorEnable=0,handler=Constants.SPECIAL_INVOCATION_HANDLER)
public class TimeoutExceptionMonitor extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(TimeoutExceptionMonitor.class);
	
	@Cfg("/Monitor/TimeoutExceptionMonitor/enable")
	private boolean enable = false;
	
	@Inject
	private DegradeManager degradeManager;
	
	private Map<String,Queue<ExceItem>> exceptinErrs =  new HashMap<String,Queue<ExceItem>>();
	private Map<String,Queue<ExceItem>> bussinessErrs =  new HashMap<String,Queue<ExceItem>>();
	
	private Timer ticker = new Timer("ServiceExceptionMonitor",true);
	
	private static class ExceItem{
		public long time = 0;
		public short type;
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
			degradeManager.updateExceptionCnt(e.getKey(), q.size()+"");
		}
	
	}

	@Override
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(SubmitItem[] sis) {
		for(SubmitItem si : sis) {
			if(si.getType() != MonitorConstant.REQ_TIMEOUT) {
				continue;
			}
			
			String service = null;
			if(si.getReq() != null) {
				IRequest req = (IRequest)si.getReq();
				service = req.getServiceName() + "|"
						+req.getMethod() + "|" + UniqueServiceMethodKey.paramsStr(req.getArgs());
			}else {
				service = si.getSm().getKey().getServiceName() + "|" + si.getSm().getKey().getMethod() /*+ "|" + UniqueServiceMethodKey.paramsStr(si.getReq().toString())*/;
			}
			
			ExceItem ei = new ExceItem();
			ei.time = si.getTime();
			ei.type = si.getType();
			
			if(MonitorConstant.CLIENT_SERVICE_ERROR == si.getType()){
				if(!exceptinErrs.containsKey(service)){
					exceptinErrs.put(service, new ConcurrentLinkedQueue<ExceItem>());
				}
				exceptinErrs.get(service).offer(ei);
			} else if(MonitorConstant.CLIENT_SERVICE_ERROR == si.getType()){
				if(!bussinessErrs.containsKey(service)){
					bussinessErrs.put(service, new ConcurrentLinkedQueue<ExceItem>());
				}
				bussinessErrs.get(service).offer(ei);
			}
		}
		
	}

	@Override
	public Short[] intrest() {
		return new Short[]{MonitorConstant.REQ_TIMEOUT};
	}

}
