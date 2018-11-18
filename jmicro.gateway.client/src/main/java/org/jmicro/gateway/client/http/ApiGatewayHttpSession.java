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

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.net.AbstractSession;
import org.jmicro.api.net.Message;
import org.jmicro.client.ClientMessageReceiver;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:22:37
 *
 */
public class ApiGatewayHttpSession extends AbstractSession implements IClientSession {

	private String url;
	private ClientMessageReceiver receiver;
	
	public ApiGatewayHttpSession(ClientMessageReceiver receiver,String url,int readBufferSize,int heardbeatInterval) {
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
			byte[] data = doPostRequest(bb.array());
			Message message = new Message();
            message.decode(ByteBuffer.wrap(data));
            receiver.receive(ApiGatewayHttpSession.this,message);
		}).start();
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
	}

	private byte[] doPostRequest(byte[] data) {
        HttpPost httpost = new HttpPost(url);
        byte[] result = new byte[0];
        try {
            HttpClient httpclient = HttpConnectionManager.getHttpClient();
            httpost.setEntity(new ByteArrayEntity(data,0,data.length));
            httpost.setHeader("Connection", "close");
            httpost.setHeader(Constants.HTTP_HEADER_ENCODER, Message.PROTOCOL_BIN+"");
            
            HttpResponse response = null;

            try {
                response = httpclient.execute(httpost);
            } catch (Exception e) {
                httpclient.getConnectionManager().closeExpiredConnections();
                httpclient.getConnectionManager().closeIdleConnections(0,TimeUnit.SECONDS);
                response = httpclient.execute(httpost);
            }

            HttpEntity entity = response.getEntity();
            if (entity != null) {
            	int len = (int)entity.getContentLength();
            	if(len > 0) {
            		result = new byte[len];
            		InputStream is = entity.getContent();
                	is.read(result, 0, len);
            	}
                entity.consumeContent();
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return result;
    }

}
