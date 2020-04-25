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
import org.jmicro.api.monitor.v2.IMonitorServer;
import org.jmicro.api.monitor.v2.MRpcItem;
import org.jmicro.api.monitor.v2.MonitorClient;
import org.jmicro.api.monitor.v2.OneItem;
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
public class SF {
	
	private static MonitorClient m = null;
	
	public static boolean linkStart(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.LINK_START)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.setReq(req);
			mi.addOneItem(MonitorConstant.LINK_START, tag);
			return true;
		}
		return false;
	}
	
	public static boolean linkEnd(String tag,IResp resp) {
		if(isMonitorable(MonitorConstant.LINK_END)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.setResp(resp);
			mi.addOneItem(MonitorConstant.LINK_END, tag);
			return true;
		}
		return false;
	}
	
	public static boolean serviceNotFound(String tag) {
		if(isMonitorable(MonitorConstant.SERVICE_NOT_FOUND)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.addOneItem(MonitorConstant.SERVICE_NOT_FOUND, tag);
			return true;
		}
		return false;
	}
	
	public static boolean reqStart(String tag,IReq req) {
		if(isMonitorable(MonitorConstant.REQ_START)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.setReq(req);
			mi.addOneItem(MonitorConstant.REQ_START, tag);
			return true;
		}
		return false;
	}
	
	public static boolean reqEnd(String tag,IResp resp) {
		if(isMonitorable(MonitorConstant.REQ_END)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.setResp(resp);
			mi.addOneItem(MonitorConstant.REQ_END, tag);
			return true;
		}
		return false;
	}
	
	public static boolean reqTimeout(String tag) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.addOneItem(MonitorConstant.REQ_TIMEOUT, tag);
			return true;
		}
		return false;
	}
	
	public static boolean reqTimeoutFail(String tag) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT_FAIL)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.addOneItem(MonitorConstant.REQ_TIMEOUT_FAIL, tag);
			return true;
		}
		return false;
	}
	
	public static boolean reqTimeoutRetry(String tag) {
		if(isMonitorable(MonitorConstant.REQ_TIMEOUT_RETRY)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.addOneItem(MonitorConstant.REQ_TIMEOUT_RETRY, tag);
			return true;
		}
		return false;
	}
	
	/**
	 * 服务器响应一个业务错误
	 * @param req
	 * @param se
	 * @return
	 */
	public static boolean reqServiceRespError(String tag,ServerError se) {
		if(isMonitorable(MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			OneItem oi = mi.addOneItem(MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR, tag,se.toString());
			return true;
		}
		return false;
	}
	
	/**
	 * 服务器发生系统级错误
	 * @param req
	 * @param msg
	 * @return
	 */
	public static boolean reqServerError(String tag,String msg) {
		if(isMonitorable(MonitorConstant.CLIENT_SERVICE_ERROR)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.addOneItem(MonitorConstant.CLIENT_SERVICE_ERROR, tag,msg);
			return true;
		}
		return false;
	}
	
	public static boolean reqError(String tag,String msg) {
		if(isMonitorable(MonitorConstant.REQ_ERROR)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.addOneItem(MonitorConstant.REQ_ERROR, tag,msg);
			return true;
		}
		return false;
	}
	
	public static boolean reqSuccess(String tag) {
		if(isMonitorable(MonitorConstant.REQ_SUCCESS)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.addOneItem(MonitorConstant.REQ_SUCCESS, tag);
			return true;
		}
		return false;
	}
	
	public static boolean breakService(String tag,ServiceMethod sm,String desc) {
		MonitorClient mo = monitor();
		if(mo.isServerReady() && !mo.canSubmit(MonitorConstant.SERVICE_BREAK)) {
			return false;
		}
		MRpcItem mi = new MRpcItem();
		mi.setSm(sm);
		mi.addOneItem(MonitorConstant.SERVICE_BREAK, tag,desc);
		setCommon(mi);
		mo.readySubmit(mi);
		return true;
	}
	
	public static boolean netIo(short type,String desc,Class cls,Throwable ex) {
		
		MonitorClient mo = monitor();
		if(mo.isServerReady() && !mo.canSubmit(type)) {
			return false;
		}
		
		MRpcItem mi = JMicroContext.get().getMRpcItem();
		boolean f = false;
		if(mi == null) {
			mi = new MRpcItem();
			f = true;
		}
		OneItem oi = mi.addOneItem(type, cls.getName(),desc);
		oi.setEx(ex);
		if(f) {
			setCommon(mi);
			return mo.submit2Cache(mi);
		}
		return false;
		
	}
	
	public static boolean netIoRead(String tag,short type,long num) {
		MonitorClient mo = monitor();
		if(mo.isServerReady() && !mo.canSubmit(type)) {
			return false;
		}
		
		MRpcItem mi = JMicroContext.get().getMRpcItem();
		boolean f = false;
		if(mi == null) {
			mi = new MRpcItem();
			f = true;
		}
		OneItem oi = mi.addOneItem(type, tag);
		oi.setVal(num);
		if(f) {
			setCommon(mi);
			return mo.submit2Cache(mi);
		}
		return true;
	}
	
	
	public static boolean limit(String tag) {
		if(isMonitorable(MonitorConstant.SERVICE_SPEED_LIMIT)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			OneItem oi = mi.addOneItem(MonitorConstant.SERVICE_SPEED_LIMIT, tag);
		}
		return false;
	}
	
	public static void doServiceLog(byte level,Class<?> cls,Throwable exp,String desc) {
		doLog(level,cls,null,exp,desc);
	}

	public static void doBussinessLog(byte level, Class<?> tag, Throwable exp,String desc) {
		doLog(level,tag,null,exp,desc);
	}
	
	public static void doRequestLog(byte level,Class<?> cls,Throwable exp,String desc) {
		doLog(level,cls,null,exp,desc);
	}
	
	public static void doResponseLog(byte level,Class<?> cls,Throwable exp,String desc) {
		doLog(level,cls,null,exp,desc);
	}
	
	public static void doMessageLog(byte level,Class<?> cls,Message msg,Throwable exp,String desc) {
		doLog(level,cls,msg,exp,desc);
	}
	
	private static void doLog(byte level,Class<?> cls,Message msg,Throwable exp, String desc) {
		if(isLoggable(level)) {
			int lineNum = Thread.currentThread().getStackTrace()[3].getLineNumber();
			
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			boolean f = false;
			if(mi == null) {
				mi = new MRpcItem();
				f = true;
			}
			desc += ", Line: " + lineNum;
			if(msg != null && msg.isDebugMode()) {
				desc += ", Method: "+ msg.getMethod();
				mi.setReqId(msg.getReqId());
				mi.setLinkId(msg.getLinkId());
			}
			OneItem oi = mi.addOneItem(MonitorConstant.LINKER_ROUTER_MONITOR, cls.getName(),desc);
			oi.setEx(exp);
			oi.setLevel(level);
			mi.setMsg(msg);
			if(f) {
				setCommon(mi);
				monitor().submit2Cache(mi);
			}
		}
	}
	
	public static void setCommon(MRpcItem si) {
		if(si == null) {
			return;
		}

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
	}
	
	private static MonitorClient monitor() {
		if(m== null) {
			m = JMicro.getObjectFactory().get(MonitorClient.class);
		}
		return m;
	}
	
	private static boolean isMonitorable(short type) {
		
		if(!JMicroContext.get().isMonitorable()) {
			return false;
		}
		if(isMonitorServer()) {
			return false;
		}
		
		return monitor().canSubmit(type);
	}
	
	private static boolean isMonitorServer() {
		String  srvName = JMicroContext.get().getParam(org.jmicro.api.JMicroContext.CLIENT_SERVICE, null);
		if(srvName != null) {
			//自身的RPC本身肯定不能通过此方式记录日志，否则进入死循环了
			return IMonitorServer.class.getName().equals(srvName);
		}
		return false;
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
	public static boolean isLoggable(int needLevel,int ...rpcMethodLevel) {
		MonitorClient mo = monitor();
		if(mo.isServerReady() && !mo.canSubmit(MonitorConstant.LINKER_ROUTER_MONITOR) 
				|| needLevel == MonitorConstant.LOG_NO) {
			return false;
		}
		
		if(isMonitorServer()) {
			return false;
		}
		
		int rpcLevel;
		if(rpcMethodLevel == null || rpcMethodLevel.length == 0) {
			ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
			 if(sm != null) {
				 rpcLevel = sm.getLogLevel();
			 } else {
				 //默认错误日志强制监控
				 rpcLevel = MonitorConstant.LOG_ERROR;
			 }
		}else {
			rpcLevel = rpcMethodLevel[0];
		}
		 
		 //如果级别大于或等于错误，不管RPC的配置如何，肯定需要日志
		return needLevel >= rpcLevel;
	}
	
	
	
}
