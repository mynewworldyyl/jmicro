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
package cn.jmicro.transport.http;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:17:01
 */
@SuppressWarnings("restriction")
@Component
public class StaticResourceHttpHandler implements HttpHandler {

	static final Logger LOG = LoggerFactory.getLogger(StaticResourceHttpHandler.class);
	
	@Cfg("/StaticResourceHttpHandler/root")
	private String root;
	
	@Cfg("/StaticResourceHttpHandler/indexPage")
	private String indexPage;
	
	private Map<String,String> contents = new HashMap<>();
	
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		exchange.sendResponseHeaders(200, 0);
		if(path.equals("/")){
			response(this.root+indexPage,exchange.getResponseBody());
		}else {
			response(this.root+path,exchange.getResponseBody());
		}
		exchange.close();
	}

	private void response(String path, OutputStream responseBody) {
		String content = getContent(path);
		if(content == null){
			content = getContent(root+"404.html");
		}
		try {
			responseBody.write(content.getBytes(Constants.CHARSET));
		} catch (IOException e) {
			LOG.error("getContent",e);
		}
	}

	private String getContent(String path) {
		if(contents.containsKey(path)){
			return contents.get(path);
		}
		BufferedReader bisr = null;
		try {
			StringBuffer sb = new StringBuffer();
			bisr = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			String line = null;
			while((line = bisr.readLine()) != null){
				sb.append(line);
			}
			String content = sb.toString();
			contents.put(path, content);
			return content;
		} catch (IOException e) {
			LOG.error("getContent",e);
		}finally{
			if(bisr != null){
				try {
					bisr.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

}
