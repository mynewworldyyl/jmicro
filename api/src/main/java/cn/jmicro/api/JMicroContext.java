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
package cn.jmicro.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.Linker;
import cn.jmicro.api.monitor.LogMonitorClient;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcLogItem;
import cn.jmicro.api.monitor.MRpcStatisItem;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:55:28
 */
public class JMicroContext  {

	static final Logger logger = LoggerFactory.getLogger(JMicroContext.class);
	
	//value should be true or false, for provider or consumer side
	public static final String CALL_SIDE_PROVIDER = "_callSideProvider";
	
	public static final String LOCAL_HOST = "_host";
	public static final String LOCAL_PORT = "_port";
	
	public static final String REMOTE_HOST = "_remoteHost";
	public static final String REMOTE_PORT = "_remotePort";
	
	public static final String LOGIN_ACT = "_loginAccount";
	
	public static final String LOGIN_KEY = "loginKey";
	
	public static final String SM_LOG_LEVEL = "__smLogLevel";
	
	public static final String CACHE_LOGIN_KEY = "__ActLoginKey_";
	
	
	public static final String LINKER_ID = "_linkerId";
	public static final String REQ_PARENT_ID = "_reqParentId";
	//控制RPC方法在每个服务中输出日志，区加于往监控服务器上传日志
	public static final String IS_DEBUG = "_isDebug";
	public static final String IS_MONITORENABLE = "_monitorEnable";
	public static final String REQ_ID = "_reqId";
	public static final String MSG_ID = "_msgId";
	
	//public static final String MONITOR = "monitor";
	
	public static final String CLIENT_SERVICE = "clientService";
	public static final String CLIENT_NAMESPACE = "clientNamespace";
	public static final String CLIENT_VERSION = "clientVersion";
	public static final String CLIENT_METHOD = "clientMehtod";
	public static final String CLIENT_ARGSTR= "argStr";
	
	public static final String MRPC_LOG_ITEM = "_mrpc_log_item";
	public static final String CLIENT_UP_TIME = "_client_up_time";
	public static final String SERVER_GOT_TIME = "_server_got_time";
	public static final String DEBUG_LOG = "_debug_loggner";
	
	public static final String MRPC_STATIS_ITEM = "_mrpc_statis_item";
	
	public static final String SESSION_KEY="_sessionKey";
	private static final ThreadLocal<JMicroContext> cxt = new ThreadLocal<JMicroContext>();
	
	private Stack<Map<String,Object>> ctxes = new Stack<>();
	
	protected Map<String,Object> curCxt = new HashMap<String,Object>();
	
	private JMicroContext() {}
	
	public MRpcLogItem getMRpcLogItem() {
		//使用者需要调用isMonitor()或isDebug()判断是否可用状态
		return this.getParam(MRPC_LOG_ITEM, null);
	}
	
	public MRpcStatisItem getMRpcStatisItem() {
		return this.getParam(MRPC_STATIS_ITEM, null);
	}
	
	public void submitMRpcItem(LogMonitorClient mo,StatisMonitorClient smc) {
		
		MRpcLogItem item = getMRpcLogItem();
		if(item != null && item.getItems() != null && item.getItems().size() > 0 ) {
			if(StringUtils.isEmpty(item.getActName())) {
				ActInfo ai = this.getAccount();
				if(ai != null) {
					item.setClientId(ai.getClientId());
					ai.setActName(ai.getActName());
				}
			}
			LG.setCommon(item);
			item.setCostTime(TimeUtils.getCurTime() - item.getCreateTime());
			mo.readySubmit(item);
			JMicroContext.get().removeParam(MRPC_LOG_ITEM);
		}
	
		
		if(this.isMonitorable()) {
			MRpcStatisItem sItem = getMRpcStatisItem();
			if(sItem != null && sItem.getTypeStatis() != null 
					&& !sItem.getTypeStatis().isEmpty()) {
				if(StringUtils.isEmpty(item.getActName())) {
					ActInfo ai = this.getAccount();
					if(ai != null) {
						item.setClientId(ai.getClientId());
						ai.setActName(ai.getActName());
					}
				}
				MT.setCommon(sItem);
				sItem.setCostTime(TimeUtils.getCurTime() - sItem.getCreateTime());
				smc.readySubmit(sItem);
				JMicroContext.get().removeParam(MRPC_STATIS_ITEM);
			}
		}
	}
	
	public static void remove(){
		JMicroContext c = cxt.get();
		if(c != null) {
			cxt.remove();
		}
	}
	
	public static boolean existRpcContext() {
		return cxt.get() != null && (cxt.get().exists(JMicroContext.REQ_ID) 
				|| cxt.get().exists(JMicroContext.LINKER_ID));
	}
	
	public static JMicroContext get(){
		JMicroContext c = cxt.get();
		if(c != null) {
			return c;
		}
		synchronized(JMicroContext.class) {
			 c = cxt.get();
			if(c == null) {
				c = new JMicroContext();
				cxt.set(c);
			}
		}
		return c;
	}
	
	public static void clear(){
		cxt.set(null);
	}
	
	public static void setCallSide(Boolean flag){
		get().setBoolean(JMicroContext.CALL_SIDE_PROVIDER, flag);
	}
	
	public static boolean isContainCallSide(){
		return get().exists(JMicroContext.CALL_SIDE_PROVIDER);
	}
	
	public static boolean isCallSideService(){
		if(isContainCallSide()) {
			return get().getBoolean(JMicroContext.CALL_SIDE_PROVIDER, false);
		}
		throw new CommonException("Non RPC Context!");
	}
	
	public static boolean isCallSideClient(){
		return !isCallSideService();
	}
	
	public static void configProvider(ISession s,Message msg) {
		setCallSide(true);
		
		JMicroContext context = get();
		
		context.setParam(JMicroContext.SESSION_KEY, s);
			
		context.setParam(JMicroContext.REMOTE_HOST, s.remoteHost());
		context.setParam(JMicroContext.REMOTE_PORT, s.remotePort()+"");
		
		context.setParam(JMicroContext.LOCAL_HOST, s.localHost());
		context.setParam(JMicroContext.LOCAL_PORT, s.localPort()+"");
		
		context.setParam(JMicroContext.LINKER_ID, msg.getLinkId());
		context.setParam(Constants.NEW_LINKID, false);
		
		context.setParam(JMicroContext.SM_LOG_LEVEL, msg.getLogLevel());
		
		//context.isLoggable = msg.isLoggable();
		//debug mode 下才有效
		context.setParam(IS_DEBUG, msg.isDebugMode());
		if(msg.isDebugMode()) {
			//long clientTime = msg.getTime();
			StringBuilder sb = new StringBuilder();
			long curTime = TimeUtils.getCurTime();
			sb.append("Provider, Client to server cost: ").append(curTime-msg.getTime()).append(", ");
			context.setParam(CLIENT_UP_TIME, msg.getTime());
			context.setParam(SERVER_GOT_TIME, curTime);
			context.setParam(DEBUG_LOG, sb);
		}
		
		boolean iMonitorable = msg.isMonitorable();
		context.setParam(IS_MONITORENABLE, iMonitorable);
		if(iMonitorable) {
			initMrpcStatisItem();
		}
		
		if(msg.getLogLevel() != MC.LOG_NO) {
			initMrpcLogItem();
		}
	}
	
	private static void initMrpcStatisItem() {
		JMicroContext context = cxt.get();
		MRpcStatisItem item = context.getMRpcStatisItem();
		if(item == null) {
			synchronized(MRpcStatisItem.class) {
				item = context.getMRpcStatisItem();
				if(item == null) {
					item = new MRpcStatisItem();
					ActInfo ai = context.getAccount();
					if(ai != null) {
						item.setClientId(ai.getClientId());
						item.setActName(ai.getActName());
					}
					//the pre RPC Request ID as the parent ID of this request
					context.setParam(MRPC_STATIS_ITEM, item);
				}
			}
		}
	
	}
	
	private static void initMrpcLogItem() {
		
		JMicroContext context = cxt.get();
		MRpcLogItem item = context.getMRpcLogItem();
		if(item == null) {
			synchronized(MRpcLogItem.class) {
				item = context.getMRpcLogItem();
				if(item == null) {
					item = new MRpcLogItem();
					ActInfo ai = context.getAccount();
					if(ai != null) {
						item.setClientId(ai.getClientId());
						item.setActName(ai.getActName());
					}
					//the pre RPC Request ID as the parent ID of this request
					context.setParam(MRPC_LOG_ITEM, item);
					
					
				}
			}
		}
		
		item.setProvider(false);
		item.setReqParentId(context.getLong(REQ_PARENT_ID, 0L));
	}
	
	public static boolean enableOrDisable(int siCfg,int smCfg) {
		return smCfg == 1 ? true: (smCfg == 0 ? false:(siCfg == 1 ? true:false));
	}
	
	public static void configComsumer(ServiceMethod sm,ServiceItem si) {
		JMicroContext context = cxt.get();
		setCallSide(false);
		//debug mode 下才有效
		boolean isDebug = enableOrDisable(si.getDebugMode(),sm.getDebugMode());
		context.setParam(IS_DEBUG, isDebug);
		if(isDebug) {
			context.setParam(CLIENT_UP_TIME, TimeUtils.getCurTime());
			context.setParam(DEBUG_LOG, new StringBuilder("Comsumer "));
		}
		
		boolean iMonitorable = enableOrDisable(si.getMonitorEnable(),sm.getMonitorEnable());
		context.setParam(IS_MONITORENABLE, iMonitorable);
		if(iMonitorable) {
			initMrpcStatisItem() ;
		}
		
		if(sm.getLogLevel() != MC.LOG_NO) {
			initMrpcLogItem();
			MRpcLogItem mi = context.getMRpcLogItem();
			if(mi != null) {
				mi.setImplCls(si.getImpl());
				mi.setSmKey(sm.getKey());
				ActInfo ai = context.getAccount();
				if(ai != null) {
					mi.setActName(ai.getActName());
					mi.setClientId(ai.getClientId());
				}
			}
		}
		
	}
	
	
	public static void config(IRequest req, ServiceLoader serviceLoader,IRegistry registry) {
		
		Object obj = serviceLoader.getService(Integer.parseInt(req.getImpl()));
		if(obj == null){
			LG.log(MC.LOG_ERROR,JMicroContext.class," service INSTANCE not found");
			MT.nonRpcEvent(Config.getInstanceName(), MC.MT_PLATFORM_LOG);
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found,srv: "+req.getImpl());
		}
		
		JMicroContext context = cxt.get();
		context.setString(JMicroContext.CLIENT_SERVICE, req.getServiceName());
		context.setString(JMicroContext.CLIENT_NAMESPACE, req.getNamespace());
		context.setString(JMicroContext.CLIENT_METHOD, req.getMethod());
		context.setString(JMicroContext.CLIENT_VERSION, req.getVersion());
		//context.setLong(JMicroContext.REQ_PARENT_ID, req.getRequestId());
		context.setParam(JMicroContext.REQ_ID, req.getRequestId());
		
		context.setParam(JMicroContext.CLIENT_ARGSTR, UniqueServiceMethodKey.paramsStr(req.getArgs()));
		context.mergeParams(req.getRequestParams());
		
		ServiceItem si = registry.getOwnItem(Integer.parseInt(req.getImpl()));
		if(si == null){
			if(LG.isLoggable(MC.LOG_ERROR,req.getLogLevel())) {
				LG.log(MC.LOG_ERROR,JMicroContext.class," service ITEM not found");
				MT.nonRpcEvent(Config.getInstanceName(), MC.MT_SERVICE_ITEM_NOT_FOUND);
			}
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found impl："+req.getImpl()+", srv: " + req.getServiceName());
		}
		
		ServiceMethod sm = si.getMethod(req.getMethod(), req.getArgs());
		context.setObject(Constants.SERVICE_ITEM_KEY, si);
		context.setObject(Constants.SERVICE_METHOD_KEY, sm);
		context.setObject(Constants.SERVICE_OBJ_KEY, obj);
		
		MRpcLogItem mi = context.getMRpcLogItem();
		
		if( mi != null) {
			mi.setReqParentId(req.getReqParentId());
			mi.setReqId(req.getRequestId());
			mi.setReq(req);
			mi.setImplCls(si.getImpl());
			mi.setSmKey(sm.getKey());
		}
		
	}
	
	public static boolean existLinkId(){
		return get().exists(LINKER_ID);
	}
	
	public static Long lid(){
		JMicroContext c = get();
		Long id = c.getLong(LINKER_ID, null);
		if(id != null) {
			return id;
		}
		ComponentIdServer idGenerator = JMicro.getObjectFactory().get(ComponentIdServer.class);
		if(idGenerator != null) {
			id = idGenerator.getLongId(Linker.class);
			c.setLong(LINKER_ID, id);
		}
		return id;
	}
	
	public boolean isAsync() {
		if(isContainCallSide() && isCallSideService()) {
			return this.exists(Constants.CONTEXT_SERVICE_RESPONSE);
		} else {
			return this.exists(Constants.CONTEXT_CALLBACK_CLIENT);
		}
	}
	
	/**
	 * 同一个线程多个RPC之间上下文切换
	 */
	public void backupAndClear() {
		Map<String,Object> ps = new HashMap<>();
		ps.putAll(cxt.get().curCxt);
		ctxes.push(ps);
		cxt.get().curCxt.clear();
	}
	
	public ActInfo getAccount() {
		 return JMicroContext.get().getParam(JMicroContext.LOGIN_ACT, null);
	}
	
	public void setAccount(ActInfo act) {
		 JMicroContext.get().setParam(JMicroContext.LOGIN_ACT, act);
	}
	
	public boolean hasPermission(int reqLevel) {
		 ActInfo ai = getAccount();
		 if(ai != null) {
			return  true/*ai.getClientId() <= reqLevel*/;
		 }
		 return false;
	}
	
	public boolean hasPermission(int reqLevel, int defaultLevel) {
		 ActInfo ai =  getAccount();
		 if(ai != null) {
			return ai.getClientId() <= reqLevel;
		 } else {
			 return defaultLevel <= reqLevel;
		 }
	}
	
	public void restore() {
		cxt.get().curCxt.clear();
		if(ctxes.isEmpty()) {
			throw new CommonException("JMicro Context stack is empty");
		}
		Map<String,Object> ps = ctxes.pop();
		cxt.get().curCxt.putAll(ps);
	}
	
	public void getAllParams(Map<String,Object> params) {
		params.putAll(this.curCxt);
	}
	
	public boolean isDebug(){
		return this.getBoolean(IS_DEBUG, false);
	}
	
	public boolean isMonitorable(){
		return this.getBoolean(IS_MONITORENABLE, false);
	}
	
	//debug mode 下才有效
	public StringBuilder getDebugLog() {
		return this.getParam(DEBUG_LOG, null);
	}
	
	//debug mode 下才有效
	public void appendCurUseTime(String label,boolean force) {
		if(isDebug()) {
			ServiceMethod sm = this.getParam(Constants.SERVICE_METHOD_KEY, null);
			if(sm != null) {
				long curTime = TimeUtils.getCurTime();
				long cost = curTime - this.getLong(CLIENT_UP_TIME, curTime);
				if(force || cost > sm.getTimeout()-100) {
					//超时的请求才记录下来
					StringBuilder sb = this.getDebugLog();
					sb.append(",").append(label).append(": ").append(cost);
				}
			}
		}
	}
	
	public void debugLog(long timeout) {
		if(!this.isDebug()) {
			return;
		}

		StringBuilder log = this.getDebugLog();
		this.removeParam(DEBUG_LOG);
		long curTime = TimeUtils.getCurTime();
		long cost = curTime - this.getLong(CLIENT_UP_TIME, curTime);
		if(timeout > 0) {
			if(cost > timeout) {
				//超时的请求才记录下来
				log.append(", cost expect :").append(timeout).append(" : ").append(cost);
				logger.warn(log.toString());
			}
		} else {
			ServiceMethod sm = this.getParam(Constants.SERVICE_METHOD_KEY, null);
			if(sm != null) {
				if(cost > sm.getTimeout()-100) {
					//超时的请求才记录下来
					log.append(", maybe timeout expect :").append((sm.getTimeout()-100)).append(" : ").append(cost);
					logger.warn(log.toString());
				}
			} else {
				//超时的请求才记录下来
				log.append(", with ServiceMethod is NULL :");
				logger.warn(log.toString());
			}
		}			
	}
	

	public void mergeParams(Map<String,Object> params){
		if(params == null || params.isEmpty()) {
			return;
		}
		for(Map.Entry<String, Object> p : params.entrySet()){
			this.curCxt.put(p.getKey(), p.getValue());
		}
	}
	
	public boolean exists(String key){
		return this.curCxt.containsKey(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key,T defautl){
		T v = (T)this.curCxt.get(key);
		if(v == null){
			return defautl;
		}
		return v;
	}
	
	public void removeParam(String key){
	    this.curCxt.remove(key);
	}
	
	public <T> void setParam(String key,T val){
		this.curCxt.put(key,val);
	}
	
	public void setInt(String key,int defautl){
	    this.setParam(key,defautl);
	}
	
	public void setString(String key,String val){
		 this.setParam(key,val);
	}
	
	public void setBoolean(String key,boolean val){
		 this.setParam(key,val);
	}
	
	
	public void setFloat(String key,Float val){
		 this.setParam(key,val);
	}
	
	public void setDouble(String key,Double val){
		 this.setParam(key,val);
	}
	
	public void setLong(String key,Long val){
		 this.setParam(key,val);
	}
	
	public void setObject(String key,Object val){
		 this.setParam(key,val);
	}
	
	public Integer getInt(String key,int defautl){
		return this.getParam(key,defautl);
	}
	
	public Long getLong(String key,Long defautl){
		return this.getParam(key,defautl);
	}
	
	public String getString(String key,String defautl){
		return this.getParam(key,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return this.getParam(key,defautl);
	}
	
	public Float getFloat(String key,Float defautl){
		return this.getParam(key,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return this.getParam(key,defautl);
	}
	
	public Object getObject(String key,Object defautl){
		return this.getParam(key,defautl);
	}
}
