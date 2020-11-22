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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.gateway.ApiResponse;
import cn.jmicro.api.net.IReq;
import cn.jmicro.api.net.IResp;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.net.RpcResponse;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午9:34:45
 */
public class LG {
	
	private final static Logger logger = LoggerFactory.getLogger(LG.class);
	
	private static LogMonitorClient m = null;
	
	private static boolean isInit = false;
	
	private static boolean isMs = false;
	
	//private static boolean isDs = false;
	
	//public static byte SYSTEM_LOG_LEVEL = MC.LOG_INFO;
	
	public static boolean respEvent(byte level,IResp resp,String tag) {
		if(isLoggable(level)) {
			MRpcLogItem mi = JMicroContext.get().getMRpcLogItem();
			mi.setResp(resp);
			OneLog oi = mi.addOneItem(level,tag,"");
			setStackTrance(oi,3);
			return true;
		}
		return false;
	} 
	
	public static boolean reqEvent(byte level,IReq req,String tag,String desc) {
		if(isLoggable(level)) {
			MRpcLogItem mi = JMicroContext.get().getMRpcLogItem();
			mi.setReq(req);
			OneLog oi = mi.addOneItem(level,tag,desc);
			setStackTrance(oi,3);
			return true;
		}
		return false;
	} 
	
	public static boolean log(byte level,Class<?> tag,String desc) {
		return log(level,tag.getName(),desc,null);
	}
	
	public static boolean log(byte level,Class<?> tag,String desc,Throwable exp) {
		return log(level,tag.getName(),desc,exp);
	}
	
	private static void setStackTrance(OneLog oi,int idx) {
		StackTraceElement se = Thread.currentThread().getStackTrace()[idx];
		oi.setLineNo(se.getLineNumber());
		oi.setFileName(se.getFileName());
	}
	
	public static boolean log(byte level,String tag,String desc,Throwable exp) {

		if(isLoggable(level)) {
			MRpcLogItem mi = null;
			if(JMicroContext.existRpcContext()) {
				 mi = JMicroContext.get().getMRpcLogItem();
			}
			
			boolean f = false;
			if(mi == null) {
				 f = true;
				 mi = new MRpcLogItem();
			}
			
			OneLog oi = mi.addOneItem(level, tag,desc);
			setStackTrance(oi,4);
			
			if(exp != null) {
				if(exp instanceof CommonException) {
					CommonException ce = (CommonException)exp;
					if(ce.getReq() != null) {
						mi.setReq(ce.getReq());
					}
					if(ce.getResp() != null) {
						mi.setResp(ce.getResp());
					}
					if(ce.getAi() != null) {
						mi.setClientId(ce.getAi().getClientId());
						mi.setActName(ce.getAi().getActName());
					}
				}
				oi.setEx(serialEx(exp));
			}
			
			if(f) {
				setCommon(mi);
				return m.readySubmit(mi);
			}
		}
		return false;
	
	}
	
	public static boolean breakService(byte level,String tag,ServiceMethod sm,String desc) {
		if(isLoggable(level,sm.getLogLevel())) {
			MRpcLogItem mi = new MRpcLogItem();
			mi.setSmKey(sm.getKey());
			OneLog oi = mi.addOneItem(level, tag, desc);
			setStackTrance(oi,3);
			setCommon(mi);
			m.readySubmit(mi);
			return true;
		}
		return false;
	}
	
	public static boolean netIo(byte level,Class<?> cls,String desc,Throwable ex) {
		if(!isLoggable(level)) {
			return false;
		}
		
		MRpcLogItem mi = null;
		if(JMicroContext.existRpcContext()) {
			mi = JMicroContext.get().getMRpcLogItem();
		}
		
		boolean f = false;
		if(mi == null) {
			mi = new MRpcLogItem();
			f = true;
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai != null) {
				mi.setClientId(ai.getClientId());
			}
		}
		
		OneLog oi = mi.addOneItem(level,cls.getName(),desc);
		setStackTrance(oi,3);
		if(ex != null) {
			oi.setEx(serialEx(ex));
		}
		
		if(f) {
			setCommon(mi);
			return m.submit2Cache(mi);
		}
		return false;
		
	}
	
	public static String serialEx(Throwable ex) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ex.printStackTrace(new PrintStream(baos,true,Constants.CHARSET));
			return baos.toString(Constants.CHARSET);
		} catch (UnsupportedEncodingException e) {
			logger.error("",ex);
			logger.error("",e);
		}
		return ex.getMessage();
	}
	
	public static void logWithNonRpcContext(byte level, Class<?> tag, String desc) {
		logWithNonRpcContext(level,tag.getName(),desc,null);
	}
	
	public static void logWithNonRpcContext(byte level, Class<?> tag, String desc, Throwable exp) {
		logWithNonRpcContext(level,tag.getName(),desc,exp);
	}
	
	public static void logWithNonRpcContext(byte level, String tag, String desc, Throwable exp) {
		if(level == MC.LOG_NO || level < Config.getSystemLogLevel()) {
			return;
		}

		MRpcLogItem mi = new MRpcLogItem();
		OneLog oi = mi.addOneItem(level, tag,desc);
		setStackTrance(oi,4);
		if(exp != null) {
			oi.setEx(serialEx(exp));
		}

		setCommon(mi);
		m.submit2Cache(mi);
	
	}
	
	public static void setCommon(MRpcLogItem si) {
		if(si == null) {
			return;
		}

		if(JMicroContext.existRpcContext()) {
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai != null) {
				si.setClientId(ai.getClientId());
				si.setActName(ai.getActName());
			}
			
			//在RPC上下文中才有以上信息
			si.setLinkId(JMicroContext.lid());
			si.setLocalPort(JMicroContext.get().getString(JMicroContext.LOCAL_PORT, ""));
			si.setRemoteHost(JMicroContext.get().getString(JMicroContext.REMOTE_HOST, ""));
			si.setRemotePort(JMicroContext.get().getString(JMicroContext.REMOTE_PORT, ""));
			
			ServiceMethod sm = (ServiceMethod)JMicroContext.get().getObject(Constants.SERVICE_METHOD_KEY, null);
			if(si.getSmKey() == null && sm != null) {
				si.setSmKey(sm.getKey());
				si.setLogLevel(sm.getLogLevel());
			}
		}
		
		si.setLocalHost(Config.getExportSocketHost());
		si.setInstanceName(Config.getInstanceName());
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
	public static boolean isLoggable(int needLevel, int ...rpcMethodLevel) {
		
		if(!isInit) {
			isInit = true;
			m = JMicro.getObjectFactory().get(LogMonitorClient.class);
			isMs = m != null;
		}
		
		if(!isMs || !m.isServerReady() || needLevel == MC.LOG_NO) {
			return false;
		}
		
		byte rpcLevel = Config.getSystemLogLevel();
		if(rpcMethodLevel == null || rpcMethodLevel.length == 0) {
			if(JMicroContext.existRpcContext()) {
				rpcLevel = JMicroContext.get().getParam(JMicroContext.SM_LOG_LEVEL, rpcLevel);
			}
		} else {
			rpcLevel = (byte)rpcMethodLevel[0];
		}
		 //如果级别大于或等于错误，不管RPC的配置如何，肯定需要日志
		return rpcLevel != MC.LOG_NO && needLevel >= rpcLevel;
	}
	
	
	public static final String messageLog(String prefixMsg,Message msg) {
		StringBuffer sb = new StringBuffer(prefixMsg);
		sb.append(",msgType:").append(msg.getType());
		sb.append(",msdId:").append(msg.getId());
		sb.append(",reqId:").append(msg.getReqId());
		sb.append(",linkId:").append(msg.getLinkId());
		sb.append(",instanceName:").append(msg.getInsId());
		sb.append(",method:").append(msg.getMethod());
		if(msg.getTime() > 0) {
			sb.append(",cost:").append(TimeUtils.getCurTime() - msg.getTime());
		}
		sb.append(",flag:0X").append(Integer.toHexString(msg.getFlag()));
		return sb.toString();
	}

	public static String reqMessage(String prefix, IReq r) {
		StringBuffer sb = new StringBuffer(prefix);
		if(r instanceof RpcRequest) {
			RpcRequest req = (RpcRequest)r;
			sb.append(",sn:").append(req.getServiceName());
			sb.append(",ns:").append(req.getNamespace());
			sb.append(",ver:").append(req.getVersion());
			sb.append(",method:").append(req.getMethod());
			sb.append(",params:").append(req.getArgs());
			sb.append(",reqId:").append(req.getRequestId());
			sb.append(",parentId:").append(req.getReqParentId());
		}else if(r instanceof ApiRequest) {
			ApiRequest req = (ApiRequest)r;
			sb.append(",sn:").append(req.getServiceName());
			sb.append(",ns:").append(req.getNamespace());
			sb.append(",ver:").append(req.getVersion());
			sb.append(",method:").append(req.getMethod());
			sb.append(",params:").append(req.getArgs());
		}
		return sb.toString();
	}
	
	public static String respMessage(String prefix, IResp r) {
		StringBuffer sb = new StringBuffer(prefix);
		if(r instanceof RpcResponse) {
			RpcResponse req = (RpcResponse)r;
			sb.append(",success:").append(req.isSuccess());
			sb.append(",reqId:").append(req.getRequestId());
			sb.append(",result:").append(req.getResult());
		}else if(r instanceof ApiResponse) {
			ApiResponse req = (ApiResponse)r;
			sb.append(",success:").append(req.isSuccess());
			sb.append(",reqId:").append(req.getReqId());
			sb.append(",result:").append(req.getResult());
		}
		return sb.toString();
	}
	
}
