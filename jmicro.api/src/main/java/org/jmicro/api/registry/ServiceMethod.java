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

import org.jmicro.api.exception.CommonException;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:38
 */
public class ServiceMethod {

	private String methodName="";
	
	private String methodParamTypes=""; //full type name
	
	//-1 use Service config, 0 disable, 1 enable
	private int monitorEnable = -1;
	
	private int retryCnt; //method can retry times, less or equal 0 cannot be retry
	private int retryInterval; // milliseconds how long to wait before next retry
	private int timeout; // milliseconds how long to wait for response before timeout 
	
	/**
	 * Max failure time before degrade the service
	 */
	private int maxFailBeforeDegrade;
	
	/**
	 * Max failure time before cutdown the service
	 */
	private int maxFailBeforeFusing;
	
	/**
	 * after the service cutdown, system can do testing weather the service is recovery
	 * with this arguments to invoke the service method
	 */
	private String testingArgs;
	/**
	 * max qps
	 */
	private int maxSpeed;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	private int avgResponseTime;

	
	/**
	 * true all service method will fusing, false is normal service status
	 */
	private boolean fusing = false;
	
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
	public boolean needResponse = true;
	
	//true async return result,
	//public boolean async = false;
		
	//false: not stream, true:stream, more than one request and response double stream
	//a stream service must be async=true, and get got result by callback
	public String streamCallback = "";
	
	public void formPersisItem(ServiceMethod p){
		this.monitorEnable = p.monitorEnable;
		
		this.retryCnt=p.retryCnt;
		this.retryInterval=p.retryInterval;
		this.timeout = p.timeout;
		
		this.maxFailBeforeDegrade=p.maxFailBeforeDegrade;
		this.maxFailBeforeFusing=p.maxFailBeforeFusing;
		
		this.testingArgs = p.testingArgs;
		this.fusing = p.fusing;
		
		this.degrade = p.degrade;
		this.maxSpeed = p.maxSpeed;
		this.avgResponseTime = p.avgResponseTime;
	}
	
	public String toJson(){
		StringBuffer sb = new StringBuffer("{");
		Field[] fields = this.getClass().getDeclaredFields();
		
		for(int i =0; i < fields.length; i++){
			Field f = fields[i];
			if(Modifier.isStatic(f.getModifiers())){
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
		
		/*sb.append("maxFailBeforeCutdown").append(":").append(this.getMaxFailBeforeCutdown()).append(",");
		sb.append("methodName").append(":").append(this.getMethodName()).append(",");
		sb.append("methodParamTypes").append(":").append(this.getMethodParamTypes()).append(",");
		sb.append("retryCnt").append(":").append(this.getRetryCnt()).append(",");
		sb.append("retryInterval").append(":").append(this.getRetryInterval()).append(",");
		sb.append("timeout").append(":").append(this.getTimeout()).append(",");
		sb.append("maxFailBeforeDowngrade").append(":").append(this.getMaxFailBeforeDowngrade()).append(",");
		sb.append("testingArgs").append(":").append(this.getTestingArgs()).append("");*/
		sb.append("}");
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
	
	public static String methodParamsKey(Class<?>[] clazzes){
		if(clazzes != null && clazzes.length >0){
			StringBuffer sb = new StringBuffer();
			for(Class<?> mc: clazzes){
				sb.append(mc.getName()).append("_");
			}
			String sbt = sb.substring(0, sb.length()-1);
			return sbt;
		}
		return "";
	}
	
	public static String methodParamsKey(Object[] args){
		if(args != null && args.length >0){
			Class<?>[] clazzes = new Class<?>[args.length];
			int i = 0;
			for(Object obj: args){
				clazzes[i++]=obj.getClass();
			}
			return methodParamsKey(clazzes);
		}
		return "";
	}
	
	public boolean isFusing() {
		return fusing;
	}

	public void setFusing(boolean fusing) {
		this.fusing = fusing;
	}

	public int getDegrade() {
		return degrade;
	}

	public void setDegrade(int degrade) {
		this.degrade = degrade;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public int getAvgResponseTime() {
		return avgResponseTime;
	}

	public void setAvgResponseTime(int avgResponseTime) {
		this.avgResponseTime = avgResponseTime;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getMethodParamTypes() {
		return methodParamTypes;
	}

	public void setMethodParamTypes(String methodParamTypes) {
		this.methodParamTypes = methodParamTypes;
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

	public int getMaxFailBeforeFusing() {
		return maxFailBeforeFusing;
	}

	public void setMaxFailBeforeFusing(int maxFailBeforeFusing) {
		this.maxFailBeforeFusing = maxFailBeforeFusing;
	}

	public String getTestingArgs() {
		return testingArgs;
	}

	public void setTestingArgs(String testingArgs) {
		this.testingArgs = testingArgs;
	}

	public boolean getNeedResponse() {
		return needResponse;
	}

	public void setNeedResponse(boolean needResponse) {
		this.needResponse = needResponse;
	}

	
	public String getStreamCallback() {
		return streamCallback;
	}

	public void setStreamCallback(String streamCallback) {
		this.streamCallback = streamCallback;
	}
	
}
