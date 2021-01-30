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
package cn.jmicro.api.choreography;

import cn.jmicro.api.config.Config;

public interface ChoyConstants {

	public static final String ROOT_ACTIVE_AGENT = Config.ChoreographyDir + "/activeAgents";
	
	public static final String ROOT_AGENT = Config.ChoreographyDir + "/agents";
	
	public static final String DEP_DIR = Config.ChoreographyDir + "/deployments";
	
	public static final String INS_ROOT = Config.ChoreographyDir + "/instances";
	
	//controller存活标志
	public static final String ROOT_CONTROLLER = Config.ChoreographyDir + "/controllers";
	
	public static final String ID_PATH = Config.ChoreographyDir + "/" + ProcessInfo.class.getName();
	
	public static final String PROCESS_INFO_FILE = "processInfoFile";
	
	public static final String ARG_INSTANCE_ID = "instanceId";
	
	public static final String ARG_MYPARENT_ID = "myParentId";
	
	public static final String ARG_DEP_ID = "depId";
	
	public static final String RES_ID = "resId";
	
	public static final String RES_OWNER_ID = "resOwnerId";
	
	public static final String ARG_AGENT_ID = "agentId";
	
	public static final String ARG_AGENT_PRIVATE = "private";
	
	public static final String ARG_INIT_DEP_IDS = "initDepIds";
	
	public static final String AGENT_CMD_NOP = "0";
	
	public static final String AGENT_CMD_STARTING_TIMEOUT = "1";
	
	public static final String AGENT_CMD_STOPING_TIMEOUT = "2";
	
	public static final String AGENT_CMD_CLEAR_LOCAL_RESOURCE = "3";
	
	public static final String AGENT_CMD_STOP_ALL_INSTANCE = "4";
	
	//public static final String AGENT_CMD_NOP = "3";
}
