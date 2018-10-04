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

import org.jmicro.api.exception.CommonException;

import javassist.Modifier;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:38
 */
public class ServiceMethod {

	private String methodName="";
	
	private String methodParamTypes=""; //full type name
	
	private int retryCnt; //method can retry times, less or equal 0 cannot be retry
	private int retryInterval; // milliseconds how long to wait before next retry
	private int timeout; // milliseconds how long to wait for response before timeout 
	
	/**
	 * Max failure time before downgrade the service
	 */
	private int maxFailBeforeDowngrade;
	
	/**
	 * Max failure time before cutdown the service
	 */
	private int maxFailBeforeCutdown;
	
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
	 * min qps
	 * real qps less this value will downgrade service
	 */
	private int minSpeed;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	private int avgResponseTime;

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
				sb.append(f.getName()).append(":").append(v.toString());
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
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("Parse service mehtod error: "+mstr,e);
			}
		}
	}
	
	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(int maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public int getMinSpeed() {
		return minSpeed;
	}

	public void setMinSpeed(int minSpeed) {
		this.minSpeed = minSpeed;
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

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getMaxFailBeforeDowngrade() {
		return maxFailBeforeDowngrade;
	}

	public void setMaxFailBeforeDowngrade(int maxFailBeforeDowngrade) {
		this.maxFailBeforeDowngrade = maxFailBeforeDowngrade;
	}

	public int getMaxFailBeforeCutdown() {
		return maxFailBeforeCutdown;
	}

	public void setMaxFailBeforeCutdown(int maxFailBeforeCutdown) {
		this.maxFailBeforeCutdown = maxFailBeforeCutdown;
	}

	public String getTestingArgs() {
		return testingArgs;
	}

	public void setTestingArgs(String testingArgs) {
		this.testingArgs = testingArgs;
	}
	
	
	
}
