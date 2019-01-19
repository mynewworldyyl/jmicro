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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.degrade.DegradeManager;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.IServiceMonitorData;
import org.jmicro.api.monitor.ServiceStatis;
import org.jmicro.api.net.IWriteCallback;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;

/**
 * @author Yulei Ye
 * @date 2018年10月22日-下午1:43:11
 */
@Component
@Service(monitorEnable=0,retryCnt=0)
public class ServiceMonitorDataImpl implements IServiceMonitorData{

	private Map<String,Set<ServiceStatis>> statis = new HashMap<String,Set<ServiceStatis>>();
	
	private Map<Integer,IDataListener> listeners = new HashMap<>();
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IDataOperator dataOperator;
	
	@Override
	public Integer subsicribe(String service) {
		Integer id = idGenerator.getIntId(ServiceStatis.class);
		final String lkey = DegradeManager.AVG_TIME_ROOT+service;
		
		IWriteCallback sender = JMicroContext.get().getParam(Constants.CONTEXT_CALLBACK_SERVICE, null);
		
		final IDataListener l = new IDataListener() {
			public void dataChanged(String path,String data) {
				if(!sender.send(data)) {
					dataOperator.removeDataListener(lkey, this);
				}
			}
		};
		
		listeners.put(id, l);
		dataOperator.addDataListener(lkey,l);
		return id;
	}
	
	@Override
	public void unsubsicribe(Integer id,String service) {
		IDataListener lis = listeners.get(id);
		dataOperator.removeDataListener(DegradeManager.AVG_TIME_ROOT+service, lis);
		listeners.remove(id);
	}

	@Override
	public List<String> getAllServices() {
		List<String> services = dataOperator.getChildren(DegradeManager.AVG_TIME_ROOT);
		return services;
	}
	
}
