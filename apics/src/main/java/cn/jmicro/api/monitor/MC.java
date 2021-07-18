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
package cn.jmicro.api.monitor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

/**
 * 全部RPC过程事件都需要记录，linkID和ReqID，在RPC请求开始时记录Request请求全部数据，后面通过ReqID关联查找请求数据，通过linkID查找链路
 * 
 * 若要重新发起一个之前发起过的RPC，只需要复制Request请求数据并发起即可，linkID和ReqID需要重新生成。
 * 
 * 一个RPC链路可以有一个或多个RPC请求，但
 * 
 * RPC链路开始 记录Req实例ID，从此ID可以找到链路的开始请求实例数据
 * RPC链路结束 记录Response实例ID，从此ID找到链路结束时返回数据
 * 
 * RPC请求开始  记录Req数据
 * RPC请求结束 记录返回结果Response实例数据，成功失败标识
 * 
 * RPC超时 计数一次超时
 * 
 * RPC服务熔断 RPC请求时，服务已经被熔断
 * RPC服务熔断开启
 * RPC服务熔断关闭
 * RPC服务熔断半开启
 * 
 * RPC客户端请求时，服务已经被熔断
 * RPC服务未找到
 * 
 * RPC客户端异常　可能由于服务端抛异常导致客户端异常
 * RPC服务端异常　服务端异常，客户端肯定也异常 
 * 
 * @author Beck Ye
 * @date 2018年10月4日-上午11:49:26
 */
public final class MC {
	
	private MC(){}
	
	@MCA(group="deft", value="默认类型")
	public static final short MT_DEFAULT  = 0;

	@MCA(group="rpc", value="请求开始")
	public static final short MT_REQ_START  = 0x0001;
	@MCA(group="rpc", value="请求结束")
	public static final short MT_REQ_END  = 0X0002;
	
	@MCA(group="rpc", value="链路开始")
	public static final short MT_LINK_START  = 0X0003;
	
	@MCA(group="rpc", value="链路结束")
	public static final short MT_LINK_END  = 0X0004;
	
	@MCA(group="rpc", value="请求超时 ")
	public static final short MT_REQ_TIMEOUT = 0x0005;
	
	@MCA(group="rpc", value="服务未找到",desc="ServiceItem未找到")
	public static final short MT_SERVICE_ITEM_NOT_FOUND = 0x0006;
	
	//客户端接收到服务器错误，非业务逻辑返回错误 246
	@MCA(group="rpc",value="服务器错误",desc="客户端收到ServerError实例")
	public static final short MT_CLIENT_RESPONSE_SERVER_ERROR = 0x0007;
	
	//服务业务错误，即业务逻辑错误
	@MCA(value="服务业务错误 ", desc = "由应用自行提交，平台将其作为一种错误标识并加以应用")
	public static final short MT_SERVICE_ERROR = 0x0008;
	
	@MCA(group="instance", value="服务器开启 ", desc="MT_PROCESS_REMOVE对应服务退出，开启服务编排器情况下才可监测到")
	public static final short MT_SERVER_START = 0x0009;
	
	@MCA(group="instance", value="服务停止 ", desc="MT_PROCESS_REMOVE对应服务退出，开启服务编排器情况下才可监测到")
	public static final short MT_SERVER_STOP = 0x000A;
	
	/*@MCA("线程池拒绝任务")
	public static final short MT_EXECUTOR_REJECT = 0x000A;*/
	
	/*@MCA("服务端服务不存在")
	public static final short MT_SERVER_REQ_SERVICE_NOT_FOUND = 0x000B;*/
	
	@MCA(group="rpc",value="服务限速")
	public static final short MT_SERVICE_SPEED_LIMIT = 0x000C;
	
	@MCA(group="rpc",value="请求超时失败")
	public static final short MT_REQ_TIMEOUT_FAIL = 0x000D;
	
	@MCA(group="clientNetIo",value="客户端网络通信关闭")
	public static final short MT_CLIENT_IOSESSION_CLOSE = 0x000E;
	
	@MCA(group="clientNetIo",value="客户端网络通信打开")
	public static final short MT_CLIENT_IOSESSION_OPEN = 0x000F;
	
	@MCA(group="clientNetIo",value="客户端网络链路空闲事件")
	public static final short MT_CLIENT_IOSESSION_IDLE = 0x0010;
	
	@MCA(group="clientNetIo",value="客户端IO写数据（上行）")
	public static final short MT_CLIENT_IOSESSION_WRITE = 0x012;
	
	//网络下行流量
	@MCA(group="clientNetIo",value="客户端IO读数据（下行）")
	public static final short MT_CLIENT_IOSESSION_READ =  0x0013;
	
	@MCA(group="clientNetIo",value="客户端网络IO异步")
	public static final short MT_CLIENT_IOSESSION_EXCEPTION = 0x0014;
	//public static final short CLIENT_PACKAGE_SESSION_ID_ERR = 0X00E5;
	
	@MCA(group="serverNetIo",value="服务端网络关闭")
	public static final short MT_SERVER_IOSESSION_CLOSE = 0x0015;
	
	@MCA(group="serverNetIo",value="服务端网络打开")
	public static final short MT_SERVER_IOSESSION_OPEN =  0x0016;
	
	@MCA(group="serverNetIo",value="服务端链路空闲事件")
	public static final short MT_SERVER_IOSESSION_IDLE =  0x0017;
	
	@MCA(group="serverNetIo",value="服务端IO写数据（下行）")
	public static final short MT_SERVER_JRPC_RESPONSE_SUCCESS = 0x0018;
	
	//网络上行流量
	@MCA(group="serverNetIo",value="服务端IO读数据次数（上行包数量）")
	public static final short MT_SERVER_JRPC_GET_REQUEST =  0x0019;
	
	@MCA(group="serverNetIo",value="服务端网络IO异步")
    public static final short MT_SERVER_IOSESSION_EXCEPTION = 0x001A;
    
    //public static final short LINKER_ROUTER_MONITOR = 0X00EC;
	@MCA(group="clientNetIo",value="客户端连接失败")
    public static final short MT_CLIENT_CONNECT_FAIL = 0x001B;
    
	@MCA(group="rpc",value="请求超时重试")
	public static final short MT_REQ_TIMEOUT_RETRY = 0x001C;
	
	@MCA(group="rpc",value="未知错误")
	public static final short MT_REQ_ERROR = 0x001D;
	
	@MCA(group="rpc",value="请求成功")
	public static final short MT_REQ_SUCCESS = 0x001E;
	
	@MCA(group="rpc",value="服务熔断")
	public static final short MT_SERVICE_BREAK = 0x001F;
	
	@MCA(group="rpc",value="消息处理器未找到")
	public static final short MT_HANDLER_NOT_FOUND = 0x0020;
	
	@MCA(group="rpc",value="异步RPC失败")
	public static final short MT_ASYNC_RPC_FAIL = 0x0021;
	
	@MCA(group="rpc",value="服务方法未找到")
	public static final short MT_SERVICE_METHOD_NOT_FOUND = 0x0022;
	
	@MCA(group="choyController", value="进程启动")
	public static final short MT_PROCESS_ADD = 0x0023;
	@MCA(group="choyController", value="进程关闭")
	public static final short MT_PROCESS_REMOVE = 0x0024;
	@MCA(group="choyController", value="进程日志")
	public static final short MT_PROCESS_LOG = 0x0025;
	
	@MCA(group="choyAgent", value="主机代理启动")
	public static final short MT_AGENT_ADD = 0x0026;
	@MCA(group="choyAgent", value="主机代理关闭")
	public static final short MT_AGENT_REMOVE = 0x0027;
	@MCA(group="choyAgent", value="主机代理日志")
	public static final short MT_AGENT_LOG = 0x0028;
	
	@MCA(group="choyController", value="服务部署描述加入")
	public static final short MT_DEPLOYMENT_ADD = 0x0029;
	@MCA(group="choyController", value="服务部署描述移除")
	public static final short MT_DEPLOYMENT_REMOVE = 0x002A;
	@MCA(group="choyController", value="服务部署描述更新")
	public static final short MT_DEPLOYMENT_UPDATE = 0x002B;
	@MCA(group="choyController", value="服务部署描述日志")
	public static final short MT_DEPLOYMENT_LOG = 0x002C;
	
	@MCA(group="choyController", value="服务分配增加")
	public static final short MT_ASSIGN_ADD = 0x002D;
	@MCA(group="choyController", value="服务分配移除")
	public static final short MT_ASSIGN_REMOVE = 0x002E;
	@MCA(group="choyController", value="服务分配日志")
	public static final short MT_ASSIGN_LOG = 0x002F;
	
	@MCA("消息订阅日志")
	public static final short MT_PUBSUB_LOG = 0x0030;
	
	@MCA("平台日志")
	public static final short MT_PLATFORM_LOG = 0x0031;
	
	@MCA(value="接收消息数量", group="ms")
	public static final short Ms_ReceiveItemCnt = 0x0032;
	
	@MCA(value="失败消息数量", group="ms")
	public static final short Ms_FailItemCount = 0x0033;
	
	@MCA(value="线程提交消息数量", group="ms")
	public static final short Ms_CheckerSubmitItemCnt = 0x0034;
	
	@MCA(value="任务提交成功消息数量", group="ms")
	public static final short Ms_TaskSuccessItemCnt = 0x0035;
	
	@MCA(value="任务提交失败消息数量", group="ms")
	public static final short Ms_TaskFailItemCnt = 0x0036;
	
	@MCA(value="提交消息数量", group="ms")
	public static final short Ms_SubmitCnt = 0x0037;
	
	@MCA(value="提交到消息篮子失败消息数量", group="ms")
	public static final short Ms_Fail2BorrowBasket = 0x0038;
	
	@MCA(value="线程提交异常消息数量", group="ms")
	public static final short Ms_CheckerExpCnt = 0x0039;
	
	@MCA(value="线程循环次数", group="ms")
	public static final short Ms_CheckLoopCnt = 0x003A;
	
	@MCA(value="归还篮子失败次数", group="ms")
	public static final short Ms_FailReturnWriteBasket = 0x003B;
	
	@MCA(value="收到无效消息主题", group=Constants.PUBSUB_KEY)
	public static final short Ms_TopicInvalid = 0x003C;
	
	@MCA(value="消息服务器强制丢弃消息", group=Constants.PUBSUB_KEY)
	public static final short Ms_ServerDisgard = 0x003D;
	
	@MCA(value="消息服务器繁忙", group=Constants.PUBSUB_KEY)
	public static final short Ms_ServerBusy = 0x003E;
	
	@MCA(value="消息入分发队列", group=Constants.PUBSUB_KEY)
	public static final short Ms_Pub2Cache = 0x003F;
	
	@MCA(value="提交任务次数", group=Constants.PUBSUB_KEY)
	public static final short Ms_SubmitTaskCnt = 0x0040;
	
	@MCA(value="消息重新分发次数", group=Constants.PUBSUB_KEY)
	public static final short Ms_DoResendCnt = 0x0041;
	
	@MCA(value="消息重新分发遇到接收器为空次数", group=Constants.PUBSUB_KEY)
	public static final short Ms_DoResendWithCbNullCnt = 0x0042;
	
	//总成功数 
	@MCA(value="总成功数", group=Constants.STATIS_KEY,desc="业务失败，RPC成功 两者之和为总成功RPC数")
	public static final short STATIS_TOTAL_SUCCESS = 0x0043;
	
	//总失败数
	@MCA(value="总失败数", group=Constants.STATIS_KEY,desc="服务器错误，未知错误，超时失败  三者之和为失败RPC总数")
	public static final short STATIS_TOTAL_FAIL = 0x0044;
	
	@MCA(value="总成功数所占比率", group=Constants.STATIS_KEY, desc="")
	public static final short STATIS_SUCCESS_PERCENT = 0x0045;
	
	@MCA(value="总失败数所占请求数比率", group=Constants.STATIS_KEY, desc="")
	public static final short STATIS_FAIL_PERCENT = 0x0046;
	
	//超时数 REQ_TIMEOUT 总数
	@MCA(value="超时总数", group=Constants.STATIS_KEY, desc="")
	public static final short STATIS_TOTAL_TIMEOUT = 0x0047;
	
	//超时失败百分比  REQ_TIMEOUT_FAIL 占 RPC请求总数
	@MCA(value="超时失败百分比占 RPC请求总数", group=Constants.STATIS_KEY, desc="")
	public static final short STATIS_TIMEOUT_PERCENT = 0x0048;
	
	//MonitorConstant.CLIENT_IOSESSION_READ 总数即为服务响应数
	@MCA(value="RPC服务响应总数", group=Constants.STATIS_KEY, desc="")
	public static final short STATIS_TOTAL_RESP = 0x0049;
	
	@MCA("应用日志")
	public static final short MT_APP_LOG = 0x004A;
	
	@MCA("服务器错误")
	public static final short MT_SERVER_ERROR = 0x004B;
	
	@MCA("无效登陆信息")
	public static final short MT_INVALID_LOGIN_INFO = 0x004C;
	
	@MCA("服务代理实例未找到")
	public static final short MT_SERVICE_RROXY_NOT_FOUND = 0x004D;
	
	@MCA(value="线程池终止", group=Constants.EXECUTOR_POOL, desc="")
	public static final short EP_TERMINAL = 0x004E;
	
	@MCA(value="线程池开始", group=Constants.EXECUTOR_POOL, desc="")
	public static final short EP_START = 0x004F;
	
	@MCA(value="队列任务数报警", group=Constants.EXECUTOR_POOL, desc="")
	public static final short EP_TASK_WARNING = 0x0050;
	
	@MCA(value="线程池拒绝任务提交", group=Constants.EXECUTOR_POOL, desc="")
	public static final short MT_EXECUTOR_REJECT = 0x0051;
	
	@MCA(group="rpc",value="RPC参数大小超过限定大小", desc="")
	public static final short MT_PACKET_TOO_MAX = 0x0052;
	
	@MCA(value="clientId权限拒绝", desc="")
	public static final short MT_CLIENT_ID_REJECT = 0x0053;
	
	@MCA(value="账号权限拒绝", desc="")
	public static final short MT_ACT_PERMISSION_REJECT = 0x0054;
	
	@MCA(group="clientNetIo",value="客户端写字节数", desc="")
	public static final short MT_CLIENT_IOSESSION_WRITE_BYTES = 0x0055;
	
	@MCA(group="clientNetIo",value="客户端读字节数", desc="")
	public static final short MT_CLIENT_IOSESSION_READ_BYTES = 0x0056;
	
	@MCA(group="serverNetIo",value="服务端IO读数据字节数（上行）")
	public static final short MT_SERVER_JRPC_GET_REQUEST_READ =  0x0057;
	
	@MCA(group="serverNetIo",value="服务端IO写数据字节数（下行）")
	public static final short MT_SERVER_JRPC_RESPONSE_WRITE =  0x0058;
	
	@MCA(group="rpc",value="服务端限速队例消息入队")
	public static final short MT_SERVER_LIMIT_MESSAGE_PUSH =  0x0059;
	
	@MCA(group="rpc",value="服务端限速队例消息拒绝")
	public static final short MT_SERVER_LIMIT_MESSAGE_REJECT =  0x005A;
	
	@MCA(group="rpc",value="服务端限速队例消息出队列")
	public static final short MT_SERVER_LIMIT_MESSAGE_POP =  0x005B;
	
	@MCA(group="rpc",value="AES解密错误，需要重密钥")
	public static final short MT_AES_DECRYPT_ERROR =  0x005C;
	
	public static final short KEEP_MAX_VAL = 0x0FFF;
	
	public static final short INVALID_VAL = -32768;
	
	//每秒响应数定义为 QPS？ 
	//public static final short STATIS_QPS = 9;
	
	public static final String TEST_SERVICE_METHOD_TOPIC = "/statics/smTopic";
	
    //日志级别
	//0表示禁止日志监控
	
	public static final byte LOG_DEPEND = -1;
	
	public static final byte LOG_NO = 0;
	  
    public static final byte LOG_TRANCE = 1;
    
    public static final byte LOG_DEBUG = 2;
    
    public static final byte LOG_INFO = 3;
    
    public static final byte LOG_WARN = 4;
    
    public static final byte LOG_ERROR = 5;
    
    public static final byte LOG_FINAL = 6;
    
    public static final byte LOG_FORCE = 7;
    
    public static final String PREFIX_TOTAL = "total";
    public static final String PREFIX_TOTAL_PERCENT = "totalPercent";
    public static final String PREFIX_QPS = "qps";
    public static final String PREFIX_CUR = "cur";
    public static final String PREFIX_CUR_PERCENT = "curPercent";
    
    public static final String TYPE_DEF_GROUP = "deflt";
    
    //以下类型出现时，将数据全部上传服务器，不做数据清除，如Rep数据，响应数据
    public static final Set<Short> MT_TYPES = new HashSet<>();
    public static final Set<Short> MS_TYPES = new HashSet<>();
    public static final Set<Short> MTMS_TYPES = new HashSet<>();
    public static final Set<Short> STATIS_TYPES = new HashSet<>();
    public static final Set<Short> EP_TYPES = new HashSet<>();
    
    public static final List<MCConfigJRso> MC_CONFIGS = new ArrayList<>();
    
    public static final Map<String,Byte> LogKey2Val = new HashMap<>();
    public static final Map<String,Short> MT_Key2Val = new HashMap<>();
    
    public static Short[] MT_TYPES_ARR;
    public static Short[] MS_TYPES_ARR;
    public static Short[] MTMS_TYPES_ARR;
    public static Short[] EP_TYPES_ARR;
    
    public static Short[] STATIS_TYPES_ARR;
    public static Short[] STATIS_INDEX_ARR;
    
	public static final Map<Short,String> MONITOR_VAL_2_KEY = new HashMap<>();
	static {
		
		Field[] fs = MC.class.getDeclaredFields();
		for(Field f: fs){
			
			try {
				
				if(f.getType() == Byte.TYPE) {
					String name = f.getName();
					Byte val = f.getByte(null);
					if(name.startsWith("LOG_")) {
						LogKey2Val.put(name, val);
					}
					continue;
				}
				
				if(!f.isAnnotationPresent(MCA.class)){
					continue;
				}
				
				MCA mca = f.getAnnotation(MCA.class);
				String name = f.getName();
				Short val = f.getShort(null);
				
				MCConfigJRso mcc = new MCConfigJRso();
				mcc.setDesc(mca.desc());
				mcc.setFieldName(name);
				mcc.setType(val);
				mcc.setLabel(mca.value());
				
				if(Utils.isEmpty(mca.group())) {
					mcc.setGroup(MC.TYPE_DEF_GROUP);
				}else {
					mcc.setGroup(mca.group());
				}
				
				MC_CONFIGS.add(mcc);
				
				MONITOR_VAL_2_KEY.put(val,name);
				
				if(name.startsWith("MT_")) {
					MT_TYPES.add(val);
					MTMS_TYPES.add(val);
					MT_Key2Val.put(name, val);
				}else if(name.startsWith("Ms_")) {
					MS_TYPES.add(val);
					MTMS_TYPES.add(val);
				}else if(name.startsWith("STATIS_")) {
					STATIS_TYPES.add(val);
				}else if(name.startsWith("EP_")) {
					EP_TYPES.add(val);
					MTMS_TYPES.add(val);
				}
				
				MT_TYPES_ARR = new Short[MT_TYPES.size()];
				MT_TYPES.toArray(MT_TYPES_ARR);
				
				MS_TYPES_ARR = new Short[MS_TYPES.size()];
				MS_TYPES.toArray(MS_TYPES_ARR);
				
				EP_TYPES_ARR = new Short[EP_TYPES.size()];
				EP_TYPES.toArray(EP_TYPES_ARR);
				
				STATIS_TYPES_ARR = new Short[STATIS_TYPES.size()];
				STATIS_TYPES.toArray(STATIS_TYPES_ARR);
					
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
