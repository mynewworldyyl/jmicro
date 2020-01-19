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

import org.jmicro.api.JMicroContext;
import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class SF {
	
	public static boolean doSubmit(int type,String... objs){
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
	
	public static void doServiceLog(byte level,Class<?> cls,ServiceMethod sm,Throwable exp,String... others) {
		long linkId = JMicroContext.lid();
		IMonitorDataSubmiter monitor = monitor();
		if(monitor != null) {
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,linkId,sm,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			monitor.submit(si);
		}
	}
	
	public static void doBussinessLog(byte debug, Class<?> tag, Throwable exp, String... msgs) {
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		doServiceLog(debug,tag,sm,exp,msgs);
	}

	
	public static void doRequestLog(byte level,Class<?> cls,IReq req,Throwable exp, String... others) {
		long linkId = JMicroContext.lid();
		IMonitorDataSubmiter monitor = monitor();
		if(monitor != null) {
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,linkId,
					req,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			monitor.submit(si);
		}
	}
	
	public static void doResponseLog(byte level,Class<?> cls,IResp resq,Throwable exp, String... others) {
		long linkId = JMicroContext.lid();
		IMonitorDataSubmiter monitor = monitor();
		if(monitor != null) {
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,linkId,
					resq,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			monitor.submit(si);
		}
	}
	
	public static void doMessageLog(byte level,Class<?> cls,Message msg,Throwable exp,String... others) {
		long linkId = JMicroContext.lid();
		IMonitorDataSubmiter monitor = monitor();
		if(monitor != null) {
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,
					msg,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			si.setLinkId(linkId);
			monitor.submit(si);
		}
	}
	
	public static IMonitorDataSubmiter monitor() {
		return JMicroContext.get().getParam(JMicroContext.MONITOR, null);
	}
	
	public static boolean isMonitorable(Integer type) {
		return JMicroContext.get().isMonitor() && monitor() != null && monitor().canSubmit(type);
	}
	
	public static boolean isLoggable(boolean isComOpen,int level) {

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
			 return true;
		 }
		 //如果级别大于或等于错误，不管RPC的配置如何，肯定需要日志
		return (JMicroContext.get().isLoggable(isComOpen) || level >= MonitorConstant.LOG_ERROR) && monitor() != null;
	}
	
}
