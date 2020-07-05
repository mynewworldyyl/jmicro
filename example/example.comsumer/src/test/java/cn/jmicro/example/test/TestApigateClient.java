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
package cn.jmicro.example.test;

import org.junit.Before;
import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.ITestRpcService;
import cn.jmicro.example.api.rpc.ISimpleRpc;
import cn.jmicro.gateway.client.ApiGatewayClient;
import cn.jmicro.gateway.client.ApiGatewayConfig;

public class TestApigateClient {

	private ApiGatewayClient client = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_SOCKET));
	
	@Before
	public void setUp() {
		
		client.getConfig().setDebug(true);
		
		client.getConfig().setClientType(Constants.TYPE_SOCKET);
		client.getConfig().setPort(62688);
		
		/*client.getConfig().setPort(9090);
		client.getConfig().setClientType(Constants.TYPE_HTTP);*/
	}
	
	@Test
	public void testGetService() {
		ISimpleRpc srv = client.getService(ISimpleRpc.class,
				"simpleRpc", "0.0.1");
		System.out.println(srv.hello("Hello api gateway"));
	}
	
	@Test
	public void testCallService() {
		String[] args = new String[] {"hello"};
		String result =(String) client.callService(ISimpleRpc.class.getName(),
		"simpleRpc", "0.0.1","hello",args);
		System.out.println(result);
	}
	
	@Test
	public void testCallTestRpcService() {
		String[] args = new String[] {"hello"};
		String result =(String) client.callService(ITestRpcService.class.getName(),
		"testrpc", "0.0.1","subscrite",args, (msg,f)->{
			System.out.println("Got server msg:"+msg);
		});
		System.out.println(result);
		JMicro.waitForShutdown();
	}
	
}
