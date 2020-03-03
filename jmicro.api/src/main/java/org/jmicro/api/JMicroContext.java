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
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.Linker;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
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
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:55:28
 */
public class JMicroContext  {

	//value should be true or false, for provider or sunsumer side
	public static final String CALL_SIDE_PROVIDER = "callSideProvider";
	
	public static final String LOCAL_HOST = "host";
	public static final String LOCAL_PORT = "port";
	
	public static final String REMOTE_HOST = "remoteHost";
	public static final String REMOTE_PORT = "remotePort";
	
	public static final String LINKER_ID = "linkerId";
	public static final String REQ_ID = "reqId";
	public static final String MSG_ID = "msgId";
	
	public static final String MONITOR = "monitor";
	
	public static final String CLIENT_SERVICE = "clientService";
	public static final String CLIENT_NAMESPACE = "clientNamespace";
	public static final String CLIENT_VERSION = "clientVersion";
	public static final String CLIENT_METHOD = "clientMehtod";
	public static final String CLIENT_ARGSTR= "argStr";
	
	public static String[] args = {};
	
	protected Map<String,Object> params = new HashMap<String,Object>();
	
	public static final String SESSION_KEY="_sessionKey";
	private static final ThreadLocal<JMicroContext> cxt = new ThreadLocal<JMicroContext>();
	
	private Stack<Map<String,Object>> stack = new Stack<>();
	
	private boolean isLoggable = false;
	
	private JMicroContext() {}
	
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
		if(c == null) {
			c = new JMicroContext();
			cxt.set(c);
		}
		return c;
	}
	
	public static boolean callSideProdiver(Boolean ... flag){
		if(flag == null || flag.length == 0) {
			return get().getBoolean(JMicroContext.CALL_SIDE_PROVIDER, true);
		} else {
			get().setBoolean(JMicroContext.CALL_SIDE_PROVIDER, flag[0]);
		}
		return flag[0];
	}
	
	public static void configProvider(IMonitorDataSubmiter monitor,ISession s) {
		callSideProdiver(true);
		setMonitor();
		JMicroContext context = get();
		
		context.setParam(JMicroContext.SESSION_KEY, s);
			
		context.setParam(JMicroContext.REMOTE_HOST, s.remoteHost());
		context.setParam(JMicroContext.REMOTE_PORT, s.remotePort()+"");
		
		context.setParam(JMicroContext.LOCAL_HOST, s.localHost());
		context.setParam(JMicroContext.LOCAL_PORT, s.localPort()+"");
	}
	
	public static void configProvider(Message msg) {
		JMicroContext context = cxt.get();
		context.configMonitorable(msg.isMonitorable());
		context.setParam(JMicroContext.LINKER_ID, msg.getLinkId());
		context.isLoggable = msg.isLoggable();
	}
	
	public static void config(IRequest req, ServiceLoader serviceLoader,IRegistry registry) {
		JMicroContext context = cxt.get();
		context.setString(JMicroContext.CLIENT_SERVICE, req.getServiceName());
		context.setString(JMicroContext.CLIENT_NAMESPACE, req.getNamespace());
		context.setString(JMicroContext.CLIENT_METHOD, req.getMethod());
		context.setString(JMicroContext.CLIENT_VERSION, req.getVersion());
		context.setParam(JMicroContext.CLIENT_ARGSTR, UniqueServiceMethodKey.paramsStr(req.getArgs()));
		context.mergeParams(req.getRequestParams());
		
		ServiceItem si = registry.getServiceByImpl(req.getImpl());
		if(si == null){
			if(req.isLoggable()) {
				SF.doRequestLog(MonitorConstant.LOG_ERROR,JMicroContext.class, req,null," service ITEM not found");
			}
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found impl："+req.getImpl());
		}
		
		ServiceMethod sm = si.getMethod(req.getMethod(), req.getArgs());
		
		context.setObject(Constants.SERVICE_ITEM_KEY, si);
		context.setObject(Constants.SERVICE_METHOD_KEY, sm);
		
		Object obj = serviceLoader.getService(req.getImpl());
		
		if(obj == null){
			SF.doRequestLog(MonitorConstant.LOG_ERROR,JMicroContext.class, req,null," service INSTANCE not found");
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found,srv: "+req.getImpl());
		}
		context.setObject(Constants.SERVICE_OBJ_KEY, obj);
	}
	
	/*public static void setSrvLoggable(){
		JMicroContext c = get();
		ServiceItem si = c.getParam(Constants.SERVICE_ITEM_KEY, null);
		ServiceMethod sm = c.getParam(Constants.SERVICE_METHOD_KEY, null);
		
		if(sm == null && si == null) {
			//都没有配置的情况下,不启用服务级的log功能
			c.isLoggable = false;
			return;
		}
		
		if(sm.getLoggable() != -1) {
			//方法级配置有效
			c.isLoggable = sm.getLoggable() == 1;
			return;
		}
		
		if(si.getLoggable() != -1) {
			//服务级配置有效
			c.isLoggable = sm.getLoggable() == 1;
			return;
		}
		
		//默认不启用服务级的log配置
		c.isLoggable = false;
	}*/
	
	/*public boolean isLoggable(boolean isComOpen) {
		return isLoggable || isComOpen;
	}*/
	
	public static boolean existLinkId(){
		return get().exists(LINKER_ID) && get().getBoolean(LINKER_ID, false);
		
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
	
	public void backup() {
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
	
	public static void setMonitor(){
		JMicroContext.get().setObject(JMicroContext.MONITOR, 
				JMicro.getObjectFactory().get(IMonitorDataSubmiter.class));
	}
	
	public void configMonitor(int methodCfg,int srvCfg){
		if(methodCfg != -1){
			configMonitorable(methodCfg==1);
		} else if(srvCfg != -1){
			configMonitorable(srvCfg==1);
		}
	}
	
	public void configMonitorable(boolean enable){
		this.setBoolean(Constants.MONITOR_ENABLE_KEY, enable);
	}
	
	

	public void mergeParams(JMicroContext c){
		Map<String,Object> ps = c.params;
		if(ps == null || ps.isEmpty()) {
			return;
		}
		for(Map.Entry<String, Object> p : ps.entrySet()){
			this.params.put(p.getKey(), p.getValue());
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
	
	public Boolean isMonitor(){
		if(this.exists(Constants.MONITOR_ENABLE_KEY)){
			return this.getBoolean(Constants.MONITOR_ENABLE_KEY, false);
		};
		Config cfg = JMicro.getObjectFactory().get(Config.class);
		return cfg.getBoolean(Constants.MONITOR_ENABLE_KEY,false);
	}
	
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
	
	@SuppressWarnings("unchecked")
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
