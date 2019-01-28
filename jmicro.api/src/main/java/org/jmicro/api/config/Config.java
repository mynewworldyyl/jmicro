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
package org.jmicro.api.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:54:53
 */
@Component(value="defaultConfig",lazy=false,level = 0)
public class Config implements IConfigChangeListener{
	
	private final static Logger logger = LoggerFactory.getLogger(Config.class);
	
	private static String RegistryProtocol = "zookeeper";
	private static String RegistryHost = "localhost";
	private static String RegistryPort = "2181";
	
	private static String Host = "";
	
	//全局配置目录
	public static final String CfgDir = Constants.CFG_ROOT +"/config";
	
	//服务注册目录
	public static final String ServiceRegistDir = Constants.CFG_ROOT +"/services";
	
	//全局消息订阅根目录
	public static final String PubSubDir = Constants.CFG_ROOT+"/"+Constants.DEFAULT_PREFIX +"pubsub";
	
	//服务编排相关数据根目录
	public static final String ChoreographyDir = Constants.CFG_ROOT+"/"+Constants.DEFAULT_PREFIX +"Choreography";
	
	//当前启动实例名称
	private static String InstanceName = "";
	
	private static String[] commandArgs = null;
	
	//服务在RAFT中的根目录
	private static String RaftBaseDir = "";
	
	//针对服务配置的目录
	public static String ServiceItemCofigDir = null;

	public static String ServiceConfigDir = null;
	
	public static long systemStartTime = System.currentTimeMillis();
	
	private static String[] BasePackages = {"org.jmicro"};
	
	@Cfg("/basePackages")
	private Collection<String> basePackages = null;
	
	private final Map<String,String> servicesConfig = new HashMap<>();
	
	private final Map<String,String> globalConfig = new HashMap<>();
	
	private static Map<String,String> CommadParams = new HashMap<String,String>();
	
	private Map<String,Set<IConfigChangeListener>> configChangeListeners = new HashMap<>();
	
	private Map<String,Set<IConfigChangeListener>> patternConfigChangeListeners = new HashMap<>();
	
	private IDataOperator dataOperator;
	
	public Config() {}
	
	public static void parseArgs(String[] args) {
		commandArgs = args;
		for(String arg : args){
			if(arg.startsWith("-D")){
				String ar = arg.substring(2);
				if(StringUtils.isEmpty(ar)){
					throw new CommonException("Invalid arg: "+ arg);
				}
				ar = ar.trim();
				if(ar.indexOf("=") > 0){
					String[] ars = ar.split("=");
					CommadParams.put(ars[0].trim(), ars[1].trim());
				} else {
					CommadParams.put(ar, null);
				}
			}
		}
		
		if(CommadParams.containsKey(Constants.INSTANCE_NAME)) {
			InstanceName = CommadParams.get(Constants.INSTANCE_NAME);
		} else {
			InstanceName = System.currentTimeMillis()+"";
		}
		RaftBaseDir = Constants.CFG_ROOT +"/"+InstanceName;
		ServiceConfigDir = RaftBaseDir+"/config";
		ServiceItemCofigDir = RaftBaseDir+"/srvconfig";
				
		if(CommadParams.containsKey(Constants.BASE_PACKAGES_KEY)) {
			String ps = CommadParams.get(Constants.BASE_PACKAGES_KEY);
			if(!StringUtils.isEmpty(ps)){
				String[] pps = ps.split(",");
				setBasePackages0(Arrays.asList(pps));
			}
		}
		
		if(CommadParams.containsKey(Constants.REGISTRY_URL_KEY)) {
			String registry = CommadParams.get("registryUrl");
			if(StringUtils.isEmpty(registry)){
				throw new CommonException("Invalid registry url: "+ registry);
			}
			int index = registry.indexOf("://");
			if(index > 0){
				RegistryProtocol = registry.substring(0,index);
			}else {
				throw new CommonException("Invalid registry url: "+ registry);
			}
			registry = registry.substring(index+3);
			
			if((index = registry.indexOf(":")) > 0){
				String[] hostport = registry.split(":");
				RegistryHost = hostport[0];
				RegistryPort = hostport[1];
			}else {
				throw new CommonException("Invalid registry url: "+ registry);
			}
		}
	}
	
	public static long getSystemStartTime() {
		return systemStartTime;
	}
	
	public static String getInstanceName(){
		if(StringUtils.isEmpty(InstanceName)){
			throw new CommonException("InstanceName cannot be NULL");
		}
		return InstanceName;
	}
	
	public static String getRaftBaseDir(){
		if(StringUtils.isEmpty(RaftBaseDir)){
			throw new CommonException("RaftBaseDir cannot be NULL");
		}
		return RaftBaseDir;
	}

	public static void setBasePackages0(Collection<String>  basePackages) {
		if(basePackages == null || basePackages.size() == 0) {
			return;
		}
		Set<String> set = new HashSet<>();
		for(String p: basePackages) {
			set.add(p.trim());
		}
		for(String p: BasePackages) {
			set.add(p.trim());
		}
		String[] pps = new String[set.size()];
		set.toArray(pps);
		BasePackages = pps;
	}
	
	public void loadConfig(List<IConfigLoader> configLoaders){
		for(IConfigLoader cl : configLoaders){
			cl.setDataOperator(this.dataOperator);
			cl.load(ServiceConfigDir,this.servicesConfig);
			cl.load(CfgDir,this.globalConfig);
			cl.setConfigChangeListener(this);
		}
		init0();
	}
	
	public void createConfig(String value, String path, boolean isGlobal){
		createConfig(value,path,isGlobal,false);
	}
	
	public void createConfig(String value, String path, boolean isGlobal, boolean el){
		if(isGlobal) {
			this.dataOperator.createNode(CfgDir + path, value,el);
		} else {
			this.dataOperator.createNode(ServiceConfigDir + path, value,el);
		}
	}
	
	@Override
	public void configChange(String path, String value) {
		int index = -1;
		if((index = path.indexOf(ServiceConfigDir)) >= 0 ) {
			String subPath = path.substring(index+ServiceConfigDir.length(), path.length());
			this.servicesConfig.put(subPath, value);
			notifiListener(subPath,value);
		}else if((index = path.indexOf(CfgDir)) >= 0 )  {
			String subPath = path.substring(index+CfgDir.length(), path.length());
			this.globalConfig.put(subPath, value);
			notifiListener(subPath,value);
		} else {
			logger.debug("Invalid config :"+path+",value:"+value);
		}
	}
	
	private void notifiListener(String subPath, String value,Set<IConfigChangeListener> l) {
		if(l != null && !l.isEmpty() ){
			for(IConfigChangeListener lis: l){
				lis.configChange(subPath, value);
			}
		}
	}
	
	private void notifiListener(String subPath, String value) {
		Set<IConfigChangeListener> l = this.configChangeListeners.get(subPath);
		this.notifiListener(subPath, value, l);
		
		for(String key : patternConfigChangeListeners.keySet()) {
			if(subPath.startsWith(key)) {
				this.notifiListener(subPath, value, patternConfigChangeListeners.get(key));
			}
		}
	}
	
	public void removeConfigListener(String key,IConfigChangeListener lis){
		if(key == null) {
			return;
		}
		Set<IConfigChangeListener> l = null;				
		if(key.endsWith("*")) {
			key = key.substring(0,key.length()-1);
			l = this.patternConfigChangeListeners.get(key);
		} else {
			l = this.configChangeListeners.get(key);
		}
		if(l != null && !l.isEmpty()){
			l.remove(lis);
		}
	}

	public void addConfigListener(String key,IConfigChangeListener lis){
		if(key == null) {
			return;
		}
		if(key.endsWith("*")) {
			key = key.substring(0,key.length()-1);
			Set<IConfigChangeListener> l = this.patternConfigChangeListeners.get(key);
			if(l == null ){
				this.patternConfigChangeListeners.put(key, l = new HashSet<IConfigChangeListener>());
			}
			l.add(lis);
		} else {
			Set<IConfigChangeListener> l = this.configChangeListeners.get(key);
			if(l == null ){
				this.configChangeListeners.put(key, l = new HashSet<IConfigChangeListener>());
			}
			l.add(lis);
		}
	}

	//@JMethod("init")
	public void init0(){
		//命令行参数具有最高优先级
		//params.putAll(CommadParams);
		if(CommadParams.containsKey(Constants.BIND_IP)) {
	        Host = getCommandParam(Constants.BIND_IP);
		}else if(this.servicesConfig.containsKey(Constants.BIND_IP)) {
			 Host = this.getServiceParam(Constants.BIND_IP);
		}else if(this.globalConfig.containsKey(Constants.BIND_IP)) {
			 Host = this.getGlobalServiceParam(Constants.BIND_IP);
		} else {
			List<String> ips = Utils.getIns().getLocalIPList();
	        if(ips.isEmpty()){
	        	throw new CommonException("IP not found");
	        }
	        Host = ips.get(0);
		}
	}
	
	public static String getRegistryHost() {
		return RegistryHost;
	}
	
	public static String getRegistryPort() {
		return RegistryPort;
	}
	
	public static String getHost() {
		return Host;
	}
	
	public static boolean isClientOnly() {
		return CommadParams.containsKey(Constants.CLIENT_ONLY);
	}
	
	public static boolean isServerOnly() {
		return CommadParams.containsKey(Constants.SERVER_ONLY);
	}
	
	public static <T> T getCommandParam(String key,Class<T> type,T defalutValue) {
		return getValue(CommadParams.get(key),type,defalutValue);
	}
	
	public static String getCommandParam(String key) {
		return CommadParams.get(key);
	}
	
	public String getServiceParam(String key) {
		return servicesConfig.get(key);
	}
	
	public String getGlobalServiceParam(String key) {
		return globalConfig.get(key);
	}
	
	public static <T> T getEnvParam(String key,Class<T> type,T defalutValue) {
		return getValue(getEnvParam(key),type,defalutValue);
	}
	
	public static String getEnvParam(String key) {
		if(System.getProperty(key) != null) {
			return System.getProperty(key);
		}
		return System.getenv(key);
	}
	
	public <T> T getServiceParam(String key,Class<T> type,T defalutValue) {
		return getValue(getServiceParam(key),type,defalutValue);
	}
	
	public <T> T getGlobalParam(String key,Class<T> type,T defalutValue) {
		return getValue(getGlobalServiceParam(key),type,defalutValue);
	}
	
	public static String[]  getBasePackages() {
		return BasePackages;
	}
	
	public void setBasePackages(Collection<String>  basePackages) {
		 setBasePackages0(basePackages);
	}	
	
	public Integer getInt(String key,int defautl){
		return getValue(getValue(key),Integer.TYPE,defautl);
	}
	
	public String getString(String key,String defautl){
		return getValue(getValue(key),String.class,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return getValue(getValue(key),Boolean.TYPE,defautl);
	}

	public Float getFloat(String key,Float defautl){
		return getValue(getValue(key),Float.TYPE,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return getValue(getValue(key),Double.TYPE,defautl);
	}
	
	/**
	 * 3个优先级，从高到底分别为，命令行参数，服务级配置参数，全局配置参数, 系统环境变量
	 * 当命令行参数匹配成功时，不会再找其他参数，否则找服务级参数，最后找全局配置参数，都匹配不到，返回空。
	 * 1。命令行参数中查找，如果找不到，进入2
	 * 2。优先在服务配置中查找配置，如果找不到，进入3
	 * 3。在全局配置中查找，如果找不到，进入4
	 * 4。在环境系统环境变量中找，如果没找到，返回NULL
	 * @param key
	 * @return
	 */
	private String getValue(String key){
		
		String v = getCommandParam(key);
		
		if(v == null){
			v = this.getServiceParam(key);
		}
		
		if(v == null){
			v = this.getGlobalServiceParam(key);
		}
		
		/*if(v == null){
			v = System.getProperty(key);
		}*/
		
		if(v == null){
			v = getEnvParam(key);
		}
		
		return v;
	}
	
	public static <T> T getValue(String str,Class<T> type, T defaultVal) {
		
		if(StringUtils.isEmpty(str)){
			return defaultVal;
		}
		Object v = null;
		if(type == Boolean.TYPE || type == Boolean.class){
			v = Boolean.parseBoolean(str);
		}else if(type == Short.TYPE || type == Short.class){
			v = Short.parseShort(str);
		}else if(type == Integer.TYPE || type == Integer.class){
			v = Integer.parseInt(str);
		}else if(type == Long.TYPE || type == Long.class){
			v = Long.parseLong(str);
		}else if(type == Float.TYPE || type == Float.class){
			v = Float.parseFloat(str);
		}else if(type == Double.TYPE || type == Double.class){
			v = Double.parseDouble(str);
		}else if(type == Byte.TYPE || type == Byte.class){
			v = Byte.parseByte(str);
		} else {
			v = str;
		}	
		return (T)v;
	}

	public IDataOperator getDataOperator() {
		return dataOperator;
	}

	public void setDataOperator(IDataOperator dataOperator) {
		if(dataOperator == null) {
			throw new CommonException("dataOperator cannot be null");
		}
		this.dataOperator = dataOperator;
		if(!dataOperator.exist(Config.CfgDir)) {
			dataOperator.createNode(Config.CfgDir, "", false);
			//dataOperator.createNode(Config.CfgDir+"/val", "_v", false);
		}
		
		if(!dataOperator.exist(Config.ServiceConfigDir)) {
			dataOperator.createNode(Config.ServiceConfigDir, "", false);
			//dataOperator.createNode(Config.ServiceConfigDir+"/val", "_v", false);
		}
	}

	public Map<String,String> getParamByPattern(String key) {
		
		Map<String,String> result = new HashMap<>();
		
		if(key.endsWith("*")) {
			key = key.substring(0,key.length()-1);
		}
		
		for(Map.Entry<String, String> e : System.getenv().entrySet()) {
			if(e.getKey().startsWith(key)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		
		Enumeration<?> enus = System.getProperties().propertyNames();
		for(;enus.hasMoreElements();) {
			String k = (String)enus.nextElement();
			if(k.startsWith(key)) {
				result.put(k, System.getProperty(k));
			}
		}
		
		for(Map.Entry<String, String> e : this.globalConfig.entrySet()) {
			if(e.getKey().startsWith(key)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		
		for(Map.Entry<String, String> e : this.servicesConfig.entrySet()) {
			if(e.getKey().startsWith(key)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		
		for(Map.Entry<String, String> e : CommadParams.entrySet()) {
			if(e.getKey().startsWith(key)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		
		return result;
	}
	
}
