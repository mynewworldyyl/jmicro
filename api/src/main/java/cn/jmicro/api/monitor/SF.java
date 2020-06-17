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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class SF {
	
	private final static Logger logger = LoggerFactory.getLogger("cn.jmicro.api.monitor.sf.SFF");
	
	private static MonitorClient m = null;
	
	private static boolean isInit = false;
	
	private static boolean isMs = false;
	
	private static boolean isDs = false;
	
	public static boolean linkStart(String tag,IReq req) {
		return reqEvent(MC.MT_LINK_START,MC.LOG_DEBUG,req,tag);
	}
	
	public static boolean linkEnd(String tag,IResp resp) {
		return respEvent(MC.MT_LINK_END,MC.LOG_DEBUG,resp,tag);
	}
	
	public static boolean reqStart(String tag,IReq req) {
		return reqEvent(MC.MT_REQ_START,MC.LOG_DEBUG,req,tag);
	}
	
	public static boolean reqEnd(String tag,IResp resp) {
		return respEvent(MC.MT_REQ_END,MC.LOG_DEBUG,resp,tag);
	}
	
	public static boolean respEvent(short type,byte level,IResp resp,String tag) {
		if(isMonitorable(type)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.setResp(resp);
			mi.addOneItem(type,level,tag,"");
			return true;
		}else {
			if(level >= MC.LOG_DEBUG && logger.isDebugEnabled()) {
				logger.debug("Disgard: type:" + MC.MONITOR_VAL_2_KEY.get(type) + ",level: " + level + " tag: " +tag + " Resp: "
			+JsonUtils.getIns().toJson(resp));
			}
			return false;
		}
	} 
	
	public static boolean reqEvent(short type,byte level,IReq req,String tag) {
		if(isMonitorable(type)) {
			MRpcItem mi = JMicroContext.get().getMRpcItem();
			mi.setReq(req);
			mi.addOneItem(type,level,tag,"");
			return true;
		}else {
			if(level >= MC.LOG_DEBUG && logger.isDebugEnabled()) {
				logger.debug("Disgard: type:" + MC.MONITOR_VAL_2_KEY.get(type) + ",level: " + level + " tag: " +tag + " Req: "+JsonUtils.getIns().toJson(req));
			}
			return false;
		}
	} 
	
	public static boolean serviceNotFound(String tag,String desc) {
		return eventLog(MC.MT_SERVICE_NOT_FOUND,MC.LOG_ERROR, tag,desc);
	}
	
	public static boolean reqTimeout(String tag,String desc) {
		return eventLog(MC.MT_REQ_TIMEOUT,MC.LOG_WARN, tag,desc);
	}
	
	public static boolean reqTimeoutFail(String tag,String desc) {
		return eventLog(MC.MT_REQ_TIMEOUT_FAIL,MC.LOG_ERROR, tag,desc);
	}
	
	public static boolean reqTimeoutRetry(String tag,String desc) {
		return eventLog(MC.MT_REQ_TIMEOUT_FAIL,MC.LOG_WARN, tag,desc);
	}
	
	public static boolean eventLog(short type,byte level,String tag,String desc) {
		if(isMonitorable(type)) {
			MRpcItem mi = null;
			if(JMicroContext.existRpcContext()) {
				 mi = JMicroContext.get().getMRpcItem();
			}
			boolean f = false;
			if(mi == null) {
				 f = true;
				 mi = new MRpcItem();
			}
			
			mi.addOneItem(type,level, tag,desc);
			if(f) {
				setCommon(mi);
				return m.readySubmit(mi);
			}
		}else {
			if(level >= MC.LOG_DEBUG && logger.isDebugEnabled()) {
				logger.debug("Disgard: type:" + MC.MONITOR_VAL_2_KEY.get(type) + ", level: " + level + ", tag: " +tag + ", Desc: "+desc);
			}
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
		return eventLog(MC.MT_CLIENT_RESPONSE_SERVER_ERROR,MC.LOG_ERROR, tag,se.toString());
	}
	
	/**
	 * 服务器发生系统级错误
	 * @param req
	 * @param msg
	 * @return
	 */
	public static boolean reqServerError(String tag,String msg) {
		return eventLog(MC.MT_CLIENT_SERVICE_ERROR,MC.LOG_ERROR, tag,msg);
	}
	
	public static boolean reqError(String tag,String msg) {
		return eventLog(MC.MT_REQ_ERROR,MC.LOG_ERROR, tag,msg);
	}
	
	public static boolean reqSuccess(String tag) {
		return eventLog(MC.MT_REQ_SUCCESS,MC.LOG_DEBUG, tag,"");
	}
	
	public static boolean breakService(String tag,ServiceMethod sm,String desc) {
		if(isMonitorable(MC.MT_SERVICE_BREAK)) {
			MRpcItem mi = new MRpcItem();
			mi.setSm(sm);
			mi.addOneItem(MC.MT_SERVICE_BREAK,MC.LOG_WARN, tag,desc);
			setCommon(mi);
			m.readySubmit(mi);
			return true;
		}else {
			if(logger.isDebugEnabled()) {
				logger.debug("Disgard: type:MT_SERVICE_BREAK, level: " + MC.LOG_WARN + ", tag: " +tag + ", Desc: "+desc);
			}
			return false;
		}
		
	}
	
	
	
	public static boolean netIo(short type,byte level,String desc,Class cls,Throwable ex) {
		if(!isMonitorable(type)) {
			if(logger.isDebugEnabled()) {
				logger.debug("Disgard: type:" + MC.MONITOR_VAL_2_KEY.get(type) + ", level: " + level + ", tag: " +cls.getName() + ", Desc: "+desc);
			}
			if(ex != null) {
				logger.error("",ex);
			}
			return false;
		}
		if(m.isServerReady() && !m.canSubmit(sm(),type)) {
			return false;
		}
		
		MRpcItem mi = null;
		if(JMicroContext.existRpcContext()) {
			mi = JMicroContext.get().getMRpcItem();
		}
		
		boolean f = false;
		if(mi == null) {
			mi = new MRpcItem();
			f = true;
		}
		
		OneItem oi = mi.addOneItem(type,level,cls.getName(),desc);
		oi.setEx(ex);
		if(f) {
			setCommon(mi);
			return m.submit2Cache(mi);
		}
		return false;
		
	}
	
	public static boolean netIoRead(String tag,short type,long num) {
		if(m.isServerReady() && !m.canSubmit(sm(),type)) {
			if(logger.isDebugEnabled()) {
				logger.debug("Disgard: type:" + MC.MONITOR_VAL_2_KEY.get(type) + ", level: " + MC.LOG_DEBUG + ", tag: " +tag + ", Num: "+num);
			}
			return false;
		}
		
		MRpcItem mi = null;
		if(JMicroContext.existRpcContext()) {
			mi = JMicroContext.get().getMRpcItem();
		}
		
		boolean f = false;
		if(mi == null) {
			mi = new MRpcItem();
			f = true;
		}
		
		OneItem oi = mi.addOneItem(type, tag);
		oi.setVal(num);
		if(f) {
			setCommon(mi);
			return m.submit2Cache(mi);
		}
		return true;
	}
	
	public static boolean limit(String tag) {
		return eventLog(MC.MT_SERVICE_SPEED_LIMIT,MC.LOG_WARN, tag,"");
	}
	
	public static void doServiceLog(short type,byte level,Class<?> cls,Throwable exp,String desc) {
		doLog(type,level,cls,null,exp,desc);
	}
	
	public static void doServiceLog(short type,byte level,Class<?> cls,String desc) {
		doLog(type,level,cls,null,null,desc);
	}

	public static void doBussinessLog(short type,byte level, Class<?> cls, Throwable exp,String desc) {
		doLog(type,level,cls,null,exp,desc);
	}
	
	public static void doRequestLog(short type,byte level,Class<?> cls,Throwable exp,String desc) {
		doLog(type,level,cls,null,exp,desc);
	}
	
	public static void doResponseLog(short type,byte level,Class<?> cls,Throwable exp,String desc) {
		doLog(type,level,cls,null,exp,desc);
	}
	
	public static void doMessageLog(short type,byte level,Class<?> cls,Message msg,Throwable exp,String desc) {
		doLog(type,level,cls,msg,exp,desc);
	}
	
	private static void doLog(short type,byte level,Class<?> cls,Message msg,Throwable exp, String desc) {
		if(isMonitorable(type)) {
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
			OneItem oi = mi.addOneItem(type,level, cls.getName(),desc);
			oi.setEx(exp);
			mi.setMsg(msg);
			if(f) {
				setCommon(mi);
				m.submit2Cache(mi);
			}
		}else {
			if(logger.isDebugEnabled()) {
				logger.debug("Disgard: type:" + MC.MONITOR_VAL_2_KEY.get(type) + ", level: " + MC.LOG_DEBUG + ", tag: " +cls.getName() + ", Desc: "+desc);
			}
			
			if(exp != null) {
				logger.error("",exp);
			}
		}
	}
	
	private static ServiceMethod sm() {
		if(JMicroContext.existRpcContext()) {
			return JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		}
		return null;
	}
	
	public static void setCommon(MRpcItem si) {
		if(si == null) {
			return;
		}

		if(JMicroContext.existRpcContext()) {
			si.setAct(JMicroContext.get().getAccount());
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
	
	private static boolean isMonitorable(short type) {
		
		if(!isInit) {
			isInit = true;
			isMs = JMicro.getObjectFactory().get(IMonitorServer.class) != null;
			m = JMicro.getObjectFactory().get(MonitorClient.class);
			isDs = JMicro.getObjectFactory().get(IMonitorDataSubscriber.class) != null;
		}
		
		if(JMicroContext.existRpcContext() && !JMicroContext.get().isMonitorable()
				|| m == null || !m.isServerReady()) {
			return false;
		}
		
		if((isDs || isMs) && JMicroContext.existLinkId()) {
			//avoid dead loop
			return false;
		}
		
		return m.canSubmit(sm(),type);
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
		if(!m.isServerReady() /*&& !mo.canSubmit(MonitorConstant.LINKER_ROUTER_MONITOR) */
				|| needLevel == MC.LOG_NO) {
			return false;
		}
		
		int rpcLevel;
		if(rpcMethodLevel == null || rpcMethodLevel.length == 0) {
			if(JMicroContext.existRpcContext()) {
				ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
				 if(sm != null) {
					 rpcLevel = sm.getLogLevel();
				 } else {
					 //默认错误日志强制监控
					 rpcLevel = MC.LOG_ERROR;
				 }
			} else {
				 //默认错误日志强制监控
				 rpcLevel = MC.LOG_ERROR;
			 }
		} else {
			rpcLevel = rpcMethodLevel[0];
		}
		 //如果级别大于或等于错误，不管RPC的配置如何，肯定需要日志
		return needLevel >= rpcLevel;
	}
	
}
