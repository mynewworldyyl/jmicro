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

import cn.jmicro.api.EnterMain;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class MT {
	
	private static StatisMonitorClient m = null;
	
	private static boolean isInit = false;
	
	private static boolean isMs = false;
	
	private static boolean isDs = false;
	
	public static boolean rpcEvent(ServiceMethodJRso sm,short type,long val) {
		if(sm != null && sm.getMonitorEnable() == 1 && isInit && m != null 
				&& m.canSubmit(sm,type,Config.getClientId())) {
			JMStatisItemJRso mi = new JMStatisItemJRso();
			mi.setKey(sm.getKey().fullStringKey());
			setCommon(mi);
			mi.addType(type,val);
			return m.submit2Cache(mi);
		}
		return false;
	} 
	
	public static boolean rpcEvent(short type,long val) {
		if(!JMicroContext.existRpcContext()) {
			return nonRpcEvent(Config.getInstanceName(),type,val);
		}
		
		if(isMonitorable(type)) {
			JMStatisItemJRso mi = JMicroContext.get().getMRpcStatisItem();
			mi.addType(type,val);
			return true;
		}
		return false;
	} 
	
	public static boolean nonRpcEvent(String key,short type,long num) {
		if(!isMonitorable(type)) {
			return false;
		}
		
		if(JMicroContext.existRpcContext()) {
			return rpcEvent(type,num);
		}
		
		JMStatisItemJRso mi = new JMStatisItemJRso();
		mi.setKey(key);
		setCommon(mi);
		
		mi.addType(type,num);
		
		return m.submit2Cache(mi);
		
	}
	
	public static boolean rpcEvent(short type) {
		return rpcEvent(type,1);
	} 
	
	public static boolean nonRpcEvent(String key,short type) {
		return nonRpcEvent(key,type,1);
	}
	
	public static boolean nonRpcEvent(short type) {
		return nonRpcEvent(Config.getInstanceName(),type,1);
	}
	
	private static ServiceMethodJRso sm() {
		if(JMicroContext.existRpcContext()) {
			return JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		}
		return null;
	}
	
	public static void setCommon(JMStatisItemJRso si) {
		if(si == null) {
			return;
		}

		if(JMicroContext.existRpcContext()) {
			ActInfoJRso ai = JMicroContext.get().getAccount();
			if(ai != null) {
				si.setClientId(ai.getClientId());
			} /*else {
				si.setClientId(Config.getClientId());
			}*/
			//在RPC上下文中才有以上信息
			ServiceMethodJRso sm = (ServiceMethodJRso)JMicroContext.get().getObject(Constants.SERVICE_METHOD_KEY, null);
			si.setLocalPort(JMicroContext.get().getString(JMicroContext.LOCAL_PORT, ""));
			si.setRemoteHost(JMicroContext.get().getString(JMicroContext.REMOTE_HOST, ""));
			si.setRemotePort(JMicroContext.get().getString(JMicroContext.REMOTE_PORT, ""));
			//si.setKey(sm.getKey().toKey(true, true, true));
			si.setSmKey(sm.getKey());
			si.setKey(sm.getKey().fullStringKey());
			si.setRpc(true);
		} else {
			si.setRpc(false);
			si.setClientId(Config.getClientId());
		}
		si.setLocalHost(Config.getExportSocketHost());
		si.setInstanceName(Config.getInstanceName());
	}
	
	private static boolean isMonitorable(short type) {
		
		if(!isInit || m == null) {
			IObjectFactory of = EnterMain.getObjectFactory();
			if(of == null) {
				return false;
			}
			m = of.get(StatisMonitorClient.class,false);
			if(m == null) return false;
			
			isMs = null != of.get(IStatisMonitorServerJMSrv.class,false);
			isDs = of.get(IMonitorDataSubscriberJMSrv.class,false) != null;
			
			isInit = true;
			
		}
		
		if(JMicroContext.existRpcContext() && !JMicroContext.get().isMonitorable()
				|| m == null || !m.isServerReady()) {
			return false;
		}
		
		if((isDs || isMs) && JMicroContext.existLinkId()) {
			//avoid dead loop
			return false;
		}
		
		if(JMicroContext.existRpcContext()) {
			ActInfoJRso ai = JMicroContext.get().getAccount();
			if(ai != null) {
				return m.canSubmit(sm(),type,ai.getClientId());
			}
		}
		
		return m.canSubmit(sm(),type,Config.getClientId());
		
	}
	
}
