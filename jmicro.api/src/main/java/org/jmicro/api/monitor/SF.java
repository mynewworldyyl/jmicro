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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.config.Config;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class SF {
	
	public static void doSubmit(int type,String... objs){
		if(needLog()) {
			 monitor().submit(type,objs);
		}
	}
	
	public static void doSubmit(int type,Throwable exp,String... objs){
		if(needLog()) {
			 monitor().submit(type,exp,objs);
		}
	}
	
	public static void doSubmit(int type,IRequest req,Throwable exp,String... objs){
		if(needLog()) {
			 monitor().submit(type, req,exp,objs);
		}
	}
	
	public static void doSubmit(int type,IResponse resp,Throwable exp,String... objs){
		if(needLog()) {
			 monitor().submit(type,resp,exp,objs);
		}
	}
	
	public static void doSubmit(int type,IRequest req,IResponse resp,Throwable exp,String... objs){
		if(needLog()) {
			 monitor().submit(type, req,resp,exp,objs);
		}
	}
	
	public static void doSubmit(int type,Message msg,Throwable exp,String... objs){
		if(needLog()) {
			 monitor().submit(type, msg,exp,objs);
		}
	}
	
	public static void doServiceLog(byte level,Class<?> cls,long linkId,String sn,String ns,String v,
			String m,Object[] args,Throwable exp,String... others) {
		if(needLog()) {
			IMonitorDataSubmiter monitor = monitor();
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,linkId,
					sn,ns,v,m,args,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			monitor.submit(si);
		}
	}
	
	public static void doRequestLog(byte level,long linkId,Class<?> cls,IRequest req,Throwable exp, String... others) {
		if(needLog()) {
			IMonitorDataSubmiter monitor = monitor();
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,linkId,
					req,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			monitor.submit(si);
		}
	}
	
	public static void doResponseLog(byte level,long linkId,Class<?> cls,IResponse resq,Throwable exp, String... others) {
		if(needLog()) {
			IMonitorDataSubmiter monitor = monitor();
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,linkId,
					resq,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			monitor.submit(si);
		}
	}
	
	public static void doMessageLog(byte level,Class<?> cls,Message msg,Throwable exp,String... others) {
		if(needLog()) {
			IMonitorDataSubmiter monitor = monitor();
			SubmitItem si = new SubmitItem(MonitorConstant.LINKER_ROUTER_MONITOR,level,
					msg,others);
			si.setTagCls(cls.getName());
			si.setEx(exp);
			monitor.submit(si);
		}
	}
	
	private static IMonitorDataSubmiter monitor() {
		return JMicroContext.get().getParam(JMicroContext.MONITOR, null);
	}
	
	private static boolean needLog() {
		return JMicroContext.get().isMonitor() && monitor() != null;
	}
	
	private static void response(StringBuilder sb, IResponse resp) {
		sb.append("reqId[").append(resp.getRequestId()).append("] success[")
		.append(resp.isSuccess()).append("] monitorable[")
		.append(resp.isMonitorEnable()).append("] result[")
		.append(resp.getResult()).append("]");
	}

	private static void reqeust(StringBuilder sb, IRequest req) {
		sb.append("reqId[").append(req.getId()).append("]");
		service(sb, req.getServiceName(), req.getNamespace(), req.getVersion(), req.getMethod(),
				req.getArgs());
	}

	private static StringBuilder logHeaders(String tag) {
		StringBuilder sb = new StringBuilder("[").append(tag);
		sb.append("] host [").append(Config.getHost());
		sb.append("] instanceName[").append(Config.getInstanceName());
		if(Config.isClientOnly()) {
			sb.append("] side[").append(Constants.SIDE_COMSUMER);
		} else if(Config.isServerOnly()) {
			sb.append("] side[").append(Constants.SIDE_PROVIDER);
		} else {
			sb.append("] side[").append(Constants.SIDE_ANY);
		}
		sb.append("]");
		return sb;
	}
	
	private static void service(StringBuilder sb,String sn,String ns,String v,String method,Object[] args) {
		sb.append(" service[").append(ServiceItem.serviceName(sn, ns, v))
		.append("&").append(method).append("] args[");
		if(args != null && args.length > 0) {
			for(int i=0; i< args.length;i++) {
				sb.append(args[i].getClass()).append("=").append(args[i]);
				if(i != args.length-1) {
					sb.append("&");
				}
			}
		}
		sb.append("] ");
	}
	
	private static void others(StringBuilder sb, Object[] others) {
		if(others != null && others.length > 0) {
			sb.append("Others[");
			for(int i=0; i< others.length;i++) {
				sb.append(others[i]);
				if(i != others.length-1) {
					sb.append("&");
				}
			}
		}
		sb.append("]");
	}
	
	private static StringBuilder message(StringBuilder sb,  Message msg) {
		sb.append("msgId[").append(msg.getId()).append("] reqId[")
		.append(msg.getReqId()).append("] type[")
		.append(Integer.toHexString(msg.getType())).append("] flag[")
		.append(Integer.toHexString(msg.getFlag())).append("]");
		return sb;
	}
	
}
