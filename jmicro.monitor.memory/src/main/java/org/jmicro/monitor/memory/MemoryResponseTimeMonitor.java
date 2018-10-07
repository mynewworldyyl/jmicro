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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.IMonitorSubmitWorker;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItem;

@Component
@Service(version="0.0.1", namespace="memoryResponseTimeMonitor", monitorEnable=0)
public class MemoryResponseTimeMonitor implements IMonitorSubmitWorker {

	private Map<Long,AvgResponseTimeItem> reqRespAvgList = new HashMap<>();
	
	private Map<String,Queue<Long>> reqRespAvgs =  new HashMap<String,Queue<Long>>();
	
	private static class AvgResponseTimeItem {
		public long reqId;
		public String service;
		public long startTime;
		//public long endtime;
	}
	
	@JMethod("init")
	public void init() {
		
	}
	
	@Override
	public void submit(SubmitItem si) {
		if(MonitorConstant.CLIENT_REQ_BEGIN == si.getType()){
			AvgResponseTimeItem i = new AvgResponseTimeItem();
			i.reqId = si.getReqId();
			i.service = si.getServiceName()+"|"+si.getMethod()+"|"+si.getReqArgsStr();
			i.startTime = si.getTime();
			reqRespAvgList.put(i.reqId, i);
		}else if(MonitorConstant.CLIENT_RESP_OK == si.getType()){
			AvgResponseTimeItem i = reqRespAvgList.get(si.getReqId());
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
