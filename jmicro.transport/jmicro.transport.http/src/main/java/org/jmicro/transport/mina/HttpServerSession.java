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
package org.jmicro.transport.mina;

import java.io.IOException;

import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.server.IServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

/** 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:14:01
 */
public class HttpServerSession extends AbstractHttpSession implements IServerSession{

	static final Logger LOG = LoggerFactory.getLogger(HttpServerSession.class);
	
	public HttpServerSession(HttpExchange exchange,int readBufferSize,int hearbeatInterval) {
		super(exchange,readBufferSize,hearbeatInterval);
	}

	@SuppressWarnings("restriction")
	@Override
	public void write(Message msg) {
		try {
			Headers responseHeaders = exchange.getResponseHeaders();
	        responseHeaders.set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, 0);
			String json = JsonUtils.getIns().toJson(msg);
			exchange.getResponseBody().write(json.getBytes(Constants.CHARSET));
			this.close(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void close(boolean flag) {
		super.close(flag);
	}
}
