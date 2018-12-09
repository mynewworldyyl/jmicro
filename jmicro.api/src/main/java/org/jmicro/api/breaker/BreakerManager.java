package org.jmicro.api.breaker;

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
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.service.ServiceManager;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:49:55
 */
@Component
public class BreakerManager {
	
	@Inject
	private ServiceManager srvManager;
	
	public void init(){
		
	}

	public void breakService(String key, ServiceMethod sm) {
		srvManager.breakService(sm);
		if(sm.isBreaking()) {
			//服务熔断了，做自动服务检测
			
		} else {
			//关闭服务自动检测
			
		}
		
	}
}
