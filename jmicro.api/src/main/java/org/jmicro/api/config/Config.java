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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.exception.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:54:53
 */
@Component(value="defaultConfig",lazy=false)
public class Config {
	
	private static String RegistryProtocol = "zookeeper";
	private static String RegistryHost = "localhost";
	private static String RegistryPort = "2180";
	
	//private static String RegistryUrl = "zookeeper://localhost:2180";
	
	private static String[] commandArgs = null;
	
	private static String ConfigRoot = "/jmicro/config";
	private static String[] BasePackages = {"org.jmicro"};
	
	private static Map<String,String> CommadParams = new HashMap<String,String>();
	
	@Cfg("/basePackages")
	private Collection<String> basePackages = null;
	
	private static final Map<String,String> params = new HashMap<>();
	
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
		
		if(CommadParams.containsKey(Constants.CONFIG_ROOT_KEY)) {
			ConfigRoot = CommadParams.get(Constants.CONFIG_ROOT_KEY);
		}
		
		if(CommadParams.containsKey(Constants.BASE_PACKAGES_KEY)) {
			String ps = CommadParams.get(Constants.BASE_PACKAGES_KEY);
			if(!StringUtils.isEmpty(ps)){
				String[] pps = ps.split(",");
				//Arrays.asList(pps);
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
		//RegistryUrl = RegistryProtocol+"://"+RegistryHost+":"+RegistryPort;
		//new URL(RegistryProtocol,RegistryHost,Integer.parseInt(RegistryPort));
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
	
	//@JMethod("init")
	public void init(){
		//命令行参数具有最高优先级
		params.putAll(CommadParams);
	}
	
	public static String getConfigRoot() {
		return ConfigRoot;
	}
	
	public static String getRegistryHost() {
		return RegistryHost;
	}
	
	public static String getRegistryPort() {
		return RegistryPort;
	}
	
	public static String[]  getBasePackages() {
		return BasePackages;
	}
	
	public void setBasePackages(Collection<String>  basePackages) {
		 setBasePackages0(basePackages);
	}
	
	public Map<String,String> getParams(){
		return params;
	}	
	
	public Integer getInt(String key,int defautl){
		return getValue(params,Integer.TYPE,key,defautl);
	}
	
	public String getString(String key,String defautl){
		return getValue(params,String.class,key,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return getValue(params,Boolean.TYPE,key,defautl);
	}
	
	
	public Float getFloat(String key,Float defautl){
		return getValue(params,Float.TYPE,key,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return getValue(params,Double.TYPE,key,defautl);
	}
	
	public static <T> T getValue( Map<String,String> params,Class<T> type,String key, T defaultVal) {
		String str = params.get(key);
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
