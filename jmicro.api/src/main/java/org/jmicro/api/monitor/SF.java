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

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.config.Config;
import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class SF {
	
	public static boolean linkStart(IReq req) {
		if(isMonitorable(MonitorConstant.LINK_START)) {
			SubmitItem si = createSubmitItem(MonitorConstant.LINK_START,req,null,null,null,null,false);
			return monitor().submit(si);
		}
		return false;
	}
	
	public static boolean linkEnd(IReq req,IResp resp) {
		if(isMonitorable(MonitorConstant.LINK_END)) {
			SubmitItem si = createSubmitItem(MonitorConstant.LINK_END,req,null,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean serviceNotFound(IReq req) {
		if(isMonitorable(MonitorConstant.SERVICE_NOT_FOUND)) {
			SubmitItem si = createSubmitItem(MonitorConstant.SERVICE_NOT_FOUND,req,null,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqStart(IReq req) {
		if(isMonitorable(MonitorConstant.REQ_START)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_START,req,null,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqEnd(IReq req,IResp resp) {
		if(isMonitorable(MonitorConstant.REQ_END)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_END,req,null,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqTimeout(IReq req) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_TIMEOUT,req,null,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqTimeoutFail(IReq req) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT_FAIL)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_TIMEOUT_FAIL,req,null,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqTimeoutRetry(IReq req) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT_RETRY)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_TIMEOUT_RETRY,req,null,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	/**
	 * 服务器响应一个业务错误
	 * @param req
	 * @param se
	 * @return
	 */
	public static boolean reqServiceRespError(IReq req,ServerError se) {
		if(isMonitorable(MonitorConstant.CLIENT_GET_RESPONSE_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.CLIENT_GET_RESPONSE_ERROR,req,null,
					"code: "+se.getErrorCode()+",msg: "+se.getMsg(),null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	/**
	 * 服务器发生系统级错误
	 * @param req
	 * @param msg
	 * @return
	 */
	public static boolean reqServerError(IReq req,String msg) {
		if(isMonitorable(MonitorConstant.CLIENT_GET_SERVER_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.CLIENT_GET_SERVER_ERROR,req,null,msg,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqError(IReq req,String msg) {
		if(isMonitorable(MonitorConstant.REQ_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_ERROR,req,null,msg,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqSuccess(IReq req,IResp resp) {
		if(isMonitorable(MonitorConstant.REQ_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_ERROR,req,resp,null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean serverStart(String host,int port,String desc) {
		if(monitor() != null) {
			SubmitItem si = createSubmitItem(MonitorConstant.SERVER_START,null,null,
					"host:"+host+", port:"+port+",desc"+desc,null,null,true);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean serverStop(String host,int port) {
		SubmitItem si = createSubmitItem(MonitorConstant.CLIENT_GET_SERVER_ERROR,null,null,
				"host:"+host+", port:"+port,null,null,true);
		return monitor().submit(si);
	}
	
	public static boolean netIo(short type,String desc,Class cls,Throwable ex) {
		if(isMonitorable(type)) {
			SubmitItem si = createSubmitItem(type,null,null,
					desc,cls,ex,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean netIoRead(short type,long num) {
		if(isMonitorable(type)) {
			SubmitItem si = createSubmitItem(type,null,null,
					num+"",null,null,false);
			
			return  monitor().submit(si);
		}
		return false;
	}
	
	
	public static boolean limit(IReq req) {
		if(isMonitorable(MonitorConstant.SERVICE_SPEED_LIMIT)) {
			SubmitItem si = createSubmitItem(MonitorConstant.SERVICE_SPEED_LIMIT,req,null,
					null,null,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	private static SubmitItem createSubmitItem(short type,IReq req,IResp resp,String desc,Class cls
			,Throwable ex,boolean canCache) {
		SubmitItem si = new SubmitItem();
		si.setType(type);
		si.setReq(req);
		si.setResp(resp);
		si.setDesc(desc);
		si.setCanCache(canCache);
		
		if(ex != null) {
			LogEntry le = new LogEntry(MonitorConstant.LOG_ERROR,cls.getName(),ex,desc);
			si.setLog(le);
		}
		setCommon(si);
		return si;
	}
	
/*	public static boolean doSubmit(int type,String... objs){
		if(isMonitorable(type)) {
			return  monitor().submit(type,objs);
		}
		return false;
	}
	
	public static boolean doSubmit(int type,Throwable exp,String... objs){
		if(isMonitorable(type)) {
			 monitor().submit(type,exp,objs);
		}
		return false;
	}
	
	public static boolean doSubmit(int type,IReq req,Throwable exp,String... objs){
		if(isMonitorable(type)) {
			 return monitor().submit(type, req,exp,objs);
		}
		return false;
	}
	
	public static boolean doSubmit(int type,IResp resp,Throwable exp,String... objs){
		if(isMonitorable(type)) {
			 return monitor().submit(type,resp,exp,objs);
		}
		return false;
	}
	
	public static boolean doSubmit(int type,IReq req,IResp resp,Throwable exp,String... objs){
		if(isMonitorable(type)) {
			 return monitor().submit(type, req,resp,exp,objs);
		}
		return false;
	}
	
	public static boolean doSubmit(int type,Message msg,Throwable exp,String... objs){
		if(isMonitorable(type)) {
			 return monitor().submit(type, msg,exp,objs);
		}
		return false;
	}
	*/
	
	public static void doServiceLog(byte level,Class<?> cls,Throwable exp,Object... others) {
		doLog(level,cls,null,null,null,exp,others);
	}

	public static void doBussinessLog(byte level, Class<?> tag, Throwable exp, Object... msgs) {
		doServiceLog(level,tag,exp,msgs);
	}
	
	public static void doRequestLog(byte level,Class<?> cls,IReq req,Throwable exp, Object... others) {
		doLog(level,cls,req,null,null,exp,others);
	}
	
	public static void doResponseLog(byte level,Class<?> cls,IReq req,IResp resp,Throwable exp, Object... others) {
		doLog(level,cls,req,resp,null,exp,others);
	}
	
	public static void doMessageLog(byte level,Class<?> cls,Message msg,Throwable exp,Object... others) {
		doLog(level,cls,null,null,msg,exp,others);
	}
	
	private static void doLog(byte level,Class<?> cls,IReq req,IResp resp,Message msg,Throwable exp, Object... others) {
		if(isLoggable(level)) {
			SubmitItem si = createLogSubmitItem(level,cls,exp,others);
			si.setReq(req);
			si.setResp(resp);
			si.setMsg(msg);
			monitor().submit(si);
		}
	}
	
	private static void setCommon(SubmitItem si) {
		if(JMicroContext.existsContext()) {
			//在RPC上下文中才有以上信息
			si.setLinkId(JMicroContext.lid());
			si.setLocalPort(JMicroContext.get().getString(JMicroContext.LOCAL_PORT, ""));
			si.setRemoteHost(JMicroContext.get().getString(JMicroContext.REMOTE_HOST, ""));
			si.setRemotePort(JMicroContext.get().getString(JMicroContext.REMOTE_PORT, ""));
			si.setSm((ServiceMethod)JMicroContext.get().getObject(Constants.SERVICE_METHOD_KEY, null));
		}
		si.setLocalHost(Config.getHost());
		si.setInstanceName(Config.getInstanceName());
		si.setTime(System.currentTimeMillis());
	}
	
	private static SubmitItem createLogSubmitItem(byte level,Class<?> cls,Throwable exp,Object... others) {
		LogEntry le = new LogEntry(level,cls.getName(),exp,others);
		SubmitItem si = new SubmitItem(le);
		si.setType(MonitorConstant.LINKER_ROUTER_MONITOR);
		setCommon(si);
		return si;
	}
	
	private static IMonitorDataSubmiter monitor() {
		IMonitorDataSubmiter m = JMicroContext.get().getParam(JMicroContext.MONITOR, null);
		if(m == null) {
			m = JMicro.getObjectFactory().get(IMonitorDataSubmiter.class);
		}
		return m;
	}
	
	private static boolean isMonitorable(short type) {
		return  JMicroContext.get().isMonitor() && monitor() != null && monitor().canSubmit(type);
	}
	
	/**
	 * 日志输出4个条件
	 * 1. 对应组件打开debug模式，isDebug=true 或者服务方法loggable=true;
	 * 2. 日志级别大开
	 * 
	 * @param isComOpen
	 * @param level
	 * @return
	 */
	public static boolean isLoggable(int level) {

		String  srvName = JMicroContext.get().getParam(org.jmicro.api.JMicroContext.CLIENT_SERVICE, null);
		//String  ns = JMicroContext.get().getParam(org.jmicro.api.JMicroContext.CLIENT_NAMESPACE, null);
		//String  method = JMicroContext.get().getParam(org.jmicro.api.JMicroContext.CLIENT_METHOD, null);
		
		if("org.jmicro.api.monitor.IMonitorDataSubscriber".equals(srvName)) {
			//日志提交RPC本身肯定不能通过此方式记录日志，否则进入死循环了
			//if("onSubmit".equals(method) && "".equals(ns) ) {
				return false;
			//}
		}
		
		 ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		 
		 if(sm == null) {
			 //在非RPC上下文中，直接记录日志
			 return true;
		 }
		 //如果级别大于或等于错误，不管RPC的配置如何，肯定需要日志
		return (level >= sm.getLogLevel()) && monitor() != null;
	}
	
	
	
}
