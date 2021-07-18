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
package cn.jmicro.api.registry;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import lombok.Data;

/**
 *  单位时间处理速度，类似QPS，但是间单位可定制
 *  时间单位：H：小时， M： 分钟， S：秒 ，MS：毫少，NS：纳秒
 *  如90S，表示每秒钟处理90个请求，20M，表示每分钟处理20个请求，数字只能为整数，不能是小数
 *  
 *  服务标识：服务级标识，服务方法标识，服务实例标识
 *  服务级标识 同服务名称，服务命名空间，服务版本组成 , 参考 {@link UniqueServiceKeyJRso}
 *  服务方法标识 同服务标识+服务方法名称+服务参数级成，参考  {@link UniqueServiceMethodKeyJRso}
 *  服务实例标识一个具体的运行实例，分为服务实例，服务方法实例，在服务级标识及服务方法标识 基础上加实例名称组成，
 *  运行过程中又可由IP+PORT标识。
 *  
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:29
 */
@SO
@IDStrategy(value=10)
@Data
public final class ServiceItemJRso implements Comparable<ServiceItemJRso>{

	private final static Logger logger = LoggerFactory.getLogger(ServiceItemJRso.class);
	
	/*static {
		logger.info("测试类型加堞信息"+ServiceItemJRso.class.getClassLoader().getClass().getName());
	}*/
	public static final String FILE_SEPERATOR="/";
	
	public static final String I_I_SEPERATOR="####";
	
	public static final String KV_SEPERATOR="=";
	
	public static final String VAL_SEPERATOR="&";
	
	public static final String KEY_SEPERATOR="##";
	
	private static final Random rand = new Random();
	
	//-1 use system default value, 0 disable, 1 enable
	//@JField(persistence=true)
	
	private UniqueServiceKeyJRso key;
	
	private String desc;
	
	//private String instanceName="";
	
	//private int code = 0;
	
	//开启debug模式
	private int debugMode = 0;
		
	private int monitorEnable = 0;
	
	private byte logLevel = MC.LOG_ERROR;
	
	//基本时间单位
	private String baseTimeUnit = Constants.TIME_MILLISECONDS;
	
	//由baseTimeUnit计算出来的JVM极别的时间单位，方便使用
	private transient TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	
	//统计服务数据基本时长，单位同baseTimeUnit确定 @link SMethod
	private long timeWindow = 180000;
	
	private int slotSize = 60;
	
	//采样统计数据周期，单位由baseTimeUnit确定
	private long checkInterval = -1;
		
	private Set<ServerJRso> servers = new HashSet<ServerJRso>();
	
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
	
	//configurable from mng UI
	private boolean showFront = true;
	
	private boolean external = false;
	
	private Set<String> limit2Packages = new HashSet<>();
	
	private Set<ServiceMethodJRso> methods = new HashSet<>();
	
	private long createdTime = TimeUtils.getCurTime();
	
	private  transient long loadTime = TimeUtils.getCurTime();
	
	public ServiceItemJRso() {}
	
	public ServiceItemJRso(String val) {
		//this.parseKey(key);
		this.parseVal(val);
	}
	
	public ServerJRso getServer(String transport) {
		for(ServerJRso s: servers) {
			if(s.getProtocol().equals(transport)){
				return s;
			}
		}
		return null;
	}
	
    @Override
	public int compareTo(ServiceItemJRso o) {
		String key = this.fullStringKey();
		String keyo = o.fullStringKey();
		return key.compareTo(keyo);
	}

	public void formPersisItem(ServiceItemJRso p){
		this.monitorEnable = p.monitorEnable;
		this.desc = p.desc;
		
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
		//this.clientId = p.clientId;
		//this.code = p.getCode();
		this.debugMode = p.getDebugMode();
		this.servers.clear();
		this.servers.addAll(p.getServers());
		this.impl = p.getImpl();
		this.external = p.external;
		this.showFront = p.showFront;
		this.createdTime = p.createdTime;
		
		//this.actName = p.actName;
		
		this.loadTime = p.loadTime;
		
		//this.clientId = p.clientId;
		//this.createdBy = p.createdBy;
		
		//this.insId = p.insId;
		
		if(!p.limit2Packages.isEmpty()) {
			this.limit2Packages.addAll(p.limit2Packages);
		}
		
		for(ServiceMethodJRso sm : p.getMethods()){
			ServiceMethodJRso nsm = this.getMethod(sm.getKey().getMethod(), sm.getKey().getParamsStr());
			if(nsm != null){
				nsm.formPersisItem(sm);
				//nsm.setBreakingRule(new BreakRule());
				//nsm.getBreakingRule().from(sm.getBreakingRule());
			}
		}
	}

	public void addMethod(ServiceMethodJRso sm){
		methods.add(sm);
	}
	
	
	//服务标识，服务名，名称空间，版本，3元组坐标
	public String serviceKey() {
	   return UniqueServiceKeyJRso.serviceName(this.getKey().getServiceName(), this.getKey().getNamespace(),
			   this.getKey().getVersion());
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
			ServiceMethodJRso sm = new ServiceMethodJRso();
			sm.fromJson(m);
			this.methods.add(sm);
		}
	}
	
	public ServiceMethodJRso getMethod(String methodName,String mkStr){
		Set<ServiceMethodJRso> ms = new HashSet<>();
		for(ServiceMethodJRso sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod())){
				ms.add(sm);
			}
		}
		if(ms.isEmpty()) {
			return null;
		}
		
		if(ms.size() == 1) {
			return ms.iterator().next();
		}
		
		if(StringUtils.isEmpty(mkStr)) {
			throw new CommonException("Service method cannot be found with null params service: "+
					this.getKey().getServiceName()+" , M: "+methodName);
		}
		
		for(ServiceMethodJRso sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod()) && mkStr.equals(sm.getKey().getParamsStr()) ){
				return sm;
			}
		}
		
		Class<?> paramClazzes[] = UniqueServiceMethodKeyJRso.paramsClazzes(mkStr);
		for(ServiceMethodJRso sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod())){
				Class<?> paramClazzes1[] = UniqueServiceMethodKeyJRso.paramsClazzes(sm.getKey().getParamsStr());
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
	
	public ServiceMethodJRso getMethod(String methodName,Object[] args){
		String mkStr = UniqueServiceMethodKeyJRso.paramsStr(args);
		return getMethod(methodName, mkStr);
	}
	
	public ServiceMethodJRso getMethod(String methodName,Class<?>[] args){
		String mkStr = UniqueServiceMethodKeyJRso.paramsStr(args);
		return getMethod(methodName, mkStr);
	}
	
	public ServiceMethodJRso getMethod(String methodName){
		for(ServiceMethodJRso sm : this.methods){
			if(methodName.equals(sm.getKey().getMethod())){
				return sm;
			}
		}
		return null;
	}
	
	//服务实例标识,带上实例名和主机IP
	public String path(String root){
		return this.key.path(root);
	}
	
	public static String pathForKey(String root,String key){
		StringBuffer sb = new StringBuffer(Config.getRaftBasePath(root));
		sb.append(FILE_SEPERATOR);
		sb.append(key);
		return sb.toString();
	}
	
	public String getHost() {
		return this.key.getHost();
	}
	
	public String getPort() {
		return this.key.getPort();
	}
	
	public String getInsName() {
		return this.key.getInstanceName();
	}
	
	public Integer getInsId() {
		return this.key.getInsId();
	}
	
	public String getActName() {
		return this.key.getActName();
	}
	
	public Integer getActId() {
		return this.key.getCreatedBy();
	}
	
	public String toSnv() {
		return key.toSnv();
	}
	
	public String fullStringKey() {
		return key.fullStringKey();
	}
	
	public String serviceID() {
		return key.serviceID();
	}
	
	public Integer getClientId() {
		return key.getClientId();
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ServiceItemJRso)) {
			return false;
		}
		return this.key.equals(((ServiceItemJRso)obj).getKey());
	}

	public Set<String> getLimit2Packages() {
		return limit2Packages;
	}

	public void setLimit2Packages(Set<String> limit2Packages) {
		this.limit2Packages = limit2Packages;
	}
	
}