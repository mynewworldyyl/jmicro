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
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.jmicro.api.config.Config;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
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
	private int monitorEnable = -1;
	
	private String serviceName;
	
	private String namespace = Constants.DEFAULT_NAMESPACE;
	
	private String version = Constants.DEFAULT_VERSION;
	
	private Set<Server> servers = new HashSet<Server>();
	
	private String impl;
	
	private int retryCnt=-1; //method can retry times, less or equal 0 cannot be retry
	private int retryInterval=-1; // milliseconds how long to wait before next retry
	private int timeout=-1; // milliseconds how long to wait for response before timeout 
	
	/**
	 * Max failure time before downgrade the service
	 */
	private int maxFailBeforeDegrade=-1;
	
	/**
	 * Max failure time before cutdown the service
	 */
	private int maxFailBeforeFusing=-1;
	
	/**
	 * after the service fuse, system can do testing weather the service is recovery
	 * with this arguments to invoke the service method
	 */
	private String testingArgs="";
	
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
	private int degrade = -1;
	
	/**
	 * 单位时间处理速度，类似QPS，但是间单位可定制
	 *  时间单位：H：小时， M： 分钟， S：秒 ，MS：毫少，NS：纳秒
	 *  如90S，表示每秒钟处理90个请求，20M，表示每分钟处理20个请求，数字只能为整数，不能是小数
	 */
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
		
		this.maxFailBeforeDegrade=p.maxFailBeforeDegrade;
		this.maxFailBeforeFusing=p.maxFailBeforeFusing;
		
		this.testingArgs = p.testingArgs;
		this.fusing = p.fusing;
		
		this.degrade = p.degrade;
		this.maxSpeed = p.maxSpeed;
		this.avgResponseTime = p.avgResponseTime;
		
		for(ServiceMethod sm : p.getMethods()){
			ServiceMethod nsm = this.getMethod(sm.getMethodName(), sm.getMethodParamTypes());
			if(nsm != null){
				nsm.formPersisItem(sm);
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

	public boolean isBreaking() {
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
	   return serviceName(this.serviceName,this.namespace,this.version);
	}
	
	public static String namespace(String namespace){
		if(namespace == null || "".equals(namespace)){
			namespace = Constants.DEFAULT_NAMESPACE;
		}
		return namespace;
	}
	
   public static String version(String version){
		if(version == null || "".equals(version)){
			version = Constants.DEFAULT_VERSION;
		}
		return version;
	}
	
	public static String serviceName(String sn, String ns, String v) {
		return snnsPrefix(sn,ns)+version(v);
	}
	
	public static String snnsPrefix(String sn, String ns) {
		return sn+KEY_SEPERATOR+namespace(ns)+KEY_SEPERATOR;
	}
	
	/**
	 *   1.0.0
	 *   
	 *   1.0.0 < v < 2.0.3
	 *   1.0.0 < v
	 *   v < 2.0.3
	 *  
	 *   1.0.0 <= v <= 2.0.3
	 *   1.0.0 <= v
	 *   v <= 2.0.3
	 *   
	 *   x.*.*
	 *   *
	 *   *.x.*
	 *   
	 *   *.*.*
	 *   
	 * @param macher
	 * @param version
	 * @return
	 */
	public static boolean matchVersion(String macher, String version) {
		boolean result = false;
		if(version.equals(macher)) {
			return true;
		}
		if(macher.indexOf("<=") > 0) {
			String[] arr = macher.split("<=");
			if(arr.length == 3) {
				result= compare(arr[0],version)<=0 && compare(version,arr[1])<=0;
			}else if(arr.length == 2) {
				if(arr[0].indexOf(".") > 0) {
					result=  compare(arr[0],version)<=0;
				}else if(arr[1].indexOf(".") > 0) {
					result=  compare(version,arr[1])<=0;
				}
			}
		}else if(macher.indexOf("<") > 0) {
			String[] arr = macher.split("<");
			if(arr.length == 3) {
				result= compare(arr[0],version)<0 && compare(version,arr[1])<0;
			}else if(arr.length == 2) {
				if(arr[0].indexOf(".") > 0) {
					result=  compare(arr[0],version)<0;
				}else if(arr[1].indexOf(".") > 0) {
					result=  compare(version,arr[1])<0;
				}
			}
		}else if(macher.indexOf("*") >= 0) {
			if("*".equals(macher.trim())) {
				result = true;
			} else {
				result = true;
				String[] arr = macher.split(".");
				String[] varr = version.split(".");
				for(int i=0; i < arr.length; i++) {
					if(!arr[i].equals("*") && !arr[i].equals(varr[i])) {
						result = false;
						break;
					}
				}
			}
		}
		return result;
	}
	
	public static int compare(String first, String second) {
		return first.compareTo(second);
	}
	
	public static String methodKey(String serviceName, String method,String paramStr) {
		if(StringUtils.isEmpty(method)) {
			throw new CommonException("service ["+serviceName+"] Method cannot be null,");
		}
		return serviceName+KEY_SEPERATOR+method+KEY_SEPERATOR+paramStr;
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
	
	public ServiceMethod getMethod(String methodName,Object[] args){
		String mk = ServiceMethod.methodParamsKey(args);
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getMethodName()) && mk.equals(sm.getMethodParamTypes())){
				return sm;
			}
		}
		return null;
	}
	
	public ServiceMethod getMethod(String methodName,Class<?>[] args){
		String mk = ServiceMethod.methodParamsKey(args);
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getMethodName()) && mk.equals(sm.getMethodParamTypes())){
				return sm;
			}
		}
		return null;
	}
	
	public ServiceMethod getMethod(String methodName,String paramTypesStr){
		for(ServiceMethod sm : this.methods){
			if(methodName.equals(sm.getMethodName()) && paramTypesStr.equals(sm.getMethodParamTypes())){
				return sm;
			}
		}
		return null;
	}
	
	public String key(String root){
		StringBuffer sb = new StringBuffer(root);
				
		if(!this.serviceName.startsWith(FILE_SEPERATOR)){
			sb.append(FILE_SEPERATOR);
		}
		
		sb.append(Config.getInstanceName()).append(VAL_SEPERATOR)
		.append(this.serviceName).append(VAL_SEPERATOR)
		.append(this.namespace).append(VAL_SEPERATOR)
		.append(this.version)/*.append(VAL_SEPERATOR)
		.append(this.createdTime)*/;

		return sb.toString();
	}
	
	public String val(){
		StringBuffer sb = new StringBuffer();
		Field[] fields = this.getClass().getDeclaredFields();
		for(Field f : fields){
			if(Modifier.isStatic(f.getModifiers()) || "methods".equals(f.getName())){
				continue;
			}
			try {
				Object v = f.get(this);
				sb.append(f.getName()).append(KV_SEPERATOR).append(v==null?"":v.toString()).append(VAL_SEPERATOR);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				logger.error("val Field:"+f.getName(),e);
			}
		}
		
		sb.append("methods").append(KV_SEPERATOR).append("[");
		
		int i = 0;
		for(ServiceMethod sm : this.methods){
			sb.append(sm.toJson());
			if(i++ < this.methods.size()-1){
				sb.append("##");
			}
		}
		
		sb.append("]");
		
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

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		if(namespace != null && !"".equals(namespace.trim())){
			this.namespace = namespace;
		}
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		if(version != null && !"".equals(version.trim())){
			this.version = version;
		}
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

	public int getMaxFailBeforeDegrade() {
		return maxFailBeforeDegrade;
	}

	public void setMaxFailBeforeDegrade(int maxFailBeforeDowngrade) {
		this.maxFailBeforeDegrade = maxFailBeforeDowngrade;
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

	public void setMethods(Set<ServiceMethod> methods) {
		this.methods = methods;
	}
	
}