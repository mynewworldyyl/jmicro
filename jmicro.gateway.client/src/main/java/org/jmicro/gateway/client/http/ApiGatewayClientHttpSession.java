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
package org.jmicro.gateway.client.http;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.client.IClientSession;
import org.jmicro.api.codec.JDataInput;
import org.jmicro.api.net.AbstractSession;
import org.jmicro.api.net.Message;
import org.jmicro.client.ClientMessageReceiver;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:37
 *
 */
public class ApiGatewayClientHttpSession extends AbstractSession implements IClientSession {

	private String url;
	private ClientMessageReceiver receiver;
	
	public ApiGatewayClientHttpSession(ClientMessageReceiver receiver,String url,int readBufferSize,int heardbeatInterval) {
		super(readBufferSize,heardbeatInterval);
		this.receiver = receiver;
		this.url = url;
	}
	
	public InetSocketAddress getLocalAddress(){
		return null;
	}
	
	public InetSocketAddress getRemoteAddress(){
		return null;
	}
	
	@Override
	public void write(Message msg) {
		ByteBuffer bb = msg.encode();
		new Thread(() -> {
			Map<String,String> headers = new HashMap<>();
			headers.put(Constants.HTTP_HEADER_ENCODER, Message.PROTOCOL_BIN+"");
			byte[] data = HttpClientUtil.doPostData(url, bb,headers);
			if(data.length > 0) {
				Message message = Message.decode(new JDataInput(ByteBuffer.wrap(data)));
	            receiver.receive(ApiGatewayClientHttpSession.this,message);
			} else {
				throw new CommonException("Req:"+msg.getReqId()+" response null data");
			}
		}).start();
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
	}

	@Override
	public boolean isServer() {
		return false;
	}

}
