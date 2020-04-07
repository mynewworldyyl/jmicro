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

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.config.Config;
import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class SFv1 {
	
	public static boolean linkStart(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.LINK_START)) {
			SubmitItem si = createSubmitItem(MonitorConstant.LINK_START,req,null,null,tag,null,false);
			return monitor().submit(si);
		}
		return false;
	}
	
	public static boolean linkEnd(String tag,IReq req,IResp resp) {
		if(isMonitorable(MonitorConstant.LINK_END)) {
			SubmitItem si = createSubmitItem(MonitorConstant.LINK_END,req,null,null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean serviceNotFound(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.SERVICE_NOT_FOUND)) {
			SubmitItem si = createSubmitItem(MonitorConstant.SERVICE_NOT_FOUND,req,null,null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqStart(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.REQ_START)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_START,req,null,null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqEnd(String tag,IReq req,IResp resp) {
		if(isMonitorable(MonitorConstant.REQ_END)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_END,req,null,null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqTimeout(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_TIMEOUT,req,null,null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqTimeoutFail(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT_FAIL)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_TIMEOUT_FAIL,req,null,null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqTimeoutRetry(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT_RETRY)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_TIMEOUT_RETRY,req,null,null,tag,null,false);
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
	public static boolean reqServiceRespError(String tag,IReq req,ServerError se) {
		if(isMonitorable(MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR,req,null,
					"code: "+se.getErrorCode()+",msg: "+se.getMsg(),tag,null,false);
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
	public static boolean reqServerError(String tag,IReq req,String msg) {
		if(isMonitorable(MonitorConstant.CLIENT_SERVICE_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.CLIENT_SERVICE_ERROR,req,null,msg,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqError(String tag,IReq req,String msg) {
		if(isMonitorable(MonitorConstant.REQ_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_ERROR,req,null,msg,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean reqSuccess(String tag,IReq req,IResp resp) {
		if(isMonitorable(MonitorConstant.REQ_ERROR)) {
			SubmitItem si = createSubmitItem(MonitorConstant.REQ_ERROR,req,resp,null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean serverStart(String tag,String host,int port,String desc) {
		if(monitor() != null) {
			SubmitItem si = createSubmitItem(MonitorConstant.SERVER_START,null,null,
					"host:"+host+", port:"+port+",desc: "+desc,tag,null,true);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean serverStop(String tag,String host,int port) {
		SubmitItem si = createSubmitItem(MonitorConstant.CLIENT_SERVICE_ERROR,null,null,
				"host:"+host+", port:"+port,tag,null,true);
		return monitor().submit(si);
	}
	
	public static boolean netIo(short type,String desc,Class cls,Throwable ex) {
		if(isMonitorable(type)) {
			SubmitItem si = createSubmitItem(type,null,null,
					desc,cls.getName(),ex,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	public static boolean netIoRead(String tag,short type,long num,ISession session) {
		if(isMonitorable(type)) {
			SubmitItem si = createSubmitItem(type,null,null,
					num+"",tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	
	public static boolean limit(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.SERVICE_SPEED_LIMIT)) {
			SubmitItem si = createSubmitItem(MonitorConstant.SERVICE_SPEED_LIMIT,req,null,
					null,tag,null,false);
			return  monitor().submit(si);
		}
		return false;
	}
	
	private static SubmitItem createSubmitItem(short type,IReq req,IResp resp,String desc,String tag
			,Throwable ex,boolean canCache) {
		SubmitItem si = new SubmitItem();
		si.setType(type);
		si.setReq(req);
		si.setResp(resp);
		si.setDesc(desc);
		si.setCanCache(canCache);
		si.setTag(tag);
		
		si.setLevel(MonitorConstant.LOG_ERROR);
		si.setEx(ex);
		//si.setOthers(others);

		setCommon(si);
		return si;
	}
	
	public static void doServiceLog(byte level,Class<?> cls,Throwable exp,String desc,Object... others) {
		doLog(level,cls,null,null,null,exp,desc,others);
	}

	public static void doBussinessLog(byte level, Class<?> tag, Throwable exp,String desc, Object... msgs) {
		doServiceLog(level,tag,exp,desc,msgs);
	}
	
	public static void doRequestLog(byte level,Class<?> cls,IReq req,Throwable exp,String desc, Object... others) {
		doLog(level,cls,req,null,null,exp,desc,others);
	}
	
	public static void doResponseLog(byte level,Class<?> cls,IReq req,IResp resp,Throwable exp,String desc, Object... others) {
		doLog(level,cls,req,resp,null,exp,desc,others);
	}
	
	public static void doMessageLog(byte level,Class<?> cls,Message msg,Throwable exp,String desc,Object... others) {
		doLog(level,cls,null,null,msg,exp,desc,others);
	}
	
	private static void doLog(byte level,Class<?> cls,IReq req,IResp resp,Message msg,Throwable exp, String desc,Object... others) {
		if(isLoggable(level)) {
			SubmitItem si = createLogSubmitItem(level,cls,exp,others);
			si.setReq(req);
			si.setResp(resp);
			si.setMsg(msg);
			si.setTag(cls.getName());
			si.setDesc(desc);
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
		//LogEntry le = new LogEntry(level,exp,others);
		SubmitItem si = new SubmitItem();
		si.setTag(cls.getName());
		si.setType(MonitorConstant.LINKER_ROUTER_MONITOR);
		setCommon(si);
		si.setLevel(level);
		si.setEx(exp);
		si.setOthers(others);
		
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
