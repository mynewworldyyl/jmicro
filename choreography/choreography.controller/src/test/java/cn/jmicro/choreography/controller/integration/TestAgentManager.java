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
package cn.jmicro.choreography.controller.integration;

import org.junit.Assert;
import org.junit.Test;

import cn.jmicro.api.IListener;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.choreography.agent.AgentManager;
import cn.jmicro.choreography.base.AgentInfo;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.test.JMicroBaseTestCase;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:40:29
 */
public class TestAgentManager extends JMicroBaseTestCase{

	@Test
	public void testCreateAgent() {
		AgentInfo ai = new AgentInfo();
		ComponentIdServer idserver = of.get(ComponentIdServer.class);
		ai.setId(idserver.getStringId(AgentInfo.class));
		ai.setName("JmicroTestAgentmanager");
		ai.setStartTime(System.currentTimeMillis());
		
		String path = ChoyConstants.ROOT_AGENT + "/" + ai.getName() + ai.getId();
		String jsonData = JsonUtils.getIns().toJson(ai);
		
		IDataOperator dataOperator = of.get(IDataOperator.class);
		
		if(!dataOperator.exist(path)) {
			dataOperator.createNodeOrSetData(path, jsonData, true);
		}else {
			dataOperator.setData(path, jsonData);
		}
	}
	
	@Test
	public void testAddServiceListener() {
		AgentManager am = of.get(AgentManager.class);
		am.addAgentListener((type,ai)->{
			Assert.assertNotNull(ai);
			Assert.assertTrue(type == IListener.ADD);
			System.out.println(ai);
		});
		//this.waitForReady(30);
	}
}
