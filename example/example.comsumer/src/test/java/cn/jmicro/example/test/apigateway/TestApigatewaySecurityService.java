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
package cn.jmicro.example.test.apigateway;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import org.junit.BeforeClass;
import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.security.genclient.ISecretService$Gateway$JMAsyncClient;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.gateway.client.ApiGatewayClient;
import cn.jmicro.gateway.client.ApiGatewayConfig;

public class TestApigatewaySecurityService {
	
	private static ApiGatewayClient socketClient = null;
	
	@BeforeClass
	public static void setUp() {
		ApiGatewayConfig config = new ApiGatewayConfig(Constants.TYPE_SOCKET,"192.168.56.1",9092);
		config.setSslEnable(true);
		ApiGatewayClient.initClient(config);
		socketClient =  ApiGatewayClient.getClient();
	}
	
	@Test
	public void testGatewayCreateSecret() {
		socketClient.loginJMAsync("jmicro", "0")
		.success((resp,cxt)->{
			ISecretService$Gateway$JMAsyncClient secSrv = socketClient.getService(ISecretService$Gateway$JMAsyncClient.class, "sec","0.0.1");
			//Resp<JmicroPublicKey> rj = secSrv.createSecret("mng", "mng123");
			secSrv.createSecretJMAsync("mng", "mng123")
			.success((rj,cxt0)->{
				org.junit.Assert.assertNotNull(rj);
				org.junit.Assert.assertTrue(rj.getCode() == 0);
				org.junit.Assert.assertNotNull(rj.getData());
				System.out.println(JsonUtils.getIns().toJson(rj.getData()));
			})
			.fail((code,msg,cxt0)->{
				System.out.println("code=" + code + ",msg="+msg);
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testEncodeStr2Base64() throws UnsupportedEncodingException {
		byte[] arr = "abc".getBytes(Constants.CHARSET);
		System.out.println(Base64.getEncoder().encodeToString(arr));
	}
	
}
