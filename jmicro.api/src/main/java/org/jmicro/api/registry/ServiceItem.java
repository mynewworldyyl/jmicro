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

import org.jmicro.api.config.Config;
import org.jmicro.common.CommonException;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  单位时间处理速度，类似QPS，但是间单位可定制
 *  时间单位：H：小时， M： 分钟， S：秒 ，MS：毫少，NS：纳秒
 *  如90S，表示每秒钟处理90个请求，20M，表示每分钟处理20个请求，数字只能为整数，不能是小数
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:29
 */
public class ServiceItem{

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
	
	private String instanceName;
	
	private int monitorEnable = -1;
	
	private Set<Server> servers = new HashSet<Server>();
	
	private String impl;
	
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
	
	private String speedUnit = "MS";
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 */
	private int avgResponseTime = -1;
	
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
	
    public void formPersisItem(ServiceItem p){
		this.monitorEnable = p.monitorEnable;
		
		this.retryCnt=p.retryCnt;
		this.retryInterval=p.retryInterval;
		this.timeout = p.timeout;
		
		this.degrade = p.degrade;
		this.maxSpeed = p.maxSpeed;
		this.avgResponseTime = p.avgResponseTime;
		
		for(ServiceMethod sm : p.getMethods()){
			ServiceMethod nsm = this.getMethod(sm.getKey().getMethod(), sm.getKey().getParamsStr());
			if(nsm != null){
				nsm.formPersisItem(sm);
				nsm.setBreakingRule(new BreakRule());
				nsm.getBreakingRule().from(sm.getBreakingRule());
			}
		}
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

	public void setMonitorEnable(int monitorEnable) {
		this.monitorEnable = monitorEnable;
	}

	public int getAvgResponseTime() {
		return avgResponseTime;
	}

	public void setAvgResponseTime(int avgResponseTime) {
		this.avgResponseTime = avgResponseTime;
	}

	public void addMethod(ServiceMethod sm){
		methods.add(sm);
	}
	
	public Set<ServiceMethod> getMethods(){
		return methods;
	}
	
	public String serviceName() {
	   return this.key.toKey(false, false, false);
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
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod()) && mkStr.equals(sm.getKey().getParamsStr()) ){
				return sm;
			}
		}
		return null;
	}
	
	public ServiceMethod getMethod(String methodName,Object[] args){
		String mkStr = UniqueServiceMethodKey.paramsStr(UniqueServiceMethodKey.methodParamsKey(args));
		return getMethod(methodName, mkStr);
	}
	
	public ServiceMethod getMethod(String methodName,Class<?>[] args){
		String mkStr = UniqueServiceMethodKey.paramsStr(UniqueServiceMethodKey.methodParamsKey(args));
		return getMethod(methodName, mkStr);
	}
	
	public String key(String root){
		StringBuffer sb = new StringBuffer(root);
		sb.append(FILE_SEPERATOR);
		sb.append(key.toKey(true,true,false));
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return this.key(Config.ServiceRegistDir).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ServiceItem)) {
			return false;
		}
		return this.key(Config.ServiceRegistDir).equals(((ServiceItem)obj).key(Config.ServiceRegistDir));
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public String getSpeedUnit() {
		return speedUnit;
	}

	public void setSpeedUnit(String speedUnit) {
		this.speedUnit = speedUnit;
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

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public UniqueServiceKey getKey() {
		return key;
	}

	public void setKey(UniqueServiceKey key) {
		this.key = key;
	}
	
}