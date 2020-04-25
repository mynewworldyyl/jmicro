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
package org.jmicro.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.jmicro.api.config.Config;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.monitor.v1.Linker;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.api.monitor.v2.MRpcItem;
import org.jmicro.api.monitor.v2.MonitorManager;
import org.jmicro.api.monitor.v2.OneItem;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceLoader;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	public static final String REMOTE_HOST = "remoteHost";
	public static final String REMOTE_PORT = "_remotePort";
	
	public static final String LOGIN_ACT = "_loginAccount";
	
	public static final String LOGIN_KEY = "loginKey";
	
	public static final String CACHE_LOGIN_KEY = "__ActLoginKey_";
	
	
	public static final String LINKER_ID = "_linkerId";
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
	
	public static final String MRPC_ITEM = "_mrpc_item";
	public static final String CLIENT_UP_TIME = "_client_up_time";
	public static final String DEBUG_LOG = "_debug_loggner";
	
	public static String[] args = {};
	
	public static final String SESSION_KEY="_sessionKey";
	private static final ThreadLocal<JMicroContext> cxt = new ThreadLocal<JMicroContext>();
	
	private Stack<Map<String,Object>> stack = new Stack<>();
	
	protected Map<String,Object> params = new HashMap<String,Object>();
	
	private JMicroContext() {}
	
	public MRpcItem getMRpcItem() {
		//使用者需要调用isMonitor()或isDebug()判断是否可用状态
		return this.getParam(MRPC_ITEM, null);
	}
	
	public void submitMRpcItem(MonitorManager mo) {
		if(this.isMonitorable()) {
			MRpcItem item = getMRpcItem();
			if(item != null ) {
				SF.setCommon(item);
				mo.readySubmit(item);
				JMicroContext.get().removeParam(MRPC_ITEM);
			}
		}
	}
	
	public static void remove(){
		JMicroContext c = cxt.get();
		if(c != null) {
			cxt.remove();
		}
	}
	
	public static boolean existsContext() {
		return cxt.get() != null;
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
	
	public static boolean callSideProdiver(Boolean ... flag){
		if(flag == null || flag.length == 0) {
			return get().getBoolean(JMicroContext.CALL_SIDE_PROVIDER, true);
		} else {
			get().setBoolean(JMicroContext.CALL_SIDE_PROVIDER, flag[0]);
		}
		return flag[0];
	}
	
	public static void configProvider(ISession s,Message msg) {
		callSideProdiver(true);
		
		JMicroContext context = get();
		
		context.setParam(JMicroContext.SESSION_KEY, s);
			
		context.setParam(JMicroContext.REMOTE_HOST, s.remoteHost());
		context.setParam(JMicroContext.REMOTE_PORT, s.remotePort()+"");
		
		context.setParam(JMicroContext.LOCAL_HOST, s.localHost());
		context.setParam(JMicroContext.LOCAL_PORT, s.localPort()+"");
		
		context.setParam(JMicroContext.LINKER_ID, msg.getLinkId());
		context.setParam(Constants.NEW_LINKID, false);
		//context.isLoggable = msg.isLoggable();
		//debug mode 下才有效
		context.setParam(IS_DEBUG, msg.isDebugMode());
		if(msg.isDebugMode()) {
			context.setParam(CLIENT_UP_TIME, msg.getTime());
			context.setParam(DEBUG_LOG, new StringBuilder());
		}
		
		boolean iMonitorable = msg.isMonitorable();
		context.setParam(IS_MONITORENABLE, iMonitorable);
		if(iMonitorable) {
			MRpcItem item = context.getMRpcItem();
			if(item == null) {
				item = new MRpcItem();
				item.setReqId(msg.getReqId());
				item.setLinkId(msg.getLinkId());
				item.setMsg(msg);
				context.setParam(MRPC_ITEM, item);
			}
		}
	}
	
	public static boolean enableOrDisable(int siCfg,int smCfg) {
		return smCfg == 1 ? true: (smCfg == 0 ? false:(siCfg == 1 ? true:false));
	}
	
	public static void configComsumer(ServiceMethod sm,ServiceItem si) {
		JMicroContext context = cxt.get();
		//context.setObject(JMicroContext.MONITOR, JMicro.getObjectFactory().get(MonitorManager.class));
		context.setParam(Constants.SERVICE_METHOD_KEY, sm);
		context.setParam(Constants.SERVICE_ITEM_KEY, si);
		context.setParam(JMicroContext.LOCAL_HOST, Config.getHost());
		
		//debug mode 下才有效
		boolean isDebug = enableOrDisable(si.getDebugMode(),sm.getDebugMode());
		context.setParam(IS_DEBUG, isDebug);
		if(isDebug) {
			context.setParam(CLIENT_UP_TIME, System.currentTimeMillis());
			context.setParam(DEBUG_LOG, new StringBuilder());
		}
		
		boolean iMonitorable = enableOrDisable(si.getMonitorEnable(),sm.getMonitorEnable());
		context.setParam(IS_MONITORENABLE, iMonitorable);
		if(iMonitorable) {
			MRpcItem item = context.getMRpcItem();
			if(item == null) {
				synchronized(MRpcItem.class) {
					item = context.getMRpcItem();
					if(item == null) {
						item = new MRpcItem();
						context.setParam(MRPC_ITEM, item);
					}
				}
			}
		}
		
	}
	
	
	public static void config(IRequest req, ServiceLoader serviceLoader,IRegistry registry) {
		JMicroContext context = cxt.get();
		context.setString(JMicroContext.CLIENT_SERVICE, req.getServiceName());
		context.setString(JMicroContext.CLIENT_NAMESPACE, req.getNamespace());
		context.setString(JMicroContext.CLIENT_METHOD, req.getMethod());
		context.setString(JMicroContext.CLIENT_VERSION, req.getVersion());
		context.setParam(JMicroContext.CLIENT_ARGSTR, UniqueServiceMethodKey.paramsStr(req.getArgs()));
		context.mergeParams(req.getRequestParams());
		
		ServiceItem si = registry.getServiceByCode(Integer.parseInt(req.getImpl()));
		if(si == null){
			if(SF.isLoggable(req.getLogLevel())) {
				SF.doRequestLog(MonitorConstant.LOG_ERROR,JMicroContext.class,null," service ITEM not found");
			}
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found impl："+req.getImpl());
		}
		
		ServiceMethod sm = si.getMethod(req.getMethod(), req.getArgs());
		
		context.setObject(Constants.SERVICE_ITEM_KEY, si);
		context.setObject(Constants.SERVICE_METHOD_KEY, sm);
		
		Object obj = serviceLoader.getService(Integer.parseInt(req.getImpl()));
		
		if(obj == null){
			SF.doRequestLog(MonitorConstant.LOG_ERROR,JMicroContext.class,null," service INSTANCE not found");
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found,srv: "+req.getImpl());
		}
		context.setObject(Constants.SERVICE_OBJ_KEY, obj);
	}
	
	public static boolean existLinkId(){
		return get().exists(Constants.NEW_LINKID) && get().getBoolean(Constants.NEW_LINKID, false);
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
	
	/**
	 * 同一个线程多个RPC之间上下文切换
	 */
	public void backupAndClear() {
		Map<String,Object> ps = new HashMap<>();
		ps.putAll(cxt.get().params);
		stack.push(ps);
		cxt.get().params.clear();
	}
	
	
	public void restore() {
		cxt.get().params.clear();
		Map<String,Object> ps = stack.pop();
		if(ps == null) {
			throw new CommonException("JMicro Context stack invalid");
		}
		cxt.get().params.putAll(ps);;
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
				long cost = System.currentTimeMillis() - this.getLong(CLIENT_UP_TIME, System.currentTimeMillis());
				if(force || cost > sm.getTimeout()-100) {
					//超时的请求才记录下来
					StringBuilder sb = this.getDebugLog();
					sb.append(",").append(label).append(": ").append(System.currentTimeMillis() - cost);
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
		long cost = System.currentTimeMillis() - this.getLong(CLIENT_UP_TIME, System.currentTimeMillis());
		if(timeout > 0) {
			if(cost > timeout) {
				//超时的请求才记录下来
				log.append(", cost except :").append(timeout).append(" : ").append(cost);
				logger.warn(log.toString());
			}
		} else {
			ServiceMethod sm = this.getParam(Constants.SERVICE_METHOD_KEY, null);
			if(sm != null) {
				if(cost > sm.getTimeout()-300) {
					//超时的请求才记录下来
					log.append(", mybe timeout except :").append((sm.getTimeout()-300)).append(" : ").append(cost);
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
			this.params.put(p.getKey(), p.getValue());
		}
	}
	
/*	public Boolean isMonitor(){
		if(this.exists(Constants.MONITOR_ENABLE_KEY)){
			return this.getBoolean(Constants.MONITOR_ENABLE_KEY, false);
		};
		
		Config cfg = JMicro.getObjectFactory().get(Config.class);
		return cfg.getBoolean(Constants.MONITOR_ENABLE_KEY,false);
	}*/
	
	public boolean exists(String key){
		return this.params.containsKey(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key,T defautl){
		T v = (T)this.params.get(key);
		if(v == null){
			return defautl;
		}
		return v;
	}
	
	public void removeParam(String key){
	    this.params.remove(key);
	}
	
	public <T> void setParam(String key,T val){
		this.params.put(key,val);
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
