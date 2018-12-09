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
package org.jmicro.common;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:08:51
 */
public interface Constants {

	public static final String CLIENT_ONLY="client";
	public static final String SERVER_ONLY="server";
	
	public static final String DATA_OPERATOR = "dataOperator";
	
	public static final String DEFAULT_DATA_OPERATOR = "ZKDataOperator";
	
	public static final String CFG_ROOT="/jmicro";
	
	public static final String SIDE_COMSUMER = "comsumer";
	public static final String SIDE_PROVIDER="provider";
	public static final String SIDE_ANY="any";
	
	public static final String MONITOR_ENABLE_KEY = "monitorEnableKey";
	public static final String DEFAULT_MONITOR="defaultMonitor";
	public static final String FROM_MONITOR = "fromMonitor";
	
	public static final int CONN_CONNECTED=1;
	public static final int CONN_RECONNECTED=2;
	public static final int CONN_LOST=3;
	
	public static final String OBJ_FACTORY_KEY="objFactory";
	public static final String DEFAULT_OBJ_FACTORY="defaultObjFactory";
	
	public static final String REGISTRY_KEY="registry";
	public static final String DEFAULT_REGISTRY="defaultRegistry";
	
	public static final String DEFAULT_CODEC_FACTORY = "defaultCodecFactory";
	public static final String DEFAULT_SERVER = "defaultServer";
	public static final String DEFAULT_HANDLER = "defaultHandler";
	public static final String LAST_INTERCEPTOR = "lastInterceptor";
	public static final String FIRST_INTERCEPTOR = "firstInterceptor";
	
	public static final String DEFAULT_CLIENT_HANDLER = "defaultClientHandler";
	public static final String LAST_CLIENT_INTERCEPTOR = "lastClientInterceptor";
	public static final String FIRST_CLIENT_INTERCEPTOR = "firstClientInterceptor";
	public static final String PROXY = "proxy";
	
	public static final String DEFAULT_SELECTOR = "defaultSelector";
	public static final String DEFAULT_INVOCATION_HANDLER = "defaultInvocationHandler";
	
	public static final String DEFAULT_NAMESPACE = "defaultNamespace";
	public static final String VERSION = "0.0.1";
	public static final String DEFAULT_IDGENERATOR = "defaultGenerator";
	
	public static final String CHARSET = "UTF-8";
	public static final String SESSION_KEY = "_sessionKey";
	
	//public static final String CONFIG_ROOT = CFG_ROOT + "/config";
	//public static final String RAFT_CONFIG_ROOT_KEY = "configRoot";
	public static final String REGISTRY_URL_KEY = "registryUrl";
	public static final String BIND_IP = "bindIp";
	
	public static final String BASE_PACKAGES_KEY = "basePackages";
	public static final String INSTANCE_NAME = "instanceName";
	
	public static final String CONTEXT_CALLBACK_SERVICE = "ServiceCallback";
	public static final String CONTEXT_CALLBACK_CLIENT = "ClientCallback";
	
	public static final String SERVICE_ITEM_KEY = "serviceItemKey";
	public static final String SERVICE_METHOD_KEY = "serviceMethodKey";
	public static final String SERVICE_OBJ_KEY = "serviceObjKey";
	public static final String CLIENT_REF_METHOD = "reflectMethod";
	
	public static final String TRANSPORT_JDKHTTP = "jdkhttp";
	public static final String TRANSPORT_MINA = "mina";
	public static final String TRANSPORT_NETTY = "netty";
	public static final String TRANSPORT_NETTY_HTTP = "nettyhttp";
	
    public static final int TYPE_HTTP = 1;
	public static final int TYPE_SOCKET = 2;
	public static final int TYPE_WEBSOCKET = 3;
	
	public static final String HTTP_HEADER_ENCODER = "DataEncoderType";
	public static final String START_HTTP = "startHttp";
	
	public static final int DEFAULT_RESP_BUFFER_SIZE = 1024*4;
	
	
	/*=====================Message Begin=======================*/
	
	public static final byte MSG_TYPE_REQ_JRPC = 0x01; //普通RPC调用请求，发送端发IRequest，返回端返回IResponse
	public static final byte MSG_TYPE_RRESP_JRPC = 0x02;//返回端返回IResponse
	
	public static final byte MSG_TYPE_REQ_RAW = 0x03; //纯二进制数据请求
	public static final byte MSG_TYPE_RRESP_RAW = 0x04;//纯二进制数据响应
	
	public static final byte MSG_TYPE_ASYNC_REQ = 0x05; //异步请求，不需求等待响应返回
	public static final byte MSG_TYPE_ASYNC_RESP = 0x06; //异步响应，通过回调用返回
	
	public static final byte MSG_TYPE_API_CLASS_REQ = (byte)0x07; //API网关请求
	public static final byte MSG_TYPE_API_CLASS_RESP = (byte)0x08;//API网关请求响应
	
	public static final byte MSG_TYPE_API_REQ = (byte)0x09; //API网关请求
	public static final byte MSG_TYPE_API_RESP = (byte)0x0A;//API网关请求响应
	
	public static final byte MSG_TYPE_ID_REQ = (byte)0x0B; //Id请求
	public static final byte MSG_TYPE_ID_RESP = (byte)0x0C;//Id请求响应
	
	public static final byte MSG_TYPE_HEARBEAT_REQ = (byte)0x0D; //心跳请求
	public static final byte MSG_TYPE_HEARBEAT_RESP = (byte)0x0E;//心跳响应
	
	public static final byte MSG_TYPE_SYSTEM_REQ_JRPC = 0x0F; // 特殊RPC 接口，如ID RPC接口中
	public static final byte MSG_TYPE_SPECAIL_RRESP_JRPC = 0x10;//16
	
	public static final byte MSG_VERSION = (byte)1;
	
	//需要响应的请求
	public static final byte FLAG_NEED_RESPONSE = 1<<0;
	
	//异步消息
	public static final byte FLAG_STREAM = 1<<1;
	
	//可监控消息
	public static final byte FLAG_MONITORABLE = 1<<2;
	
	//是否启用服务级log
	public static final short FLAG_LOGGABLE = 0x80;
	
	/*=====================Message END=======================*/
	
}
