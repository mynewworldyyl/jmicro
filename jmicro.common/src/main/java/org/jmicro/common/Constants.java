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

	public static final String SIDE_COMSUMER = "comsumer";
	public static final String SIDE_PROVIDER="provider";
	public static final String SIDE_ANY="";
	
	public static final String MONITOR_ENABLE_KEY = "monitorEnableKey";
	public static final String DEFAULT_MONITOR="defaultMonitor";
	
	public static final int CONN_CONNECTED=1;
	public static final int CONN_RECONNECTED=2;
	public static final int CONN_LOST=3;
	
	public static final String OBJ_FACTORY_KEY="objFactory";
	public static final String DEFAULT_OBJ_FACTORY="defaultObjFactory";
	
	public static final String REGISTRY_KEY="registry";
	public static final String DEFAULT_REGISTRY="defaultRegistry";
	
	public static final String DEFAULT_CODEC_FACTORY="defaultCodecFactory";
	public static final String DEFAULT_SERVER="defaultServer";
	public static final String DEFAULT_HANDLER="defaultHandler";
	public static final String LAST_INTERCEPTOR="lastInterceptor";
	public static final String FIRST_INTERCEPTOR="firstInterceptor";
	public static final String DEFAULT_SELECTOR="defaultSelector";
	public static final String DEFAULT_INVOCATION_HANDLER="defaultInvocationHandler";
	
	
	public static final String DEFAULT_NAMESPACE="defaultNamespace";
	public static final String DEFAULT_VERSION="0.0.0";
	public static final String DEFAULT_IDGENERATOR="defaultGenerator";
	
	public static final String CHARSET="UTF-8";
	public static final String SESSION_KEY="_sessionKey";
	
	public static final String CONFIG_ROOT="/jmicro/config";
	public static final String CONFIG_ROOT_KEY="configRoot";
	public static final String REGISTRY_URL_KEY="registryUrl";
	public static final String BASE_PACKAGES_KEY="basePackages";
	
	public static final String CONTEXT_CALLBACK = "Callback";
	
	public static final String SERVICE_ITEM_KEY="serviceItemKey";
	public static final String SERVICE_METHOD_KEY="serviceMethodKey";
	public static final String SERVICE_OBJ_KEY="serviceObjKey";
	
	
	/*=====================Message Begin=======================*/
	/*=====================Message Begin=======================*/
	/*=====================Message Begin=======================*/
	
	public static final int HEADER_LEN=34;
	
/*	public static final byte MSG_REQ_TYPE_RESP=1;
	
	public static final byte MSG_REQ_TYPE_REQ=2;
	
	public static final byte PROTOCOL_TYPE_BEGIN=1;
	public static final byte PROTOCOL_TYPE_END=2;
	
	public static final byte PROTOCOL_TYPE_REQ_ER=3;
	public static final byte PROTOCOL_TYPE_RESP_ER=4;*/
	
	//public static final short MSG_TYPE_ZERO = 0x0000;
	
	public static final short MSG_TYPE_REQ_JRPC = 0x0001; //普通RPC调用请求，发送端发IRequest，返回端返回IResponse
	public static final short MSG_TYPE_RRESP_JRPC = 0x0002;//返回端返回IResponse
	
	//public static final short MSG_TYPE_SERVER_ASYNC_MESSAGE = 0x0003; //异步消息请求，服务器处理
	//public static final short MSG_TYPE_RRESP_RAW = 0x0004;//纯二进制数据响应
	
	public static final short MSG_TYPE_REQ_RAW = 0x0004; //纯二进制数据请求
	public static final short MSG_TYPE_RRESP_RAW = 0x0005;//纯二进制数据响应
	
	public static final short MSG_TYPE_ASYNC_REQ = 0x0006; //异步请求，不需求等待响应返回
	public static final short MSG_TYPE_ASYNC_RESP = 0x0007; //异步响应，通过回调用返回
	
	//public static final short MSG_TYPE_SERVER_ERR = 0x7FFE;
	//public static final short MSG_TYPE_ALL = 0x7FFF;
	
	public static final short MSG_TYPE_HEARBEAT_REQ = 0x7FFC; //心跳请求
	public static final short MSG_TYPE_HEARBEAT_RESP = 0x7FFD;//心跳响应
	
	public static final byte[] VERSION = {0,0,1};
	public static final String VERSION_STR = "0.0.1";
	
	//public static final byte FLAG_ASYNC = 1<<0;
	
	public static final byte FLAG_NEED_RESPONSE = 1<<1;
	
	public static final byte FLAG_STREAM = 1<<2;
	
	/*=====================Message END=======================*/
	/*=====================Message END=======================*/
	/*=====================Message END=======================*/
	
}
