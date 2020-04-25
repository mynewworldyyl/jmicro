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
package org.jmicro.api.registry;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jmicro.api.annotation.SO;
import org.jmicro.api.config.Config;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  单位时间处理速度，类似QPS，但是间单位可定制
 *  时间单位：H：小时， M： 分钟， S：秒 ，MS：毫少，NS：纳秒
 *  如90S，表示每秒钟处理90个请求，20M，表示每分钟处理20个请求，数字只能为整数，不能是小数
 *  
 *  服务标识：服务级标识，服务方法标识，服务实例标识
 *  服务级标识 同服务名称，服务命名空间，服务版本组成 , 参考 {@link UniqueServiceKey}
 *  服务方法标识 同服务标识+服务方法名称+服务参数级成，参考  {@link UniqueServiceMethodKey}
 *  服务实例标识一个具体的运行实例，分为服务实例，服务方法实例，在服务级标识及服务方法标识 基础上加实例名称组成，
 *  运行过程中又可由IP+PORT标识。
 *  
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:29
 */
@SO
public final class ServiceItem implements Comparable<ServiceItem>{

	private final static Logger logger = LoggerFactory.getLogger(ServiceItem.class);
	
	public static final String FILE_SEPERATOR="/";
	
	public static final String I_I_SEPERATOR="####";
	
	public static final String KV_SEPERATOR="=";
	public static final String VAL_SEPERATOR="&";
	
	public static final String KEY_SEPERATOR="##";
	
	private static final Random rand = new Random();
	
	//-1 use system default value, 0 disable, 1 enable
	//@JField(persistence=true)
	
	private UniqueServiceKey key;
	
	//private String instanceName="";
	
	private int code = 0;
	
	//开启debug模式
	private int debugMode = 0;
		
	private int monitorEnable = 0;
	
	private int logLevel = MonitorConstant.LOG_ERROR;
	
	//基本时间单位
	private String baseTimeUnit = Constants.TIME_MILLISECONDS;
	
	//由baseTimeUnit计算出来的JVM极别的时间单位，方便使用
	private transient TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	
	//统计服务数据基本时长，单位同baseTimeUnit确定 @link SMethod
	private long timeWindow = 180000;
	
	private int slotSize = 60;
	
	//采样统计数据周期，单位由baseTimeUnit确定
	private long checkInterval = -1;
		
	private Set<Server> servers = new HashSet<Server>();
	
	private String impl;
	
	private String handler;
	
	private int retryCnt=-1; //method can retry times, less or equal 0 cannot be retry
	private int retryInterval=-1; // milliseconds how long to wait before next retry
	private int timeout=-1; // milliseconds how long to wait for response before timeout 
	
	/**
	 * 1 is normal status, 
	 * 
	 * update rule:
	 * 2 will trigger the maxSpeed=2*maxSpeed and minSpeed=2*minSpeed, n will trigger 
	 * maxSpeed=n*maxSpeed and minSpeed=n*minSpeed
	 * 
	 * degrade rule:
	 * -2 will trigger maxSpeed=maxSpeed/2 and minSpeed=minSpeed/2, and n will trigger
	 * maxSpeed=maxSpeed/n and minSpeed=minSpeed/n
	 * 
	 * 0 and -1 is a invalid value
	 */
	private int degrade = -1;
	
	private int maxSpeed = -1;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 */
	private int avgResponseTime = -1;
	
	private int clientId = 0;
	
	private Set<ServiceMethod> methods = new HashSet<>();
	
	//private long createdTime = System.currentTimeMillis();
	
	public ServiceItem() {}
	
	public ServiceItem(String val) {
		//this.parseKey(key);
		this.parseVal(val);
	}
	
	public Server getServer(String transport) {
		for(Server s: servers) {
			if(s.getProtocol().equals(transport)){
				return s;
			}
		}
		return null;
	}
	
    @Override
	public int compareTo(ServiceItem o) {
		String key = this.getKey().toKey(true, false, false);
		String keyo = o.getKey().toKey(true, false, false);
		return key.compareTo(keyo);
	}

	public void formPersisItem(ServiceItem p){
		this.monitorEnable = p.monitorEnable;
		
		this.retryCnt=p.retryCnt;
		this.retryInterval=p.retryInterval;
		this.timeout = p.timeout;
		
		this.degrade = p.degrade;
		this.maxSpeed = p.maxSpeed;
		this.avgResponseTime = p.avgResponseTime;
		this.logLevel = p.logLevel;
		
		this.baseTimeUnit = p.baseTimeUnit;
		this.timeUnit = p.timeUnit;
		this.timeWindow = p.timeWindow;
		this.checkInterval = p.checkInterval;
		this.handler = p.handler;
		this.slotSize = p.slotSize;
		this.clientId = p.clientId;
		
		for(ServiceMethod sm : p.getMethods()){
			ServiceMethod nsm = this.getMethod(sm.getKey().getMethod(), sm.getKey().getParamsStr());
			if(nsm != null){
				nsm.formPersisItem(sm);
				nsm.setBreakingRule(new BreakRule());
				nsm.getBreakingRule().from(sm.getBreakingRule());
			}
		}
	}

	public String getHandler() {
		return handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public long getTimeWindow() {
		return timeWindow;
	}

	public void setTimeWindow(long timeWindow) {
		this.timeWindow = timeWindow;
	}

	public String getImpl() {
		return impl;
	}

	public void setImpl(String impl) {
		this.impl = impl;
	}

	public Set<Server> getServers() {
		return servers;
	}

	public int getDegrade() {
		return degrade;
	}

	public void setDegrade(int degrade) {
		this.degrade = degrade;
	}

	public int getMonitorEnable() {
		return monitorEnable;
	}

	public int getSlotSize() {
		return slotSize;
	}

	public void setSlotSize(int slotSize) {
		this.slotSize = slotSize;
	}

	public void setMonitorEnable(int monitorEnable) {
		this.monitorEnable = monitorEnable;
	}

	public int getAvgResponseTime() {
		return avgResponseTime;
	}

	public void setAvgResponseTime(int avgResponseTime) {
		this.avgResponseTime = avgResponseTime;
	}

	public int getDebugMode() {
		return debugMode;
	}

	public void setDebugMode(int debugMode) {
		this.debugMode = debugMode;
	}

	public int getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	public void addMethod(ServiceMethod sm){
		methods.add(sm);
	}
	
	public Set<ServiceMethod> getMethods(){
		return methods;
	}
	
	//服务标识，服务名，名称空间，版本，3元组坐标
	public String serviceKey() {
	   return UniqueServiceKey.serviceName(this.getKey().getServiceName(), this.getKey().getNamespace(),
			   this.getKey().getVersion()).toString();
	}

	private void parseVal(String val) {
		if(StringUtils.isEmpty(val)){
			return;
		}
		String methodStr = null;
		
		val = Utils.getIns().decode(val);
		String[] kvs = val.split(VAL_SEPERATOR);
		for(String kv : kvs){
			String[] vs = kv.split(KV_SEPERATOR);
			if(vs.length < 1) {
				throw new CommonException("ServerItem value invalid: "+kv);
			}
			if(vs.length == 1){
				continue;
			}
			if(vs[0].equals("methods")){
				methodStr = vs[1];
				continue;
			}
			try {
				Field f = this.getClass().getDeclaredField(vs[0]);
				f.setAccessible(true);
				if(f.getType() == String.class){
					f.set(this, vs[1]);
				}else if(f.getType() == Integer.TYPE){
					f.set(this, Integer.parseInt(vs[1]));
				}else if(f.getType() == Boolean.TYPE){
					f.set(this, Boolean.parseBoolean(vs[1]));
				}else if(f.getType() == Float.TYPE){
					f.set(this, Float.parseFloat(vs[1]));
				}else if(f.getType() == Double.TYPE){
					f.set(this, Double.parseDouble(vs[1]));
				}else if(f.getType() == Byte.TYPE){
					f.set(this, Byte.parseByte(vs[1]));
				}else if(f.getType() == Short.TYPE){
					f.set(this, Short.parseShort(vs[1]));
				}else if(f.getType() == Character.TYPE){
					f.set(this,vs[1]);
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				logger.error("parseVal Field:"+vs[0],e);
			}
		}
		
		this.timeUnit = TimeUtils.getTimeUnit(this.baseTimeUnit);
		
		if(methodStr.length() == 2){
			return;
		}
		
		methodStr = methodStr.trim().substring(1, methodStr.length()-1);
		String[] ms = methodStr.split("##");
		for(String m : ms){
			ServiceMethod sm = new ServiceMethod();
			sm.fromJson(m);
			this.methods.add(sm);
		}
	}
	
	public ServiceMethod getMethod(String methodName,String mkStr){
		Set<ServiceMethod> ms = new HashSet<>();
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod())){
				ms.add(sm);
			}
		}
		if(ms.isEmpty()) {
			return null;
		}
		
		if(ms.size() == 1) {
			return methods.iterator().next();
		}
		
		if(StringUtils.isEmpty(mkStr)) {
			throw new CommonException("Service method cannot be found with null params service: "+
					this.getKey().getServiceName()+" , M: "+methodName);
		}
		
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod()) && mkStr.equals(sm.getKey().getParamsStr()) ){
				return sm;
			}
		}
		
		Class<?> paramClazzes[] = UniqueServiceMethodKey.paramsClazzes(mkStr);
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod())){
				Class<?> paramClazzes1[] = UniqueServiceMethodKey.paramsClazzes(sm.getKey().getParamsStr());
				if(paramClazzes.length == 0 && paramClazzes1.length == 0) {
					return sm;
				}
				if(paramClazzes.length != paramClazzes1.length) {
					continue;
				}
				
				boolean f = true;
				for(int i=0; i < paramClazzes1.length; i++) {
					if(paramClazzes[i].isPrimitive() || paramClazzes1[i].isPrimitive()) {
						if((paramClazzes[i] == Byte.class || paramClazzes[i] == Byte.TYPE) && (paramClazzes[i] == Byte.class || paramClazzes1[i] == Byte.TYPE) ) {
							continue;
						}else if((paramClazzes[i] == Short.class || paramClazzes[i] == Short.TYPE) && (paramClazzes1[i] == Short.class || paramClazzes1[i] == Short.TYPE) ) {
							continue;
						}else if((paramClazzes[i] == Integer.class || paramClazzes[i] == Integer.TYPE) && (paramClazzes1[i] == Integer.class || paramClazzes1[i] == Integer.TYPE) ) {
							continue;
						}else if((paramClazzes[i] == Long.class || paramClazzes[i] == Long.TYPE) && (paramClazzes1[i] == Long.class || paramClazzes1[i] == Long.TYPE) ) {
							continue;
						}else if((paramClazzes[i] == Boolean.class || paramClazzes[i] == Boolean.TYPE) && (paramClazzes1[i] == Boolean.class || paramClazzes1[i] == Boolean.TYPE) ) {
							continue;
						}else if((paramClazzes[i] == Character.class || paramClazzes[i] == Character.TYPE) && (paramClazzes1[i] == Character.class || paramClazzes1[i] == Character.TYPE) ) {
							continue;
						}else if((paramClazzes[i] == Float.class || paramClazzes[i] == Float.TYPE) && (paramClazzes1[i] == Float.class || paramClazzes1[i] == Float.TYPE) ) {
							continue;
						}else if((paramClazzes[i] == Double.class || paramClazzes[i] == Double.TYPE) && (paramClazzes1[i] == Double.class || paramClazzes1[i] == Double.TYPE) ) {
							continue;
						} else {
							f = false;
							break;
						}
					} else if(!paramClazzes1[i].isAssignableFrom(paramClazzes[i])) {
						f = false;
						break;
					}
				}
				
				if(f) {
					return sm;
				}
			}
		}
		
		return null;
	}
	
	public ServiceMethod getMethod(String methodName,Object[] args){
		String mkStr = UniqueServiceMethodKey.paramsStr(args);
		return getMethod(methodName, mkStr);
	}
	
	public ServiceMethod getMethod(String methodName,Class<?>[] args){
		String mkStr = UniqueServiceMethodKey.paramsStr(args);
		return getMethod(methodName, mkStr);
	}
	
	//服务实例标识,带上实例名和主机IP
	public String path(String root){
		return this.key.path(root, true, true, true);
	}
	
	public static String pathForKey(String key){
		StringBuffer sb = new StringBuffer(Config.ServiceRegistDir);
		sb.append(FILE_SEPERATOR);
		sb.append(key);
		return sb.toString();
	}
	
	public String key(){
		return key.toKey(true,true,true);
	}

	@Override
	public int hashCode() {
		return this.path(Config.ServiceRegistDir).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ServiceItem)) {
			return false;
		}
		return this.path(Config.ServiceRegistDir).equals(((ServiceItem)obj).path(Config.ServiceRegistDir));
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public int getRetryCnt() {
		return retryCnt;
	}

	public void setRetryCnt(int retryCnt) {
		this.retryCnt = retryCnt;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setMethods(Set<ServiceMethod> methods) {
		this.methods = methods;
	}

	public UniqueServiceKey getKey() {
		return key;
	}

	public void setKey(UniqueServiceKey key) {
		this.key = key;
	}

	public String getBaseTimeUnit() {
		return baseTimeUnit;
	}

	public void setBaseTimeUnit(String baseTimeUnit) {
		this.baseTimeUnit = baseTimeUnit;
		this.setTimeUnit(TimeUtils.getTimeUnit(baseTimeUnit));
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public long getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
	
	
}