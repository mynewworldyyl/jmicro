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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:15:02
 */
@Component(lazy=false)
public class NettyHttpChannelInitializer extends ChannelInitializer<SocketChannel> {

	private static final Logger LOG = LoggerFactory.getLogger(NettyHttpChannelInitializer.class);
	
	// @Inject
	// private NettyTextWebSocketHandler txtWsHandler;

	@Inject
	private NettyBinaryWebSocketHandler binWsHandler;

	@Inject
	private NettyHttpServerHandler httpHandler;

	//@Cfg(value="/textWebsocketContextPath",defGlobal=true)
	//private String textWebsocketContextPath = "/_txt_";

	//@Cfg(value = "/binaryWebsocketContextPath", defGlobal = true)
	private String binaryWebsocketContextPath = "/" + Constants.HTTP_binContext;

	@Cfg(value = "/jksPwd")
	private String jksPwd = "";
	
	@Cfg(value = "/keyPwd")
	private String keyPwd = null;

	@Cfg(value = "/jksFile")
	private String jksFile = "";

	@Cfg(value = "/httpsEnable")
	private boolean httpsEnable = false;
	
	@Cfg(value="/nettyHttpPort",required=false,defGlobal=false)
	private int port=0;

	// private SSLEngine sslEngine = null;

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		if(this.httpsEnable && ch.localAddress().getPort() == port) {
			/*
			 * if(sslEngine == null) { sslEngine = getSslContext().createSSLEngine();
			 * sslEngine.setUseClientMode(false); sslEngine.setNeedClientAuth(false); }
			 */
			pipeline.addFirst("ssl", getSSLHandler());
		}

		// HttpServerCodec: 针对http协议进行编解码
		pipeline.addLast("httpServerCodec", new HttpServerCodec());
		// ChunkedWriteHandler分块写处理，文件过大会将内存撑爆
		pipeline.addLast("chunkedWriteHandler", new ChunkedWriteHandler());
		// 将一个Http的消息组装成一个完成的HttpRequest或者HttpResponse
		pipeline.addLast("httpObjectAggregator", new HttpObjectAggregator(8192));

		//pipeline.addLast(new HttpRequestHandler("/ws"));
		
		pipeline.addLast("binWebSocketServerProtocolHandler",
				new WebSocketServerProtocolHandler(binaryWebsocketContextPath, null, false, 65535));
		
		pipeline.addLast("binWebSocketWsHandler", binWsHandler);

		/*
		 * pipeline.addLast("webSocketServerProtocolHandler", new
		 * WebSocketServerProtocolHandler(textWebsocketContextPath));
		 * pipeline.addLast("textWebSocketHandler", txtWsHandler);
		 */

		pipeline.addLast("jmicroHttpHandler", httpHandler);

	}

	private SSLContext getSslContext() throws Exception {
		if (!this.httpsEnable) {
			return null;
		}
		char[] passArray = jksPwd.toCharArray();
		SSLContext sslContext = SSLContext.getInstance("TLSv1");
		KeyStore ks = KeyStore.getInstance("JKS");
		FileInputStream inputStream = new FileInputStream(this.jksFile);
		ks.load(inputStream, passArray);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, passArray);
		sslContext.init(kmf.getKeyManagers(), null, null);
		inputStream.close();
		return sslContext;

	}

	private static final String PROTOCOL = "TLSv1.2";
	private static final String ALGORITHM_SUN_X509 = "SunX509";
	private static final String ALGORITHM = "ssl.KeyManagerFactory.algorithm";
	// private static final String KEYSTORE= "ssl_certs/mysslstore.jks";
	private static final String KEYSTORE_TYPE = "JKS";
	// private static final String KEYSTORE_PASSWORD= "123456";
	// private static final String CERT_PASSWORD="123456";
	private static SSLContext serverSSLContext = null;

	public SslHandler getSSLHandler() {
		SSLEngine sslEngine = null;
		if (serverSSLContext == null) {
			throw new CommonException("serverSSLContext not init");
		} else {
			sslEngine = serverSSLContext.createSSLEngine();
			sslEngine.setUseClientMode(false);
			sslEngine.setNeedClientAuth(false);

		}
		return new SslHandler(sslEngine);
	}

	public void jready() {

		if (!this.httpsEnable) {
			return;
		}
		
		String algorithm = Security.getProperty(ALGORITHM);
		if (algorithm == null) {
			algorithm = ALGORITHM_SUN_X509;
		}
		KeyStore ks = null;
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(this.jksFile);
			// inputStream = new
			// FileInputStream(SSLHandlerProvider.class.getClassLoader().getResource(KEYSTORE).getFile());
			ks = KeyStore.getInstance(KEYSTORE_TYPE);
			ks.load(inputStream, this.jksPwd.toCharArray());
			
		} catch (IOException e) {
			LOG.error("Cannot load the keystore file", e);
		} catch (CertificateException e) {
			LOG.error("Cannot get the certificate", e);
		} catch (NoSuchAlgorithmException e) {
			LOG.error("Somthing wrong with the SSL algorithm", e);
		} catch (KeyStoreException e) {
			LOG.error("Cannot initialize keystore", e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				LOG.error("Cannot close keystore file stream ", e);
			}
		}
		try {

			// Set up key manager factory to use our key store
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
			if(Utils.isEmpty(this.keyPwd)) {
				kmf.init(ks,null);
			}else {
				kmf.init(ks, this.keyPwd.toCharArray());
			}
			
			KeyManager[] keyManagers = kmf.getKeyManagers();
			// Setting trust store null since we don't need a CA certificate or Mutual
			// Authentication
			TrustManager[] trustManagers = null;

			serverSSLContext = SSLContext.getInstance(PROTOCOL);
			serverSSLContext.init(keyManagers, trustManagers, null);

		} catch (Exception e) {
			LOG.error("Failed to initialize the server-side SSLContext", e);
		}

	}

}
