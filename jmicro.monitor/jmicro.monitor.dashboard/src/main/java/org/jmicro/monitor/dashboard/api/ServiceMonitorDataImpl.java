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
package org.jmicro.monitor.dashboard.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.degrade.DegradeManager;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.monitor.IServiceMonitorData;
import org.jmicro.api.monitor.ServiceStatis;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.server.IWriteCallback;
import org.jmicro.common.Constants;

/**
 * @author Yulei Ye
 * @date 2018年10月22日-下午1:43:11
 */
@Component
@Service(monitorEnable=0,namespace="serviceMonitorData",version="0.0.1",retryCnt=0)
public class ServiceMonitorDataImpl implements IServiceMonitorData{

	private Map<String,Set<ServiceStatis>> statis = new HashMap<String,Set<ServiceStatis>>();
	
	private Map<Integer,IDataListener> listeners = new HashMap<>();
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject
	private IDataOperator dataOperator;
	
	@SMethod(streamCallback="serviceDataUpdater",retryCnt=0,maxSpeed="10s")
	@Override
	public Integer subsicribe(String service) {
		Integer id = idGenerator.getIntId(ServiceStatis.class);
		IWriteCallback sender = JMicroContext.get().getParam(Constants.CONTEXT_CALLBACK_SERVICE, null);
		IDataListener l = (path,data)->{
			sender.send(data);
		};
		listeners.put(id, l);
		dataOperator.addDataListener(DegradeManager.AVG_TIME_ROOT+service,l);
		return id;
	}
	
	@Override
	public void unsubsicribe(Integer id,String service) {
		IDataListener lis = listeners.get(id);
		dataOperator.removeDataListener(DegradeManager.AVG_TIME_ROOT+service, lis);
		listeners.remove(id);
	}
	
}