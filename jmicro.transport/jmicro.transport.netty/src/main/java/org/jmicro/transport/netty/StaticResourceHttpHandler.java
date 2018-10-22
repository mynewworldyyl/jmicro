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
package org.jmicro.transport.netty;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:16:29
 */
@SuppressWarnings("restriction")
@Component(value="nettyStaticResourceHandler",lazy=false)
public class StaticResourceHttpHandler  {

	static final Logger LOG = LoggerFactory.getLogger(StaticResourceHttpHandler.class);
	
	@Cfg("/StaticResourceHttpHandler/root")
	private String root;
	
	@Cfg("/StaticResourceHttpHandler/indexPage")
	private String indexPage;
	
	private Map<String,byte[]> contents = new HashMap<>();
	
	public boolean canhandle(FullHttpRequest request){
		return request.method().equals(HttpMethod.GET);
	}

	public void handle(ChannelHandlerContext ctx,FullHttpRequest request) throws IOException {
		String path = request.uri();
		
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set("content-Type",getContentType(path));
		
		byte[] content = null;
		if(path.equals("/")){
			content = this.getContent(this.root+indexPage);
		}else {
			content = this.getContent(this.root + path);
		}
		if(content == null) {
			content = this.getContent(this.root + "404.html");
		}
	
		response.headers().set("content-Length",content.length);
		
		ByteBuf responseBuf = Unpooled.copiedBuffer(content);
		response.content().writeBytes(responseBuf);
		responseBuf.release();
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private String getContentType(String path) {
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		if(path== null || "/".equals(path.trim())){
			return "text/html;charset=UTF-8";
		}else {
			String ct = fileNameMap.getContentTypeFor(path);
			if(ct == null) {
				ct = "text/html;charset=UTF-8";
			}
			return ct;
		}
	}
	   
	private byte[] getContent(String path) {
		if(contents.containsKey(path)){
			return contents.get(path);
		}
		InputStream bisr = null;
		try {
			bisr = new FileInputStream(path);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] line = new byte[1024];
			int len = -1;
			while((len = bisr.read(line)) > 0 ){
				bos.write(line, 0, len);
			}
			contents.put(path, bos.toByteArray());
			return bos.toByteArray();
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
