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

import org.jmicro.api.exception.CommonException;
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
	
	public static final String ROOT="/jmicro/service";
	
	public static final String PERSIS_ROOT="/jmicro/srvconfig";
	
	public static final String FILE_SEPERATOR="/";
	
	public static final String I_I_SEPERATOR="####";
	
	public static final String KV_SEPERATOR="=";
	public static final String VAL_SEPERATOR="&";
	
	private static final Random rand = new Random();
	
	//-1 use system default value, 0 disable, 1 enable
	//@JField(persistence=true)
	private int monitorEnable = -1;
	
	private String serviceName;
	
	private String namespace = Constants.DEFAULT_NAMESPACE;
	
	private String version = Constants.DEFAULT_VERSION;
	
	private String host;
	
	private int port;
	
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
	 * max qps
	 */
	private int maxSpeed = -1;
	
	/**
	 * min qps
	 * real qps less this value will downgrade service
	 */
	private int minSpeed=-1;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 */
	private int avgResponseTime=-1;
	
	private Set<ServiceMethod> methods = new HashSet<>();
	
	private long randVal = System.currentTimeMillis() ^ rand.nextLong();
	
	public ServiceItem() {}
	
	public ServiceItem(String val) {
		//this.parseKey(key);
		this.parseVal(val);
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
		this.minSpeed = p.minSpeed;
		this.avgResponseTime = p.avgResponseTime;
		
		for(ServiceMethod sm : p.getMethods()){
			ServiceMethod nsm = this.getMethod(sm.getMethodName(), sm.getMethodParamTypes());
			if(nsm != null){
				nsm.formPersisItem(sm);
			}
		}
		
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

	public int getMonitorEnable() {
		return monitorEnable;
	}

	public void setMonitorEnable(int monitorEnable) {
		this.monitorEnable = monitorEnable;
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

	public void addMethod(ServiceMethod sm){
		methods.add(sm);
	}
	
	public Set<ServiceMethod> getMethods(){
		return methods;
	}
	
	/*public static String serviceInterfaceName(String key) {
	    int i = key.indexOf(I_I_SEPERATOR);
	    if(i>0){
	    	return key.substring(0, i);
	    }else {
	    	return key;
	    }
		
	}*/
	
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
		
		return sn+"##"+namespace(ns)+"##"+version(v);
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
		sb.append(serviceName).append(I_I_SEPERATOR);
		
		StringBuffer val = new StringBuffer();
		
		val.append("host").append(KV_SEPERATOR).append(host).append(VAL_SEPERATOR)
		//.append("port").append(KV_SEPERATOR).append(port).append(VAL_SEPERATOR)
		.append("namespace").append(KV_SEPERATOR).append(this.namespace).append(VAL_SEPERATOR)
		.append("version").append(KV_SEPERATOR).append(this.version).append(VAL_SEPERATOR)
		/*.append("time").append(KV_SEPERATOR).append(this.randVal)*/;

		return sb.append(Utils.getIns().encode(val.toString())).toString();
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
		return this.key(ServiceItem.ROOT).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ServiceItem)) {
			return false;
		}
		return this.key(ServiceItem.ROOT).equals(((ServiceItem)obj).key(ServiceItem.ROOT));
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
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
