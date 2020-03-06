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
package org.jmicro.api.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

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

    //请求开始
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
	
	//客户端接收到一个错误
	public static final short CLIENT_GET_RESPONSE_ERROR = 0X00F6;
	
	//客户端接收到一个错误
	public static final short CLIENT_GET_SERVER_ERROR = 0X00F7;
	
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
	public static final short CLIENT_PACKAGE_SESSION_ID_ERR = 0X00E5;
	
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
	
	//未知错误
	public static final short REQ_SUCCESS = 0X00D1;
	
    
    public static final byte LOG_TRANCE = 1;
    
    public static final byte LOG_DEBUG = 2;
    
    public static final byte LOG_INFO = 3;
    
    public static final byte LOG_WARN = 4;
    
    public static final byte LOG_ERROR = 5;
    
    public static final byte LOG_FINAL = 6;
    
    //总失败数所占请求数比率
	public static final short STATIS_FAIL_PERCENT = 1;
	
	//总请求数
	public static final short STATIS_TOTAL_REQ = 2;
	
	//总成功数
	public static final short STATIS_TOTAL_SUCCESS = 3;
	
	//总失败数
	public static final short STATIS_TOTAL_FAIL = 4;
	
	//总成功数所占比率
	public static final short STATIS_SUCCESS_PERCENT = 5;
	
	//超时数
	public static final short STATIS_TOTAL_TIMEOUT = 6;
	
	//超时百分比
	public static final short STATIS_TIMEOUT_PERCENT = 7;
	
	public static final short STATIS_TOTAL_RESP = 8;
	
	public static final short STATIS_QPS = 9;
	
	public static final String TEST_SERVICE_METHOD_TOPIC = "/statics/smTopic";
	
	public static final Map<Integer,String> MONITOR_VAL_2_KEY = new HashMap<>();
	static {
		Field[] fs = MonitorConstant.class.getDeclaredFields();
		for(Field f: fs){
			if(!Modifier.isStatic(f.getModifiers()) || !(f.getName().startsWith("SERVER_") 
					|| f.getName().startsWith("CLIENT_") || f.getName().startsWith("STATIS_") 
					|| f.getName().startsWith("LINKER_ROUTER")) ){
				continue;
			}
			try {
				MONITOR_VAL_2_KEY.put(f.getInt(null), f.getName());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
