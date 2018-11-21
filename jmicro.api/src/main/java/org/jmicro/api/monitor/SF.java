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
import org.jmicro.api.config.Config;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.ServiceItem;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class SF {

	private static final SF ins = new SF();
	
	private SF() {}
	
	public static SF getIns() {
		return ins;
	}
	
	private IMonitorDataSubmiter monitor() {
		return JMicroContext.get().getParam(JMicroContext.MONITOR, null);
	}
	
	public void doLog(int level,Class<?> cls,Long linkId,String sn,String ns,String v,String m,Object[] args,Object... others) {
		IMonitorDataSubmiter monitor = this.monitor();
		if(JMicroContext.get().isMonitor() && monitor != null) {
			StringBuffer sb = logHeaders(cls.getSimpleName());
			service(sb, sn, ns, v, m, args);
			others(sb,others);
			this.submitRouterLog(sb.toString(), level,linkId);
		}
	}
	
	public void doLog(int level,Class<?> cls,Message msg,Object... others) {
		IMonitorDataSubmiter monitor = this.monitor();
		if(JMicroContext.get().isMonitor() && monitor != null) {
			StringBuffer sb = logHeaders(cls.getSimpleName());
			message(sb,msg);
			others(sb,others);
			this.submitRouterLog(sb.toString(), level,msg.getLinkId());
		}
	}
	
	private void submitRouterLog(String msg,int level,Long linkId) {
		IMonitorDataSubmiter monitor = this.monitor();
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.LINKER_ROUTER_MONITOR);
		si.setLevel(level);
		si.setResult(msg);
		si.setLinkId(linkId);
		monitor.submit(si);
	}
	
	private StringBuffer logHeaders(String tag) {
		StringBuffer sb = new StringBuffer("[").append(tag);
		sb.append("] host [").append(Config.getHost());
		sb.append("] instanceName[").append(Config.getInstanceName());
		sb.append("]");
		return sb;
	}
	
	private void service(StringBuffer sb,String sn,String ns,String v,String method,Object[] args) {
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
	
	private void others(StringBuffer sb, Object[] others) {
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
	
	private StringBuffer message(StringBuffer sb,  Message msg) {
		sb.append("msgId[").append(msg.getId()).append("] reqId[")
		.append(msg.getReqId()).append("]");
		return sb;
	}
	
}
