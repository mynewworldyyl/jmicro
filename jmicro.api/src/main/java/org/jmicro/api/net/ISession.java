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
package org.jmicro.api.net;

import java.nio.ByteBuffer;

import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.ServiceCounter;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:27
 */
public interface ISession{
	
	public static final int EVENT_TYPE_CLOSE = 1;
	
	public static final int EVENT_TYPE_OPEN = 2;
	
	
	public static final short CLIENT_HANDLER_NOT_FOUND = 0X7FFF;
	
	public static final short CLIENT_WRITE_BYTES = 0X7FFE;
	
	public static final short CLIENT_READ_BYTES = 0X7FFD;
	
	public static final Short[] STATIS_TYPES = new Short[]{
			//服务器发生错误,返回ServerError异常
			MonitorConstant.CLIENT_GET_SERVER_ERROR,
			//业务错误,success=false,此时接口调用正常
			MonitorConstant.CLIENT_GET_RESPONSE_ERROR,
			//请求超时
			MonitorConstant.REQ_TIMEOUT,
			//请求开始
			MonitorConstant.REQ_START,
			//异步请求成功确认包
			//MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS,
			//同步请求成功
			MonitorConstant.REQ_SUCCESS,
			//超时次数
			MonitorConstant.REQ_TIMEOUT_FAIL,
			
			ISession.CLIENT_HANDLER_NOT_FOUND,
			ISession.CLIENT_WRITE_BYTES,
			ISession.CLIENT_READ_BYTES
		};
	
	void close(boolean flag);
	
	<T> T getParam(String key);
	
	<T> void putParam(String key,T obj);
	
	//ByteBuffer getReadBuffer();
	int getReadBufferSize();
	
	//server write response, or client write no need response request
	void write(Message bb);
	
	public boolean isClose();
	
	/**
	 * 关闭会话前等待状态
	 * @return
	 */
	boolean waitingClose();
	
	void active();
	
	boolean isActive();
	
	boolean isServer();
	
	String remoteHost();
	
	int remotePort();
	
	String localHost();
	
	int localPort();
	
    long getId();
	
	void setId(long id);
	
	void receive(ByteBuffer msg) ;
	
	void setDumpUpStream(boolean dump);
	
	void setDumpDownStream(boolean dump);
	
	void increment(Short type);
	
	Double getFailPercent();
	
	Double getTakePercent(Short type);
	
	Double getTakeAvg(Short type);
	
	ServiceCounter getServiceCounter();
	
	void addSessionListener(ISessionListener lis);
	
	void removeSessionListener(ISessionListener lis);
	
	void notifySessionEvent(int eventType);
	
}
