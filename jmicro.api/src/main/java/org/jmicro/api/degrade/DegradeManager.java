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
package org.jmicro.api.degrade;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;

/**
 * @author Yulei Ye
 * @date 2018年10月16日-下午5:54:05
 */
@Component(lazy=true)
public class DegradeManager {

	public static final String MONITOR = Constants.CFG_ROOT + "/monitor/";
	
	public static final String AVG_TIME_ROOT = MONITOR + "avgtime/";
	
	public static final String TIMEOUT_ROOT = MONITOR + "timeout/";
	
	public static final String EXCEPTION_ROOT = MONITOR + "exception/";
	
	@Inject
	private IDataOperator dataOperator;
	
	public void updateAvgResponseTime(String serviceMethodId,String data){
		createOrSetData(AVG_TIME_ROOT + serviceMethodId,data);
	}
	
	public void updateTimeoutCnt(String serviceMethodId,Integer cnt){
		createOrSetData(TIMEOUT_ROOT + serviceMethodId,cnt.toString());
	}
	
	public void updateExceptionCnt(String serviceMethodId,Integer cnt){
		createOrSetData(EXCEPTION_ROOT + serviceMethodId,cnt.toString());
	}
	
	private void createOrSetData(String path, String data) {
		if(dataOperator.exist(path)) {
			dataOperator.setData(path, data);
		}else {
			dataOperator.createNode(path, data, false);
		}
	}
	
}
