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
import cn.jmicro.api.pubsub.PSData;
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
		ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_HTTP,"192.168.56.1",9090));
		//ISimpleRpc srv = socketClient.getService(ISimpleRpc.class,"simpleRpc", "0.0.1");
		//System.out.println(srv.hi(new Person()));
		
		ISimpleRpc$JMAsyncClient srv = socketClient.getService(ISimpleRpc$JMAsyncClient.class,"simpleRpc", "0.0.1");
		srv.hiJMAsync(new Person()).then((val,fail,ctx) -> {
			System.out.println("Hi: " +val);
			//System.out.println(fail);
		});
		
		srv.helloJMAsync("Hello jmicro: ").then((val,fail,ctx) -> {
			System.out.println("Hello: " +val);
			//System.out.println(fail);
		});
		
		JMicro.waitForShutdown();
	}
	
	private ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_SOCKET,"192.168.56.1",9092));
	
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
		"testrpc", "0.0.1","subscrite", String.class, args, (msg,f,ctx)->{
			System.out.println("Got server msg:"+msg);
		});
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testLoginLogout() {
		socketClient.loginJMAsync("test01", "1")
		.success((ai,cxt)->{
			System.out.println("Success login"+ai.getActName());
			socketClient.logoutJMAsync()
			.then((succ,fail0,cxt0)->{
				if(fail0 == null) {
					System.out.println("Success logout: "+succ);
				}else {
					System.out.println("Fail logout: "+fail0.toString());
				}
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishString() {
		socketClient.loginJMAsync("test01", "1")
		.success((ai,cxt)->{
			System.out.println("Success login: "+ai.getActName());
			 socketClient.getPubsubClient()
			.publishStringJMAsync("/jmicro/test/topic01", "Message from java client!",PSData.FLAG_DEFALUT,null)
			.success((id,cxt0)->{
				System.out.println("Publish result: "+id);
			})
			.fail((code,msg,cxt1)->{
				System.out.println("Fail pubilish content: code: "+ code + ", msg: " + msg);
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishByte() {
		socketClient.loginJMAsync("test01", "1")
		.success((ai,cxt)->{
			System.out.println("Success login: "+ai.getActName());
			 socketClient.getPubsubClient()
			.publishBytesJMAsync("/jmicro/test/topic01", "Message from java client!".getBytes(),PSData.FLAG_DEFALUT,null)
			.success((id,cxt0)->{
				System.out.println("Publish result: "+id);
			})
			.fail((code,msg,cxt1)->{
				System.out.println("Fail pubilish content: code: "+ code + ", msg: " + msg);
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishMutilItems() {
		socketClient.loginJMAsync("test01", "1")
		.success((ai,cxt)->{
			System.out.println("Success login: "+ai.getActName());
			
			 PSData pd = new PSData();
			 pd.setTopic("/jmicro/test/topic01");
			 pd.setData("Message from java client!");
			 
			 socketClient.getPubsubClient()
			//.publishOneItemJMAsync(null, pd)
			.publishMutilItemsJMAsync(new PSData[] {pd})
			.success((id,cxt0)->{
				System.out.println("Publish result: "+id);
			})
			.fail((code,msg,cxt1)->{
				System.out.println("Fail pubilish content: code: "+ code + ", msg: " + msg);
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		JMicro.waitForShutdown();
	}
	
	
}
