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
package org.jmicro.choreography.agent.test.integration;

import org.jmicro.api.JMicro;
import org.jmicro.api.choreography.controller.AgentManager;
import org.jmicro.api.choreography.controller.IAgentListener;
import org.jmicro.api.choreography.controller.InstanceManager;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:40:29
 */
public class TestInstanceManager extends JMicroBaseTestCase{

	@BeforeClass //
	public static void setupTestClass() {
		of = JMicro.getObjectFactoryAndStart(getArgs());
		of.start();
		registry = of.get(IRegistry.class);
	}
	
	protected static String[] getArgs() {
		return new String[] {"-DinstanceName=serviceAgentImpl "};
	}
	
	@Test
	public void testCreateAgent() {
		InstanceManager im = of.get(InstanceManager.class);
		Assert.assertNotNull(im.getAgent());
	}
	
	@Test
	public void testGetAllAgentInfo() {
		InstanceManager im = of.get(InstanceManager.class);
		Assert.assertNotNull(im.getAllAgentInfo());
		Assert.assertTrue(im.getAllAgentInfo().size() > 0);
	}
	
	@Test
	public void testAddServiceListener() {
		AgentManager am = of.get(AgentManager.class);
		am.addAgentListener((type,ai)->{
			Assert.assertNotNull(ai);
			Assert.assertTrue(type == IAgentListener.SERVICE_ADD);
			System.out.println(ai);
		});
		//this.waitForReady(30);
	}
}
