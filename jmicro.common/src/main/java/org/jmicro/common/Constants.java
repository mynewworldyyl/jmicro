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
	
	public static final String CFG_ROOT="/jmicro";
	
	public static final String LOCAL_DATA_DIR = "localDataDir";
	
	//在CFG_ROOT下目录名称前缀为JMICRO系统保留，应用不可使用
	public static final String DEFAULT_PREFIX="JMICRO";
	
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
	
	public static final String DATA_OPERATOR = "dataOperator";
	public static final String DEFAULT_DATA_OPERATOR = "ZKDataOperator";
	
	public static final String REGISTRY_KEY="registry";
	public static final String DEFAULT_REGISTRY="defaultRegistry";
	
	public static final String PUBSUB_KEY="pubsub";
	public static final String DEFAULT_PUBSUB="org.jmicro.pubsub.PubSubServer";
	
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
	public static final String SPECIAL_INVOCATION_HANDLER = "specailInvocationHandler";
	
	public static final String DEFAULT_NAMESPACE = "defaultNamespace";
	public static final String VERSION = "0.0.1";
	public static final String DEFAULT_IDGENERATOR = "defaultGenerator";
	
	public static final String CHARSET = "UTF-8";
	public static final String IO_SESSION_KEY = "_iosessionKey";
	
	//public static final String CONFIG_ROOT = CFG_ROOT + "/config";
	//public static final String RAFT_CONFIG_ROOT_KEY = "configRoot";
	public static final String REGISTRY_URL_KEY = "registryUrl";
	public static final String BIND_IP = "bindIp";
	public static final String ExportSocketIP = "exportSocketIP";
	public static final String ExportHttpIP = "exportHttpIP";
	
	
	public static final String BASE_PACKAGES_KEY = "basePackages";
	public static final String INSTANCE_NAME = "instanceName";
	public static final String INSTANCE_NAME_GEN_CLASS = "instanceNameGenClass";
	
	public static final String CONTEXT_CALLBACK_SERVICE = "ServiceCallback";
	public static final String CONTEXT_CALLBACK_CLIENT = "ClientCallback";
	
	public static final String SERVICE_ITEM_KEY = "serviceItemKey";
	
	public static final String SERVICE_NAME_KEY = "nameKey";
	
	public static final String SERVICE_NAMESPACE_KEY = "namespaceKey";
	
	public static final String SERVICE_VERSION_KEY = "versionKey";
	
	//public static final String SERVICE_SPECIFY_ITEM_KEY = "specifyServiceItemKey";
	
	public static final String SERVICE_METHOD_KEY = "serviceMethodKey";
	public static final String NEW_LINKID = "newLinkId";
	public static final String SERVICE_OBJ_KEY = "serviceObjKey";
	public static final String CLIENT_REF_METHOD = "reflectMethod";
	
	public static final String TRANSPORT_JDKHTTP = "jdkhttp";
	public static final String TRANSPORT_MINA = "mina";
	public static final String TRANSPORT_NETTY = "netty";
	public static final String TRANSPORT_NETTY_HTTP = "nettyhttp";
	
	public static final String BREAKER_TEST_CONTEXT = "breakerTestContext";
	
	public static final String REF_ANNO = "referenceAnno";
	
	public static final String DIRECT_SERVICE_ITEM = "directServiceItem";
	
	public static final String ROUTER_KEY = "routerKey";
	
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
	
	/*=====================Message END=======================*/
	
	//time unit constant
	public static final String TIME_DAY = "D"; //天
	public static final String TIME_HOUR = "H"; //时
	public static final String TIME_MINUTES = "M"; //分
	public static final String TIME_SECONDS = "S"; //秒
	public static final String TIME_MILLISECONDS = "MS"; //毫秒
	public static final String TIME_MICROSECONDS = "MC"; //微秒
	public static final String TIME_NANOSECONDS = "N"; //纳秒
	
	
	/*==============================================================*/

	//字段类型，0：表示编码，1：表示类名
	public static final byte TYPE_VAL = 0X40;
	
	//列表大小，0：表示等于0，1：大于0
	public static final byte SIZE_NOT_ZERO = 0X20;
	
	//前置元素类型1：是，0：否
	public static final byte HEADER_ELETMENT = 0X10;
	
	//能从泛型中能获取到足够的列表元素类型信息 0:不能从泛型中能获取到足够的列表元素类型信息，1：可以从泛型中获取到足够的列表元素类型信息
	public static final byte GENERICTYPEFINAL = 0X08;
	
	//段类型，0：表示编码，1：表示类名
	public static final byte ELEMENT_TYPE_CODE = 0X04;
	
	//字段类型是否是空值 0：表示空值，1：表示非空值
	public static final byte EXT0 = (byte)0X02;
	
	//字段类型是否是空值 0：表示空值，1：表示非空值
	public static final byte EXT1 = (byte)0X80;
	
	//字段类型是否是空值 0：表示空值，1：表示非空值
	public static final byte NULL_VAL = (byte)0X01;
	
	/*==============================================================*/
}
