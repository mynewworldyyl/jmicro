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
package org.jmicro.example.test;

import java.nio.ByteBuffer;

import org.jmicro.api.JMicro;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.IDecoder;
import org.jmicro.api.codec.IEncoder;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;

/**
 * 
 * @author Yulei Ye
 *
 * @date: 2018年11月10日 下午9:23:25
 */
public class TestCodec {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testEncodeDecode() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[]{"-DinstanceName=TestCodec"});
		ICodecFactory codeFactory = of.get(ICodecFactory.class);
		
		IEncoder encoder = codeFactory.getEncoder(Message.PROTOCOL_BIN);
		
		RpcResponse resp = new RpcResponse(1,new Integer[]{1,2,3});
		resp.setId(33l);
		resp.setMonitorEnable(true);
		resp.setSuccess(true);
		resp.getParams().put("key01", 3);
		resp.getParams().put("key02","hello");
		resp.setMsg(new Message());
		
		ByteBuffer bb = (ByteBuffer) encoder.encode(resp);
		
		IDecoder decoder = codeFactory.getDecoder(Message.PROTOCOL_BIN);
		resp = (RpcResponse)decoder.decode(bb, RpcResponse.class);
		System.out.println(resp);
	}
}
