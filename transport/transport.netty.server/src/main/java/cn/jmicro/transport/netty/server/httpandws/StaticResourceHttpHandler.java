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
package cn.jmicro.transport.netty.server.httpandws;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:16:29
 */
@Component(value="nettyStaticResourceHandler",lazy=false)
public class StaticResourceHttpHandler  {

	static final Logger LOG = LoggerFactory.getLogger(StaticResourceHttpHandler.class);
	
	//@Cfg("/StaticResourceHttpHandler/root")
	//private String root="*";
	
	@Cfg("/StaticResourceHttpHandler/debug")
	private boolean debug = false;
	
	@Cfg("/StaticResourceHttpHandler/indexPage")
	private String indexPage="index.html";
	
	@Cfg(value="/StaticResourceHttpHandler/staticResourceRoot_*", changeListener="resourceRootChange")
	private List<String> staticResourceRoots = new ArrayList<>();
	
	private Map<String,byte[]> contents = new HashMap<>();
	
	public boolean handle(ChannelHandlerContext ctx,FullHttpRequest request) throws IOException {
		//其他静态资源
		String path = request.uri();
		if(path.contains("?")) {
			path = path.substring(0,path.indexOf("?"));
		}
		
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		//response.headers().set("content-Type",getContentType(path));
		NettyHttpServerHandler.cors(response.headers());
		
		String path0 = URLDecoder.decode(path, Constants.CHARSET);
		byte[] content = this.getContent(path0,request,response);
		response.headers().set("Content-Length",content.length);
		ByteBuf responseBuf = Unpooled.copiedBuffer(content);
		response.content().writeBytes(responseBuf);
		responseBuf.release();
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		
		return true;
	}

	private String getContentType(String path) {
		FileNameMap fileNameMap = URLConnection.getFileNameMap();
		if(path== null || "/".equals(path.trim())){
			return "text/html;charset=UTF-8";
		} else {
			String ct = fileNameMap.getContentTypeFor(path);
			if(ct == null) {
				ct = "text/html;charset=UTF-8";
			}
			LOG.debug(path + " content type　: " + ct);
			return ct;
		}
	}
	   
	private byte[] getContent(String path, FullHttpRequest request, FullHttpResponse response) {
		String absPath = null;
		
		InputStream bisr = null;
		
		if(path.equals("/") || "".equals(path)){
			path = "/" + indexPage;
		}
		
		ClassLoader cl = StaticResourceHttpHandler.class.getClassLoader();
		for(String parent: staticResourceRoots) {
			String ph = parent + path;
			if(!debug) {
				if(contents.containsKey(ph+".gz")) {
					response.headers().set("Content-Encoding","gzip");
					return contents.get(ph+".gz");
				} else if(contents.containsKey(ph)) {
					return contents.get(ph);
				}
			}
			
			File file = new File(ph);
			File filegz = new File(ph+".gz");
			
			if(file.exists() && file.isFile()) {
				try {
					if(filegz.exists()) {
						absPath = ph+".gz";
						response.headers().set("Content-Encoding","gzip");
						bisr = new FileInputStream(filegz);
					} else {
						absPath = ph;
						bisr = new FileInputStream(absPath);
					}
					break;
				} catch (FileNotFoundException e) {
					LOG.error(absPath,e);
				}
			}

			bisr = cl.getResourceAsStream(ph);
			if(bisr != null) {
				//已经读取到资源，退出循环
				absPath = ph;
				break;
			}
		
		}
		
		if(absPath == null) {
			for(String parent: staticResourceRoots) {
				//没找到资源，返回404页面
				String ph = parent + "/404.html";
				if(!debug && contents.containsKey(ph)) {
					return contents.get(ph);
				}
				File file = new File(ph);
				if(file.exists()) {
					try {
						bisr = new FileInputStream(file);
						absPath = ph;
						break;
					} catch (FileNotFoundException e) {
						LOG.error(absPath,e);
					}
				}
				
				bisr = cl.getResourceAsStream(ph);
				if(bisr != null) {
					absPath = ph;
					break;
				}
			}
		}
		
		if(bisr == null) {
			LOG.error("Resource not found: "+path);
			try {
				return "404 page not found!".getBytes(Constants.CHARSET);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		try {
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] line = new byte[1024];
			int len = -1;
			while((len = bisr.read(line)) > 0 ){
				bos.write(line, 0, len);
			}
			contents.put(absPath, bos.toByteArray());
			return contents.get(absPath);
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
	
	public void resourceRootChange(String root) {
		if(!staticResourceRoots.contains(root)) {
			return;
		}
		
		Set<String> clears = new HashSet<String>();
		for(String p : contents.keySet()) {
			if(p.startsWith(root)) {
				clears.add(p);
			}
		}
		
		for(String p : clears) {
			contents.remove(p);
		}
	}

}
