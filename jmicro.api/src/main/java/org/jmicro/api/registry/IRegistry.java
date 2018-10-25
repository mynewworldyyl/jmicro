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
package org.jmicro.api.registry;

import java.util.Set;

import org.jmicro.api.raft.IDataOperator;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:20
 */
public interface IRegistry{

	void regist(ServiceItem si);
	
	void update(ServiceItem si);
	
	void unregist(ServiceItem si);
	
	Set<ServiceItem> getServices(String serviceName,String method,Class<?>[] args,String namespace,String version,String transport);
	
	Set<ServiceItem> getServices(String serviceName,String method,Object[] args,String namespace,String version,String transport);
	
	boolean isExist(String serviceName,String namespace,String version);
	
	Set<ServiceItem> getServices(String serviceName,String namespace,String version);
	
	//get by service name, use for collection inject
	Set<ServiceItem> getServices(String serviceName);
	
	ServiceItem getServiceByImpl(String impl);
	
	void addServiceListener(String key,IServiceListener lis);
	
	void removeServiceListener(String key,IServiceListener lis);
	
	void addServiceNameListener(String key,IServiceListener lis);
	
	void removeServiceNameListener(String key,IServiceListener lis);
	
	void init();
	
	void setDataOperator(IDataOperator dataOperator);
}
