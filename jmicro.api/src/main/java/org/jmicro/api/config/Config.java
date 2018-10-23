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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:54:53
 */
@Component(value="defaultConfig",lazy=false)
public class Config implements IConfigChangeListener{
	
	private final static Logger logger = LoggerFactory.getLogger(Config.class);
	
	private static String RegistryProtocol = "zookeeper";
	private static String RegistryHost = "localhost";
	private static String RegistryPort = "2180";
	
	//全局配置目录
	public static final String CfgDir = Constants.CFG_ROOT +"/config";
	
	//服务注册目录
	public static final String ServiceRegistDir = Constants.CFG_ROOT +"/services";
	
	//当前启动实例名称
	private static String InstanceName = "";
	
	private static String[] commandArgs = null;
	
	//服务在RAFT中的根目录
	private static String RaftBaseDir = "";
	
	//针对服务配置的目录
	public static String ServiceCofigDir = null;

	private static String[] BasePackages = {"org.jmicro"};
	
	@Cfg("/basePackages")
	private Collection<String> basePackages = null;
	
	private final Map<String,String> servicesConfig = new HashMap<>();
	
	private final Map<String,String> globalConfig = new HashMap<>();
	
	private static Map<String,String> CommadParams = new HashMap<String,String>();
	
	private Map<String,Set<IConfigChangeListener>> configChangeListeners = new HashMap<>();
	
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
		ServiceCofigDir = RaftBaseDir+"/config";
				
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
			cl.load(ServiceCofigDir,this.servicesConfig);
			cl.load(CfgDir,this.globalConfig);
			cl.setConfigChangeListener(this);
		}
		init();
	}
	
	@Override
	public void configChange(String path, String value) {
		int index = -1;
		if((index = path.indexOf(ServiceCofigDir)) >= 0 ) {
			String subPath = path.substring(index+ServiceCofigDir.length(), path.length());
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
	
	private void notifiListener(String subPath, String value) {
		Set<IConfigChangeListener> l = this.configChangeListeners.get(subPath);
		if(l == null || l.isEmpty() ){
			return;
		}
		for(IConfigChangeListener lis: l){
			lis.configChange(subPath, value);
		}
	}

	public void addConfigListener(String key,IConfigChangeListener lis){
		Set<IConfigChangeListener> l = this.configChangeListeners.get(key);
		if(l == null ){
			this.configChangeListeners.put(key, l = new HashSet<IConfigChangeListener>());
		}
		l.add(lis);
	}

	//@JMethod("init")
	public void init(){
		//命令行参数具有最高优先级
		//params.putAll(CommadParams);
	}
	
	public static String getRegistryHost() {
		return RegistryHost;
	}
	
	public static String getRegistryPort() {
		return RegistryPort;
	}
	
	public static boolean isClientOnly() {
		return CommadParams.containsKey(Constants.CLIENT_ONLY);
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
	
	private String getValue(String key){
		String v = CommadParams.get(key);
		if(v == null){
			v = this.servicesConfig.get(key);
		}
		if(v == null){
			v = this.globalConfig.get(key);
		}
		return v;
	}
	
	public static <T> T getValue(String str,Class<T> type, T defaultVal) {
		
		if(StringUtils.isEmpty(str)){
			return defaultVal;
		}
		Object v = null;
		if(type == Boolean.TYPE){
			v = Boolean.parseBoolean(str);
		}else if(type == Short.TYPE){
			v = Short.parseShort(str);
		}else if(type == Integer.TYPE){
			v = Integer.parseInt(str);
		}else if(type == Long.TYPE){
			v = Long.parseLong(str);
		}else if(type == Float.TYPE){
			v = Float.parseFloat(str);
		}else if(type == Double.TYPE){
			v = Double.parseDouble(str);
		}else if(type == Byte.TYPE){
			v = Byte.parseByte(str);
		} else {
			v = str;
		}	
		return (T)v;
	}
}
