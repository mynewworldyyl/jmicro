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
package jmicro.gateway.client.test;

import org.jmicro.api.test.ISayHello;
import org.jmicro.gateway.ApiGatewayClient;
import org.junit.Test;

public class TestApigateClient {

	public void testGetService() {
		ITestApiGatewayService srv = ApiGatewayClient.getIns().getService(ITestApiGatewayService.class,
				"testapigw", "0.0.1");
		srv.hello("Hello api gateway");
	}
	
	@Test
	public void testCallService() {
		String[] args = new String[] {"hello"};
		String result =(String) ApiGatewayClient.getIns().callService(ISayHello.class.getName(),
		"testsayhello", "0.0.1","hello",args);
		System.out.println(result);
	}
}
