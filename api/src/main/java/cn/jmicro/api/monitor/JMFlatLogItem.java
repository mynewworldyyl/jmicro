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
package cn.jmicro.api.monitor;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.utils.TimeUtils;
import lombok.Data;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:47
 */
@SO
@IDStrategy(100)
@Data
public final class JMFlatLogItem{
	
	public static final String TABLE = "rpc_log";
	
	private long linkId;
	
	private long reqId=0;
	
	private long reqParentId = -1L;
	
	private int actClientId = -1;
	
	private int sysClientId = -1;
	
	private String actName;
	
	private IReq req = null;
	
	private IResp resp = null;
	
	private UniqueServiceMethodKey smKey = null;
	
	private byte logLevel;
	
	private String implCls;
	
	private String localHost = null;
	private String localPort = null;
	
	private String remoteHost = null;
	private String remotePort = null;
	
	private String instanceName = null;
	
	private boolean provider;
	
	private long createTime = TimeUtils.getCurTime();
	private long inputTime;
	private long costTime;
	
	private String tag = null;
	
	private String configId = null;
	
	private OneLog items;
	
}
