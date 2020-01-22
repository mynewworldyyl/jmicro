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
import java.lang.reflect.Modifier;

import org.jmicro.api.annotation.SO;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:38
 */
@SO
public final class ServiceMethod {
	
	public transient ServiceItem _serviceItem;

	private UniqueServiceMethodKey key = new UniqueServiceMethodKey();
	
	//private String methodName="";
	//private String[] methodParamTypes; //full type name
	
	//-1 use Service config, 0 disable, 1 enable
	private int monitorEnable = -1;
	private int loggable  = -1;
	//dump 下行流，用于下行数问题排查
	private boolean dumpDownStream = false;
	//dump 上行流，用于上行数问题排查
	private boolean dumpUpStream = false;
	
	//开启debug模式
	private int debugMode = -1;
	
	private int retryCnt; //method can retry times, less or equal 0 cannot be retry
	private int retryInterval; // milliseconds how long to wait before next retry
	private int timeout; // milliseconds how long to wait for response before timeout 
	
	private BreakRule breakingRule = new BreakRule();
	
	private boolean asyncable = false;
	
	//统计服务数据基本时长，单位同baseTimeUnit确定 @link SMethod
	private long timeWindow = -1;
	
	//循环时钟槽位个数
	private int slotSize = 10;
	
	//采样统计数据周期，单位由baseTimeUnit确定
	private long checkInterval = -1;
	
	//基本时间单位  @link SMethod
	private String baseTimeUnit = Constants.TIME_MILLISECONDS;
	
	/**
	 * true all service method will fusing, false is normal service status
	 */
	private boolean breaking = false;
	
	/**
	 * 失败时的默认返回值，包括服务熔断失败，降级失败等
	 */
	private String failResponse;
	
	/**
	 * Max failure time before degrade the service
	 */
	private int maxFailBeforeDegrade;
	
	/**
	 * after the service cutdown, system can do testing weather the service is recovery
	 * with this arguments to invoke the service method
	 */
	private String testingArgs;
	
	/**
	 * max qps，单位同baseTimeUnit确定
	 */
	private int maxSpeed = -1;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	private int avgResponseTime;
	
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
	private int degrade = 1;
	
	//0: need response, 1:no need response
	private boolean needResponse = true;

	//true async return result,
	//public boolean async = false;

	//false: not stream, true:stream, more than one request and response double stream
	//a stream service must be async=true, and get got result by callback
	private boolean stream = false;
	
	//如果客户端RPC异步调用，此topic值必须是方法全限定名，参考toKey方法实现
	private String topic = null;

	public void formPersisItem(ServiceMethod p){
		this.monitorEnable = p.monitorEnable;

		this.retryCnt = p.retryCnt;
		this.retryInterval = p.retryInterval;
		this.timeout = p.timeout;

		this.maxFailBeforeDegrade = p.maxFailBeforeDegrade;
		this.getBreakingRule().from(p.getBreakingRule());
		this.asyncable = p.asyncable;

		this.testingArgs = p.testingArgs;
		this.breaking = p.breaking;

		this.degrade = p.degrade;
		this.maxSpeed = p.maxSpeed;
		this.avgResponseTime = p.avgResponseTime;
		
		this.loggable = p.loggable;
		
		this.baseTimeUnit = p.baseTimeUnit;
		this.checkInterval = p.checkInterval;
		
		this.dumpDownStream = p.dumpDownStream;
		this.dumpUpStream = p.dumpUpStream;
		this.debugMode = p.debugMode;
		
		this.topic = p.topic;
	}
	
	public String toJson(){
		StringBuffer sb = new StringBuffer("{");
		Field[] fields = this.getClass().getDeclaredFields();
		
		for(int i =0; i < fields.length; i++){
			Field f = fields[i];
			if(Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())){
				continue;
			}
			try {
				Object v = f.get(this);
				sb.append(f.getName()).append(":").append(v == null?"":v.toString());
				if(i != fields.length-1){
					sb.append(",");
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("toJson service mehtod error: "+f.getName());
			}
		}
		
		return sb.toString();
	}
	
	public void fromJson(String mstr){
		mstr = mstr.substring(1,mstr.length()-1);
		String[] kvs = mstr.split(",");
		Class<?> cls = this.getClass();
		for(String kv: kvs){
			String[] ms = kv.split(":");
			if(ms.length < 1){
				throw new CommonException("Parse service mehtod error: "+mstr);
			}
			if(ms.length == 1){
				continue;
			}
			try {
				Field f = cls.getDeclaredField(ms[0]);
				f.setAccessible(true);
				if(f.getType() == String.class){
					f.set(this, ms[1]);
				}else if(f.getType() == Integer.TYPE){
					f.set(this, Integer.parseInt(ms[1]));
				}else if(f.getType() == Boolean.TYPE){
					f.set(this, Boolean.parseBoolean(ms[1]));
				}else if(f.getType() == Float.TYPE){
					f.set(this, Float.parseFloat(ms[1]));
				}else if(f.getType() == Double.TYPE){
					f.set(this, Double.parseDouble(ms[1]));
				}else if(f.getType() == Byte.TYPE){
					f.set(this, Byte.parseByte(ms[1]));
				}else if(f.getType() == Short.TYPE){
					f.set(this, Short.parseShort(ms[1]));
				}else if(f.getType() == Character.TYPE){
					f.set(this,ms[1]);
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("Parse service mehtod error: "+mstr,e);
			}
		}
	}
	
	public boolean isDumpDownStream() {
		return dumpDownStream;
	}

	public void setDumpDownStream(boolean dumpDownStream) {
		this.dumpDownStream = dumpDownStream;
	}

	public boolean isDumpUpStream() {
		return dumpUpStream;
	}

	public void setDumpUpStream(boolean dumpUpStream) {
		this.dumpUpStream = dumpUpStream;
	}

	public boolean isBreaking() {
		return breaking;
	}

	public void setBreaking(boolean breaking) {
		this.breaking = breaking;
	}

	public int getDegrade() {
		return degrade;
	}

	public void setDegrade(int degrade) {
		this.degrade = degrade;
	}

	public int getAvgResponseTime() {
		return avgResponseTime;
	}

	public void setAvgResponseTime(int avgResponseTime) {
		this.avgResponseTime = avgResponseTime;
	}

	public int getLoggable() {
		return loggable;
	}

	public int getDebugMode() {
		return debugMode;
	}

	public void setDebugMode(int debugMode) {
		this.debugMode = debugMode;
	}

	public long getCheckInterval() {
		return checkInterval;
	}

	public void setCheckInterval(long checkInterval) {
		this.checkInterval = checkInterval;
	}

	public void setLoggable(int loggable) {
		this.loggable = loggable;
	}

	public UniqueServiceMethodKey getKey() {
		return key;
	}

	public void setKey(UniqueServiceMethodKey key) {
		this.key = key;
	}

	public int getRetryCnt() {
		return retryCnt;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
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
	
	public int getMonitorEnable() {
		return monitorEnable;
	}

	public void setMonitorEnable(int monitorEnable) {
		this.monitorEnable = monitorEnable;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getMaxFailBeforeDegrade() {
		return maxFailBeforeDegrade;
	}

	public void setMaxFailBeforeDegrade(int maxFailBeforeDegrade) {
		this.maxFailBeforeDegrade = maxFailBeforeDegrade;
	}

	public String getTestingArgs() {
		return testingArgs;
	}

	public void setTestingArgs(String testingArgs) {
		this.testingArgs = testingArgs;
	}

	public boolean isNeedResponse() {
		return needResponse;
	}

	public void setNeedResponse(boolean needResponse) {
		this.needResponse = needResponse;
	}

	public boolean isStream() {
		return stream;
	}

	public void setStream(boolean stream) {
		this.stream = stream;
	}

	public String getFailResponse() {
		return failResponse;
	}

	public void setFailResponse(String failResponse) {
		this.failResponse = failResponse;
	}

	public BreakRule getBreakingRule() {
		return breakingRule;
	}

	public void setBreakingRule(BreakRule breakingRule) {
		this.breakingRule = breakingRule;
	}


	public long getTimeWindow() {
		return timeWindow;
	}

	public void setTimeWindow(long timeWindow) {
		this.timeWindow = timeWindow;
	}

	public String getBaseTimeUnit() {
		return baseTimeUnit;
	}

	public void setBaseTimeUnit(String baseTimeUnit) {
		this.baseTimeUnit = baseTimeUnit;
	}

	public int getSlotSize() {
		return slotSize;
	}

	public void setSlotSize(int slotSize) {
		this.slotSize = slotSize;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	@Override
	public int hashCode() {
		return this.key==null?"".hashCode():this.key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

	public boolean isAsyncable() {
		return asyncable;
	}

	public void setAsyncable(boolean asyncable) {
		this.asyncable = asyncable;
	}


}