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
package org.jmicro.api.monitor.v1;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
public final class MonitorConstant {
	
	private MonitorConstant(){}

    //请求开始 240
	public static final short REQ_START  = 0X00F0;
	//请求结束
	public static final short REQ_END  = 0X00F1;
	
	//链路开始
	public static final short LINK_START  = 0X00F2;
	//链路结束
	public static final short LINK_END  = 0X00F3;
	
	//请求超时 
	public static final short REQ_TIMEOUT = 0X00F4;
	
	//服务未找到
	public static final short SERVICE_NOT_FOUND = 0X00F5;
	
	//客户端接收到服务器错误，非业务逻辑返回错误 246
	public static final short CLIENT_RESPONSE_SERVER_ERROR = 0X00F6;
	
	//服务业务错误，即业务逻辑错误
	public static final short CLIENT_SERVICE_ERROR = 0X00F7;
	
	public static final short SERVER_START =           0X00F8;
	public static final short SERVER_STOP =            0X00F9;
	public static final short SERVER_REQ_SERVICE_NOT_FOUND = 0X00FA;
	
	public static final short SERVICE_SPEED_LIMIT = 0X00FB;
	
	//请求超时
	public static final short REQ_TIMEOUT_FAIL = 0X00FC;
	
	public static final short CLIENT_IOSESSION_CLOSE = 0X00FD;
	
	public static final short CLIENT_IOSESSION_OPEN = 0X00E0;
	public static final short CLIENT_IOSESSION_IDLE = 0X00E1;
	public static final short CLIENT_IOSESSION_WRITE = 0X00E2;
	//网络下行流量
	public static final short CLIENT_IOSESSION_READ =  0X00E3;
	public static final short CLIENT_IOSESSION_EXCEPTION = 0X00E4;
	//public static final short CLIENT_PACKAGE_SESSION_ID_ERR = 0X00E5;
	
	public static final short SERVER_IOSESSION_CLOSE = 0X00E6;
	public static final short SERVER_IOSESSION_OPEN =  0X00E7;
	public static final short SERVER_IOSESSION_IDLE =  0X00E8;
	public static final short SERVER_IOSESSION_WRITE = 0X00E9;
	//网络上行流量
	public static final short SERVER_IOSESSION_READ =  0X00EA;
    public static final short SERVER_IOSESSION_EXCEPTION = 0X00EB;
    
    public static final short LINKER_ROUTER_MONITOR = 0X00EC;
    
    public static final short CLIENT_CONNECT_FAIL = 0X00ED;
    
	//请求超时重试
	public static final short REQ_TIMEOUT_RETRY = 0X00EF;
	
	//未知错误
	public static final short REQ_ERROR = 0X00D0;
	
	//未知错误 209
	public static final short REQ_SUCCESS = 0X00D1;
	
	//未知错误 209
	public static final short SERVICE_BREAK = 0X00D0;
	
	public static final short CLIENT_HANDLER_NOT_FOUND = 0X01FF;
	
	public static final short CLIENT_WRITE_BYTES = 0X02FE;
	
	public static final short CLIENT_READ_BYTES = 0X03FD;
	
	public static final short Ms_ReceiveItemCnt = 0X04FD;
	
	public static final short Ms_FailItemCount = 0X05FD;
	
	public static final short Ms_CheckerSubmitItemCnt = 0X06FD;
	
	public static final short Ms_TaskSuccessItemCnt = 0X07FD;
	
	public static final short Ms_TaskFailItemCnt = 0X08FD;
	
	public static final short Ms_SubmitCnt = 0X09FD;
	
	public static final short Ms_Fail2BorrowBasket = 0X0AFD;
	
	public static final short Ms_CheckerExpCnt = 0X0BFD;
	public static final short Ms_CheckLoopCnt = 0X0CFD;
	
	public static final short Ms_FailReturnWriteBasket = 0X0DFD;
	
	public static final short Ms_TopicInvalid = 0X0EFD;
	public static final short Ms_ServerDisgard = 0X0FFD;
	public static final short Ms_ServerBusy = 0X10FD;
	public static final short Ms_Pub2Cache = 0X11FD;
	
	public static final short Ms_SubmitTaskCnt = 0X12FD;
	
	public static final short Ms_DoResendCnt = 0X13FD;
	public static final short Ms_DoResendWithCbNullCnt = 0X14FD;
	
	//总成功数  业务失败，RPC成功 两者之和为总成功RPC数
	public static final short STATIS_TOTAL_SUCCESS = 3;
	
	//总失败数  服务器错误，未知错误，超时失败  三者之和为失败RPC总数
	public static final short STATIS_TOTAL_FAIL = 4;
	
	//总成功数所占比率
	public static final short STATIS_SUCCESS_PERCENT = 5;
	
	//总失败数所占请求数比率，
	public static final short STATIS_FAIL_PERCENT = 1;
	
	//超时数 REQ_TIMEOUT 总数
	public static final short STATIS_TOTAL_TIMEOUT = 6;
	
	//超时失败百分比  REQ_TIMEOUT_FAIL 占 RPC请求总数
	public static final short STATIS_TIMEOUT_PERCENT = 7;
	
	//MonitorConstant.CLIENT_IOSESSION_READ 总数即为服务响应数
	public static final short STATIS_TOTAL_RESP = 8;
	
	//每秒响应数定义为 QPS？ 
	//public static final short STATIS_QPS = 9;
	
	public static final String TEST_SERVICE_METHOD_TOPIC = "/statics/smTopic";
	
	public static final Short[] STATIS_INDEX = new Short[] { 
			STATIS_TOTAL_SUCCESS,
			STATIS_TOTAL_FAIL,
			STATIS_SUCCESS_PERCENT,
			STATIS_FAIL_PERCENT,
			STATIS_TOTAL_TIMEOUT,
			STATIS_TIMEOUT_PERCENT,
			STATIS_TOTAL_RESP,
	};
	
	public static final Short[] STATIS_TYPES = new Short[] { 
			MonitorConstant.REQ_START, 
			MonitorConstant.REQ_END,
			MonitorConstant.LINK_START,
			MonitorConstant.LINK_END,
			MonitorConstant.REQ_TIMEOUT,
			MonitorConstant.SERVICE_NOT_FOUND,
			MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR,
			MonitorConstant.CLIENT_SERVICE_ERROR,
			MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,
			MonitorConstant.SERVICE_SPEED_LIMIT,
			MonitorConstant.REQ_TIMEOUT_FAIL,
			MonitorConstant.REQ_TIMEOUT_RETRY,
			MonitorConstant.REQ_ERROR,
			MonitorConstant.REQ_SUCCESS,
			MonitorConstant.CLIENT_CONNECT_FAIL,
			
			MonitorConstant.CLIENT_IOSESSION_CLOSE,
			MonitorConstant.CLIENT_IOSESSION_OPEN,
			MonitorConstant.CLIENT_IOSESSION_IDLE,
			MonitorConstant.CLIENT_IOSESSION_WRITE,
			MonitorConstant.CLIENT_IOSESSION_READ,
			MonitorConstant.CLIENT_IOSESSION_EXCEPTION,
			//MonitorConstant.CLIENT_PACKAGE_SESSION_ID_ERR,
			
			MonitorConstant.SERVER_IOSESSION_CLOSE,
			MonitorConstant.SERVER_IOSESSION_OPEN,
			MonitorConstant.SERVER_IOSESSION_IDLE,
			MonitorConstant.SERVER_IOSESSION_WRITE,
			MonitorConstant.SERVER_IOSESSION_READ,
			MonitorConstant.SERVER_IOSESSION_EXCEPTION,
			MonitorConstant.SERVER_START,
			MonitorConstant.SERVER_STOP,
	};
	
    //日志级别
	//0表示禁止日志监控
	public static final byte LOG_NO = 0;
	  
    public static final byte LOG_TRANCE = 1;
    
    public static final byte LOG_DEBUG = 2;
    
    public static final byte LOG_INFO = 3;
    
    public static final byte LOG_WARN = 4;
    
    public static final byte LOG_ERROR = 5;
    
    public static final byte LOG_FINAL = 6;
    
    public static final String PREFIX_TOTAL = "total";
    public static final String PREFIX_TOTAL_PERCENT = "totalPercent";
    public static final String PREFIX_QPS = "qps";
    public static final String PREFIX_CUR = "cur";
    public static final String PREFIX_CUR_PERCENT = "curPercent";
    
    //以下类型出现时，将数据全部上传服务器，不做数据清除，如Rep数据，响应数据
    public static final Set<Short> KEY_TYPES = new HashSet<>();
    
	
	public static final Map<Short,String> MONITOR_VAL_2_KEY = new HashMap<>();
	static {
		KEY_TYPES.add(REQ_TIMEOUT);
		KEY_TYPES.add(SERVICE_NOT_FOUND);
		KEY_TYPES.add(CLIENT_RESPONSE_SERVER_ERROR);
		KEY_TYPES.add(CLIENT_SERVICE_ERROR);
		KEY_TYPES.add(SERVER_REQ_SERVICE_NOT_FOUND);
		KEY_TYPES.add(REQ_TIMEOUT_FAIL);
		KEY_TYPES.add(SERVICE_SPEED_LIMIT);
		KEY_TYPES.add(REQ_TIMEOUT_RETRY);
		KEY_TYPES.add(REQ_ERROR);
		KEY_TYPES.add(SERVICE_BREAK);
		
		Field[] fs = MonitorConstant.class.getDeclaredFields();
		for(Field f: fs){
			if(!Modifier.isStatic(f.getModifiers()) || !Modifier.isPublic(f.getModifiers())
					|| f.getType() != Short.TYPE ){
				continue;
			}
			try {
				MONITOR_VAL_2_KEY.put(f.getShort(null), f.getName());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
