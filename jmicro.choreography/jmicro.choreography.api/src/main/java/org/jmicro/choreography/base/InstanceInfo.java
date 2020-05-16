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
package org.jmicro.choreography.base;

import java.util.Arrays;

/**
 * 代表特定Agent启动起来的一个运行实例
 * 
 * @author Yulei Ye
 * @date 2019年1月24日 上午9:31:55
 */
public final class InstanceInfo {

	//此实例所属的Agent ID
	private String agentId;
	
	//此实例所属的Agent Name
	private String agentName;
	
	//时否存活
	private boolean isAlive;
	
	//此实例的ID
	private String processId;
	
	//运行此实例的类路径
	private String classpath;
	
	//放口类
	private String mainClazz;
	
	//启动参数
	private String[] args;

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getClasspath() {
		return classpath;
	}

	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}

	public String getMainClazz() {
		return mainClazz;
	}

	public void setMainClazz(String mainClazz) {
		this.mainClazz = mainClazz;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	@Override
	public String toString() {
		return "InstanceInfo [agentId=" + agentId + ", isAlive=" + isAlive + ", processId=" + processId + ", classpath="
				+ classpath + ", mainClazz=" + mainClazz + ", args=" + Arrays.toString(args) + "]";
	}
	
	
}
