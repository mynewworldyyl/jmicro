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
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.EnterMain;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.choreography.ProcessInfo;
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
	
	//private static boolean isMs = false;
	
	private static ProcessInfo pi = null;
	
	private static JMLogItem beforeInitItem = null;
	
	public static boolean log(byte level,String tag,String desc,short type) {
		return log(level,tag,desc,null,type);
	}
	
	public static boolean log(byte level,Class<?> tag,String desc) {
		return log(level,tag.getName(),desc,null,MC.MT_DEFAULT);
	}
	
	public static boolean log(byte level,Class<?> tag,String desc,Throwable exp) {
		return log(level,tag.getName(),desc,exp,MC.MT_DEFAULT);
	}
	
	public static boolean log(byte level,String tag,short type) {
		return log(level,tag,"",null,type);
	}
	
	public static boolean log(byte level,String tag,String desc,Throwable exp,short type) {

		if(!isLoggable(level)) {
			return false;
		}

		JMLogItem mi = null;
		if(JMicroContext.existRpcContext()) {
			 mi = JMicroContext.get().getMRpcLogItem();
		}
		
		if(mi == null && !isInit) {
			if(beforeInitItem == null) {
				beforeInitItem = new JMLogItem();
			}
			mi = beforeInitItem;
		}
		
		boolean f = false;
		if(mi == null) {
			 f = true;
			 mi = new JMLogItem();
		}
		
		OneLog oi = mi.addOneItem(level, tag,desc);
		oi.setType(type);
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
					mi.setActClientId(ce.getAi().getId());
					mi.setSysClientId(Config.getClientId());
					mi.setActName(ce.getAi().getActName());
				}
			}
			oi.setEx(serialEx(exp));
		}
		
		if(f) {
			setCommon(mi);
			return m.readySubmit(mi);
		}
	
		return true;
	
	}
	
	public static boolean breakService(byte level,String tag,ServiceMethod sm,String desc) {
		if(isLoggable(level,sm.getLogLevel())) {
			JMLogItem mi = new JMLogItem();
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
		
		JMLogItem mi = null;
		if(JMicroContext.existRpcContext()) {
			mi = JMicroContext.get().getMRpcLogItem();
		}
		
		if(mi == null && !isInit) {
			if(beforeInitItem == null) {
				beforeInitItem = new JMLogItem();
			}
			mi = beforeInitItem;
		}
		
		boolean f = false;
		if(mi == null) {
			mi = new JMLogItem();
			f = true;
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai != null) {
				mi.setActClientId(ai.getId());
				mi.setSysClientId(Config.getClientId());
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
	
	public static JMLogItem logWithNonRpcContext(byte level, Class<?> tag, String desc,short type,boolean submit) {
		return logWithNonRpcContext(level,tag.getName(),desc,null,type,submit);
	}
	
	public static JMLogItem logWithNonRpcContext(byte level, Class<?> tag, String desc, Throwable exp,boolean submit) {
		return logWithNonRpcContext(level,tag.getName(),desc,exp,MC.MT_DEFAULT,submit);
	}
	
	public static JMLogItem logWithNonRpcContext(byte level, String tag, String desc, Throwable exp,short type,boolean submit) {
		if(level == MC.LOG_NO || !isLoggable(level)) {
			return null;
		}
		
		JMLogItem mi = null;
		
		if(!isInit) {
			if(beforeInitItem == null) {
				beforeInitItem = new JMLogItem();
			}
			mi = beforeInitItem;
		}else {
			mi = new JMLogItem();
		}

		OneLog oi = mi.addOneItem(level, tag,desc);
		oi.setType(type);
		setStackTrance(oi,4);
		if(exp != null) {
			oi.setEx(serialEx(exp));
		}

		setCommon(mi);
	
		if(submit) {
			submit2Cache(mi);
		}
		
		return mi;
	}
	
	public static void submit2Cache(JMLogItem mi) {
		if(isInit) {
			m.submit2Cache(mi);
		}
	}
	
	public static void setCommon(JMLogItem si) {
		if(si == null) {
			return;
		}

		if(JMicroContext.existRpcContext()) {
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai != null) {
				si.setActClientId(ai.getId());
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
		
		si.setSysClientId(Config.getClientId());
		si.setLocalHost(Config.getExportSocketHost());
		si.setInstanceName(Config.getInstanceName());
	}
	
	
	public static void initLog() {
		if(isInit) return;

		isInit = true;
		m = EnterMain.getObjectFactory().get(LogMonitorClient.class);
		//isMs = m != null;
		pi = EnterMain.getObjectFactory().get(ProcessInfo.class);
		if(beforeInitItem != null) {
			Iterator<OneLog> items = beforeInitItem.getItems().iterator();
			while(items.hasNext()) {
				OneLog i = items.next();
				if(!isLoggable(i.getLevel())) {
					items.remove();
				}
			}
			if(beforeInitItem.getItems().size() > 0) {
				m.readySubmit(beforeInitItem);
			}
			beforeInitItem = null;
		}
	
	}
	
	/**
	 * 	日志输出4个条件
	 * 1. 对应组件打开debug模式，isDebug=true 或者服务方法loggable=true;
	 * 2. 日志级别打开
	 * 
	 * @param isComOpen
	 * @param level
	 * @return
	 */
	public static boolean isLoggable(int needLevel, int ...rpcMethodLevel) {
		
		if(!isInit) {
			//logger.warn("Log not init");
			return true;
		}
		
		byte rpcLevel = pi.getLogLevel();
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
			ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
			if(sm != null) {
				sb.append(",sn:").append(sm.getKey().getServiceName());
				sb.append(",ns:").append(sm.getKey().getNamespace());
				sb.append(",ver:").append(sm.getKey().getVersion());
				sb.append(",method:").append(sm.getKey().getMethod());
			}
			sb.append(",params:").append(req.getArgs());
		}
		return sb.toString();
	}
	
	public static String respMessage(String prefix, IResp r) {
		StringBuffer sb = new StringBuffer(prefix);
		if(r instanceof RpcResponse) {
			RpcResponse req = (RpcResponse)r;
			sb.append(",success:").append(req.isSuccess());
			sb.append(",result:").append(req.getResult());
		}else if(r instanceof ApiResponse) {
			ApiResponse req = (ApiResponse)r;
			sb.append(",success:").append(req.isSuccess());
			sb.append(",reqId:").append(req.getReqId());
			sb.append(",result:").append(req.getResult());
		}
		return sb.toString();
	}
	
	private static void setStackTrance(OneLog oi,int idx) {
		StackTraceElement se = Thread.currentThread().getStackTrace()[idx];
		oi.setLineNo(se.getLineNumber());
		oi.setFileName(se.getFileName());
	}
}
