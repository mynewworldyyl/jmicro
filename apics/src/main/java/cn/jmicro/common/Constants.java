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
package cn.jmicro.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:08:51
 */
public final class Constants {
	
	public static final long DAY_IN_MILLIS = 24*60*60*1000;
	
	//系统固定的HTTP路径前缀，应用不能使用此前缀作它用
	//public static final String  HTTP_txtContext= "_txt_";
	public static final String  HTTP_binContext = "/_bin_";//专用于web Socket rpc
	public static final String  HTTP_httpContext= "/_http_";//专用于HTTP RPC
	public static final String  HTTP_fsContext= "/_fs_";//专用于文件下载
	public static final String  HTTP_statis= "/statis";//静态资源本地目录
	
	public static final String  HTTP_METHOD_POST= "POST";
	public static final String  HTTP_METHOD_GET= "GET";
	
	public static final String  HTTP_JSON_CONTENT_TYPE = "application/json;chartset=utf-8";
	public static final String  HTTP_ALL_CONTENT_TYPE = "*";
	
	//纯文本数据，对应于spring-web的restcontroller注解
	public static final byte  HTTP_RESP_TYPE_RESTFULL= 1;
	
	//返回HTML，后端返回文件视图路径，网关读取文件并与数据整合后返回给客户端
	public static final byte  HTTP_RESP_TYPE_VIEW= 2;
	
	//返回进制流，后端通过文件系统返回文件ID，在网关读取文件流返回给客户端
	public static final byte  HTTP_RESP_TYPE_STREAM= 3;
	
	//返回原始内容，如果返回值是RespJRso实例，则返回RespJRso.data，否则直接返回resp
	//例如三方支付异步通知需要返回"SUCCESS"字符串，
	public static final byte  HTTP_RESP_TYPE_ORIGIN= 4;
	
	public static final Set<String>  HTTP_PREFIX_LIST;
	static {
		HashSet<String> h = new HashSet<>();
		h.add(Constants.HTTP_binContext);
		h.add(Constants.HTTP_fsContext);
		h.add(Constants.HTTP_httpContext);
		h.add(Constants.HTTP_statis);
		//h.add(Constants.HTTP_txtContext);
		HTTP_PREFIX_LIST = Collections.unmodifiableSet(h);
	}
	
	public static final String BASE64_IMAGE_PREFIX = "data:image/jpeg;base64,";
	public static final String BASE64_IMAGE_SUBFIX = "==";
	
	public static final String JMICRO_READY_METHOD_NAME = "jready";
	
	//public static final String JMICRO_READY = "jmicroReady";
	
	public static final String CORE_CLASS="cn.jmicro.api";
	public static final String INVALID_LOG_DESC = "nl";
	public static final byte USE_SYSTEM_CLIENT_ID = -2;
	
	public static final String LOGIN_KEY = "-119";
	
	public static final int NO_CLIENT_ID = -1;
	
	public static final byte LIMIT_TYPE_LOCAL = 1;
	
	public static final byte LIMIT_TYPE_SS = 2;
	
	//服务方法客户端账号类型
	
	//服务方法只供登陆用户调用，基于登陆账号做权利限验证
	public static final byte FOR_TYPE_USER = 1;
	
	//服务方法只供运行实例用户调用，典型的如远程类加载方法
	public static final byte FOR_TYPE_SYS = 2;
	
	//全部可以调用
	public static final byte FOR_TYPE_ALL = 4;
	
	//物联网设备
	public static final byte FOR_TYPE_DEV = 8;
	
	public static final byte FOR_TYPE_DEV_USER = FOR_TYPE_USER | FOR_TYPE_DEV;
	
	//自由调用
	public static final byte LICENSE_TYPE_FREE = 0;
	
	//设定cliengtId可以调用
	public static final byte LICENSE_TYPE_CLIENT = 1;
	
	//只有自己能调用
	public static final byte LICENSE_TYPE_PRIVATE = 2;
	
	public static final String SYSTEM_PCK_NAME_PREFIXE = "cn.jmicro";
	
	public static final String[] SYSTEM_PCK_NAME_PREFIXES = new String[] {SYSTEM_PCK_NAME_PREFIXE,
			"sun.reflect","com.alibaba.fastjson"};
	
	public static final String PUBLIC_KEYS_FILES = "publicKeyFiles";
	
	public static final String PRIVATE_KEY_PWD = "priKeyPwd";
	
	public static final String JMICRO_VERSION = "0.0.2";
	
	public static final String JMICRO_RELEASE_LABEL = "SNAPSHOT";
	
	public static final String PATH_EXCAPE = "#@!";
	
	public static final String TOPIC_SEPERATOR=",";

	public static final String CLIENT_ID="clientId";
	
	public static final String CLIENT_NAME="actName";
	
	public static final String SYSTEM_LOG_LEVEL="sysLogLevel";
	
	//public static final String ADMIN_CLIENT_ID = "adminClientId";
	
	public static final String CLIENT_ONLY = "client";
	
	public static final String SERVER_ONLY = "server";
	
	public static final String TYPE_CODE_PRODUCER = "typeCodeProducer";
	
	public static final String CFG_ROOT="/jmicro";
	
	//主从模式标签
	public static final String MASTER_SLAVE_TAG = "masterSlaveTag";
	
	public static final String LOCAL_DATA_DIR = "localDataDir";
	public static final String INSTANCE_DATA_DIR = "instanceDataDir";
	
	//在CFG_ROOT下目录名称前缀为JMICRO系统保留，应用不可使用
	public static final String DEFAULT_PREFIX="JMICRO";
	
	public static final String SIDE_COMSUMER = "comsumer";
	public static final String SIDE_PROVIDER="provider";
	public static final String SIDE_ANY="any";
	
	//public static final String MONITOR_ENABLE_KEY = "monitorEnableKey";
	//public static final String DEFAULT_MONITOR="defaultMonitor";
	public static final String FROM_MONITOR = "fromMonitor";
	
	public static final String FROM_PUBSUB = "fromPubsub";
	public static final String FROM_MONITOR_CLIENT = "_fromMonitorManager";
	
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
	public static final String STATIS_KEY="statis";
	
	public static final String EXECUTOR_POOL="executorPool";
	
	public static final String EXECUTOR_GATEWAY_KEY = "/gatewayModel";
	public static final String EXECUTOR_RECEIVE_KEY = "/serverReceiver";
	
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
	//public static final String SPECIAL_INVOCATION_HANDLER = "specailInvocationHandler";
	
	public static final String VERSION = "0.0.5";
	public static final String DEFAULT_IDGENERATOR = "defaultGenerator";
	
	public static final String CHARSET = "UTF-8";
	public static final String IO_SESSION_KEY = "_iosessionKey";
	
	public static final String IO_BIN_SESSION_KEY = "_bin_iosessionKey";
	
	//public static final String CONFIG_ROOT = CFG_ROOT + "/config";
	//public static final String RAFT_CONFIG_ROOT_KEY = "configRoot";
	public static final String REGISTRY_URL_KEY = "registryUrl";
	//public static final String BIND_IP = "bindIp";
	public static final String ExportSocketIP = "exportSocketIP";
	public static final String ExportHttpIP = "exportHttpIP";
	
	public static final String ListenSocketIP = "listenSocketIP";
	public static final String ListenHttpIP = "listenHttpIP";
	
	public static final String RemoteIp = "remoteIp";
	public static final String LocalIp = "localIp";
	
	public static final String BASE_PACKAGES_KEY = "basePackages";
	public static final String JMICRO_COMPONENTS_KEY = "components";
	public static final String INSTANCE_PREFIX = "instanceName";
	public static final String INSTANCE_NAME_GEN_CLASS = "instanceNameGenClass";
	
	public static final String LOCAL_INSTANCE_NAME = "localInstanceName";
	
	//public static final String CONTEXT_CALLBACK_SERVICE = "ServiceCallback";
	//public static final String CONTEXT_CALLBACK_CLIENT = "ClientCallback";
	//public static final String CONTEXT_SERVICE_RESPONSE = "serviceResponse";
	
	public static final String SERVICE_ITEM_KEY = "serviceItemKey";
	
	public static final String ASYNC = "_async";
	
	public static final String SERVICE_NAME_KEY = "nameKey";
	
	public static final String SERVICE_NAMESPACE_KEY = "namespaceKey";
	
	public static final String SERVICE_VERSION_KEY = "versionKey";
	
	//public static final String SERVICE_SPECIFY_ITEM_KEY = "specifyServiceItemKey";
	
	public static final String SERVICE_METHOD_KEY = "serviceMethodKey";
	public static final String ASYNC_CONFIG = "asyncConfig";
	public static final String NEW_LINKID = "newLinkId";
	public static final String SERVICE_OBJ_KEY = "serviceObjKey";
	public static final String CLIENT_REF_METHOD = "reflectMethod";
	
	public static final String TRANSPORT_JDKHTTP = "jdkhttp";
	public static final String TRANSPORT_MINA = "mina";
	public static final String TRANSPORT_NETTY = "netty";
	public static final String TRANSPORT_NETTY_HTTP = "nettyhttp";
	public static final String TRANSPORT_NETTY_UDP = "nettyUdp";
	
	//熔断器自动检测目标服务是否可用尝试上下文，服务管理器根据此值判断是否可以返回被熔断服务信息
	//此值应该在整个RPC链路中透明传递
	public static final String BREAKER_TEST_CONTEXT = "breakerTestContext";
	
	//public static final String REF_ANNO = "referenceAnno";
	//调用服务实例前，指定要调用的服务实例，不做服务负载载均衡及服务路由,在RpcClientRequestHandler获取对应的ServiceItem后
	//应该删除此值
	public static final String DIRECT_SERVICE_ITEM = "directServiceItem";
	
	public static final String DIRECT_SERVICE_ITEM_CLIENT_ID = "directServiceItemClientId";
	
	public static final String DIRECT_SERVICE_ITEM_INS_NAME = "directServiceItemInsName";
	
	public static final String DIRECT_SERVICE_ITEM_INS_PREFIX = "directServiceItemInsPrefix";
	
	public static final String ROUTER_TYPE = "routerKey";
	
    public static final int TYPE_HTTP = 1;
	public static final int TYPE_SOCKET = 2;
	public static final int TYPE_WEBSOCKET = 3;
	public static final int TYPE_UDP = 4;
	
	public static final String HTTP_HEADER_ENCODER = "DataEncoderType";
	public static final String START_HTTP = "startHttp";
	
	public static final int DEFAULT_RESP_BUFFER_SIZE = 1024*4;
	
	
	/*=====================Message Begin=======================*/
	
	public static final byte MSG_TYPE_REQ_JRPC = 0x01; //普通RPC调用请求，发送端发IRequest，返回端返回IResponse
	public static final byte MSG_TYPE_RRESP_JRPC = 0x02;//返回端返回IResponse
	
	public static final byte MSG_TYPE_PUBSUB = 0x03; //订阅消息
	public static final byte MSG_TYPE_PUBSUB_RESP = 0x04;//订阅消息响应
	
	public static final byte MSG_TYPE_ASYNC_REQ = 0x05; //异步请求，不需要等待响应返回
	public static final byte MSG_TYPE_ASYNC_RESP = 0x06; //异步响应，通过回调用返回
	
	public static final byte MSG_TYPE_API_CLASS_REQ = (byte)0x07; //API网关请求
	public static final byte MSG_TYPE_API_CLASS_RESP = (byte)0x08;//API网关请求响应
	
	public static final byte MSG_TYPE_EXSECRET_REQ = (byte)0x09; //API网关请求
	public static final byte MSG_TYPE_EXSECRET_RESP = (byte)0x0A;//API网关请求响应
	
	public static final byte MSG_TYPE_ID_REQ = (byte)0x0B; //Id请求
	public static final byte MSG_TYPE_ID_RESP = (byte)0x0C;//Id请求响应
	
	public static final byte MSG_TYPE_HEARBEAT_REQ = (byte)0x0D; //心跳请求
	public static final byte MSG_TYPE_HEARBEAT_RESP = (byte)0x0E;//心跳响应
	
	public static final byte MSG_TYPE_SYSTEM_REQ_JRPC = 0x0F; // 特殊RPC 接口，如ID RPC接口中
	public static final byte MSG_TYPE_SPECAIL_RRESP_JRPC = 0x10;//16
	
	public static final byte MSG_TYPE_OBJECT_STORAGE = 0x11; //17 对象存储消息
	public static final byte MSG_TYPE_OBJECT_STORAGE_RESP = 0x12;//18
	
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
	
	public static final String CACHE_DIR_PREFIX = "CP:";
	
	//缓存策略
	public static final byte CACHE_TYPE_NO = 0;
	
	//服务方法级缓存
	public static final byte CACHE_TYPE_MCODE = 1;
	
	//服务方法 + 账号
	public static final byte CACHE_TYPE_ACCOUNT = 2;
	
	//消息payload计算hash作为key
	public static final byte CACHE_TYPE_PAYLOAD = 3;
	
	//消息payload计算hash作为key + 账号
	public static final byte CACHE_TYPE_PAYLOAD_AND_ACT = 4;
	
	//操作类型，具体意义由实现方及使用方加以细化
	public static final byte OP_TYPE_ADD = 1;
	public static final byte OP_TYPE_UPDATE = 2;
	public static final byte OP_TYPE_DELETE = 3;
	public static final byte OP_TYPE_QUERY = 4;
	public static final byte OP_TYPE_START = 5;//开始
	public static final byte OP_TYPE_END = 6;//结束
	public static final byte OP_TYPE_PAUSE = 7;//暂停
	public static final byte OP_TYPE_RESUME = 8;//唤醒
	public static final byte OP_TYPE_STATUS = 9;
	
}
