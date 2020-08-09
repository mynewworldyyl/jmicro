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

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.test.Person;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.ITestRpcService;
import cn.jmicro.example.api.rpc.ISimpleRpc;
import cn.jmicro.example.api.rpc.genclient.ISimpleRpc$JMAsyncClient;
import cn.jmicro.gateway.client.ApiGatewayClient;
import cn.jmicro.gateway.client.ApiGatewayConfig;

public class TestApigateClient {

	//private ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_SOCKET,"192.168.56.1",9091));
	
	//private ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_HTTP,"192.168.56.1",9090));
	
	//private ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_SOCKET,"124.70.152.7",80));
	
	//private ApiGatewayClient wsClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_WEBSOCKET,"192.168.56.1",9090));
	
	public static void main(String[] args) {
		ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_HTTP,"124.70.152.7",80));
		//ISimpleRpc srv = socketClient.getService(ISimpleRpc.class,"simpleRpc", "0.0.1");
		//System.out.println(srv.hi(new Person()));
		
		ISimpleRpc$JMAsyncClient srv = socketClient.getService(ISimpleRpc$JMAsyncClient.class,"simpleRpc", "0.0.1");
		srv.hiJMAsync(new Person()).then((val,fail) -> {
			System.out.println("Hi: " +val);
			//System.out.println(fail);
		});
		
		srv.helloJMAsync("Hello jmicro: ").then((val,fail) -> {
			System.out.println("Hello: " +val);
			//System.out.println(fail);
		});
		
		JMicro.waitForShutdown();
	}
	
	private ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_HTTP,"124.70.152.7",80));
	
	@Test
	public void testHiPerson() {
		ISimpleRpc srv = socketClient.getService(ISimpleRpc.class,"simpleRpc", "0.0.1");
		System.out.println(srv.hi(new Person()));
	}
	
	@Test
	public void testGetService() {
		ISimpleRpc srv = socketClient.getService(ISimpleRpc.class,
				"simpleRpc", "0.0.1");
		System.out.println(srv.hello("Hello api gateway"));
	}
	
	@Test
	public void testCallService() {
		String[] args = new String[] {"hello"};
		String result = socketClient.callService(ISimpleRpc.class.getName(),
		"simpleRpc", "0.0.1", "hello", String.class, args,null);
		System.out.println(result);
	}
	
	@Test
	public void testCallTestRpcService() {
		Object[] args = new Object[] {"hello"};
		socketClient.callService(ITestRpcService.class.getName(),
		"testrpc", "0.0.1","subscrite", String.class, args, (msg,f)->{
			System.out.println("Got server msg:"+msg);
		});
		JMicro.waitForShutdown();
	}
	
}
