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
package cn.jmicro.api.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.ClassScannerUtils;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.service.DefaultServiceInstanceNameProvider;
import cn.jmicro.api.service.IServiceInstanceNameGenerator;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:54:53
 */
@Component(value="defaultConfig",lazy=false,level = 0)
public class Config implements IConfigChangeListener{
	
	private final static Logger logger = LoggerFactory.getLogger(Config.class);
	
	private static String RegistryProtocol = "zookeeper";
	private static String RegistryHost = "localhost:2181";
	//private static String RegistryPort = "2181";
	
	private static String Host = "";
	
	public static final String 	BASE_DIR = Constants.CFG_ROOT +"/"+Constants.DEFAULT_PREFIX;
	
	//全局配置目录,配置信息由全局配置及实例级配置组成
	public static final String CfgDir = BASE_DIR + "/config";
	//全局配置
	public static final String GROBAL_CONFIG = BASE_DIR + "/grobalConfig";
	
	//实例相关信息
	public static final String InstanceDir = BASE_DIR + "/instance";
	
	//服务实例注册目录,每个运行实例一个服务
	public static final String ServiceRegistDir = BASE_DIR+"/serviceItems";
	
	//全局服务注册目录,全部运行实例共享一个服务注册
	public static final String GrobalServiceRegistDir = BASE_DIR + "/grobalServiceItems";
	
	//全局消息订阅根目录
	public static final String PubSubDir = BASE_DIR +"/pubsub";
	
	//服务编排相关数据根目录
	public static final String ChoreographyDir = BASE_DIR +"/choreography";
	
	public static final String MonitorDir = BASE_DIR +"/monitorDir";
	
	public static final String CurCustomTypeCodeDir = MonitorDir +"/curCustomTypeCode";
	public static final String MonitorTypeConfigDir = MonitorDir +"/monitorTypeConfig";
	public static final String MonitorTypesDir = MonitorDir+"/monitorTypes";
	public static final String MonitorServiceMethodTypesDir = MonitorDir+"/serviceMethodTypes";
	public static final String NamedTypesDir = MonitorDir +"/namedTypesDir";
	
	//当前启动实例名称
	private static String InstanceName = "";
	
	private static String[] commandArgs = null;
	
	private static String LocalDataDir = "";
	
	//服务在RAFT中的根目录
	//private static String RaftBaseDir = "";
	
	//服务实例配置目录,对应服务运行实例
	public static String ServiceConfigDir = null;
	
	public static long systemStartTime = System.currentTimeMillis();
	
	private static String[] BasePackages = {"cn.jmicro"};
	
	private final Map<String,String> servicesConfig = new HashMap<>();
	
	private final Map<String,String> globalConfig = new HashMap<>();
	
	private static Map<String,String> CommadParams = new HashMap<>();
	
	private static Map<String,String> extConfig = new HashMap<>();
	
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
				String key;
				String val;
				
				if(ar.indexOf("=") > 0){
					String[] ars = ar.split("=");
					key = ars[0].trim();
					val = ars[1].trim();
				} else {
					key = ar;
					val = null;
				}
				
				if(logger.isDebugEnabled()) {
					logger.debug("{}={}",key,val);
				}
				
				CommadParams.put(key,val);
			}
		}
		
		loadExtConfig();
		
		if(contain(Constants.BASE_PACKAGES_KEY,CommadParams)) {
			String ps = CommadParams.get(Constants.BASE_PACKAGES_KEY);
			if(!StringUtils.isEmpty(ps)){
				String[] pps = ps.split(",");
				setBasePackages0(Arrays.asList(pps));
			}
		}
		
		String registry = getCommandParam(Constants.REGISTRY_URL_KEY);
		if(StringUtils.isEmpty(registry)) {
			registry = getEnvParam(Constants.REGISTRY_URL_KEY);
		}
		
		if(StringUtils.isEmpty(registry)) {
			registry = getExtParam(Constants.REGISTRY_URL_KEY);
		}
		
		if(StringUtils.isEmpty(registry)) {
			registry = "zookeeper://127.0.0.1:2181";
		}
		

		RegistryHost = null;
		String[] arr = registry.split(",");
		for(String one : arr) {
			int index = one.indexOf("://");
			if(index > 0){
				RegistryProtocol = one.substring(0,index);
			}else {
				throw new CommonException("Invalid registry url: "+ registry);
			}
			one = one.substring(index+3);
			if(RegistryHost == null) {
				RegistryHost = one;
			}else {
				RegistryHost = "," + one;
			}
		}

		String dataDir = getCommandParam(Constants.LOCAL_DATA_DIR);
		if(StringUtils.isEmpty(dataDir)) {
			dataDir = System.getProperty("user.dir")+ File.separatorChar + "data";
		}
		
		File dataDirFile = new File(dataDir);
		if(!dataDirFile.exists()) {
			if(!dataDirFile.mkdirs()) {
				throw new CommonException("Fail to create data directory ["+dataDir+"] ");
			}
		}
		
		if(dataDirFile.isFile()) {
			throw new CommonException("Data Dir ["+dataDir+"] cannot be a file");
		}
		
		CommadParams.put(Constants.LOCAL_DATA_DIR, dataDir);
	
		
	}
	
	private void initDataDir() {
		String localDataDir = CommadParams.get(Constants.LOCAL_DATA_DIR);
		File f = new File(localDataDir,InstanceName);
		if(!f.exists()) {
			f.mkdir();
		}
		CommadParams.put(Constants.INSTANCE_DATA_DIR, f.getAbsolutePath());
	}
	
	private void initInstanceName() {
		
		String insName = getCommandParam(Constants.INSTANCE_NAME);
		
		if(StringUtils.isNotEmpty(insName)) {
			InstanceName = insName;
			initDataDir();
			return;
		} 
		
		String insGenClass = getCommandParam(Constants.INSTANCE_NAME_GEN_CLASS);
		if(StringUtils.isEmpty(insGenClass)) {
			insGenClass = getExtParam(Constants.INSTANCE_NAME_GEN_CLASS);
		}
		
		if(StringUtils.isEmpty(insGenClass)) {
			insGenClass = DefaultServiceInstanceNameProvider.class.getName();
		}
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if(cl == null) {
			cl = Config.class.getClassLoader();
		}
		
		try {
			Class<?> cls = cl.loadClass(insGenClass);
			if(!IServiceInstanceNameGenerator.class.isAssignableFrom(cls)) {
				throw new CommonException("Class ["+ insGenClass+"] not a implementation of ["+IServiceInstanceNameGenerator.class.getName()+"]" );
			}
			IServiceInstanceNameGenerator sin = (IServiceInstanceNameGenerator)cls.newInstance();
			InstanceName = sin.getInstanceName(dataOperator,this);
			initDataDir();
		} catch (ClassNotFoundException e) {
			throw new CommonException("IServiceInstanceNameGenerator imple class ["+ insGenClass+"] not found" );
		} catch (InstantiationException | IllegalAccessException e) {
			throw new CommonException("",e);
		}
	}

	private static void loadExtConfig() {
		List<String> configFiles = ClassScannerUtils.getClasspathResourcePaths("META-INF/jmicro", "*.properties");
		Map<String,String> params = new HashMap<>();
		ClassLoader cl = Config.class.getClassLoader();
		Set<String> set = new HashSet<>();
		for(String f : configFiles) {
			InputStream is = null;
			try {
				is = cl.getResourceAsStream(f);
				Properties p = new Properties();
				p.load(is);
				logger.info("Path:{}",f);
				for(Object k : p.keySet()) {
					String key = k.toString();
					String v = p.getProperty(key);
					logger.debug("****{}={}", key, p.getProperty(key, ""));
					if(Constants.BASE_PACKAGES_KEY.equals(key)) {
						String ps = p.getProperty(key, null);
						if(!StringUtils.isEmpty(ps)){
							String[] pps = ps.split(",");
							set.addAll(Arrays.asList(pps));
						}
						logger.info("basePackages:{}",ps);
						continue;
					}
					
					if(params.containsKey(key)) {
						logger.warn("Repeat config KEY:"+key+",params:"+params.get(key)+",config:"+p.get(k));
						//throw new CommonException("Repeat config KEY:"+key+",params:"+params.get(key)+",config:"+p.get(k));
					}
					
					if(!key.startsWith("/")) {
						key = "/" + key;
					}
					key = key.replaceAll("\\.", "/");
					
					params.put(key, v);
				}
				logger.debug("End config {}******************************************",f);
			} catch (IOException e) {
				logger.error("loadExtConfig",e);
			} finally {
				if(is != null) {
					try {
						is.close();
					} catch (IOException e) {
						logger.error("loadExtConfig close",e);
					}
				}
			}
		}
		if(!set.isEmpty()) {
			setBasePackages0(set);
		}
		
		if(!params.isEmpty()) {
			extConfig.putAll(params);
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
	
	public synchronized static void setBasePackages0(Collection<String>  basePackages) {
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
	
	public void loadConfig(Set<IConfigLoader> configLoaders){
		
		//加载全局配置
		for(IConfigLoader cl : configLoaders){
			cl.setDataOperator(this.dataOperator);
			cl.load(GROBAL_CONFIG,this.globalConfig);
			cl.setConfigChangeListener(this);
		}
		
		initInstanceName();
		
		String path = Config.InstanceDir +"/"+Config.getInstanceName()+"_ipPort";
		if(dataOperator.exist(path)) {
			dataOperator.setData(path, "");
		}
		
		ServiceConfigDir = CfgDir+"/" + InstanceName;
		
		//   /jmicro目录
		if(!dataOperator.exist(Constants.CFG_ROOT)) {
			dataOperator.createNodeOrSetData(Constants.CFG_ROOT, "", false);
		}
		
		//   /jmicro/JMICRO
		if(!dataOperator.exist(Config.BASE_DIR)) {
			dataOperator.createNodeOrSetData(Config.BASE_DIR, "", false);
		}
		
		 //   /jmicro/JMICRO/config 目录
		if(!dataOperator.exist(Config.CfgDir)) {
			dataOperator.createNodeOrSetData(Config.CfgDir, "", false);
		}
		
	    //   /jmicro/JMICRO/config/{实例名称}  目录
		if(!dataOperator.exist(Config.ServiceConfigDir)) {
			dataOperator.createNodeOrSetData(Config.ServiceConfigDir, "", false);
		}
		
		//服务注册目录
		if(!dataOperator.exist(Config.ServiceRegistDir)) {
			dataOperator.createNodeOrSetData(Config.ServiceRegistDir, "", false);
		}
		
		//全局服务注册目录
		if(!dataOperator.exist(Config.GrobalServiceRegistDir)) {
			dataOperator.createNodeOrSetData(Config.GrobalServiceRegistDir, "", false);
		}
		
		//加载服务级配置
		for(IConfigLoader cl : configLoaders){
			cl.load(ServiceConfigDir,this.servicesConfig);
		}
		
		init0();
	}
	
	public void createConfig(String value, String path, boolean isGlobal){
		createConfig(value,path,isGlobal,false);
	}
	
	public void createConfig(String value, String path, boolean isGlobal, boolean el){
		if(isGlobal) {
			this.globalConfig.put(path, value);
			this.dataOperator.createNodeOrSetData(GROBAL_CONFIG + path, value,el);
		} else {
			this.servicesConfig.put(path, value);
			this.dataOperator.createNodeOrSetData(ServiceConfigDir + path, value,el);
		}
	}
	
	@Override
	public void configChange(String path, String value) {
		int index = -1;
		if((index = path.indexOf(ServiceConfigDir)) >= 0 ) {
			String subPath = path.substring(index + ServiceConfigDir.length(), path.length());
			this.servicesConfig.put(subPath, value);
			notifiListener(subPath,value);
		}else if((index = path.indexOf(GROBAL_CONFIG)) >= 0 )  {
			String subPath = path.substring(index + GROBAL_CONFIG.length(), path.length());
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
		
		LocalDataDir = this.getValue(Constants.LOCAL_DATA_DIR,true);
		
		Host = getValue(Constants.BIND_IP,false);
		String exportHttpIP = getValue(Constants.ExportHttpIP,false);
		String exportSocketIP = getValue(Constants.ExportSocketIP,false);
		
		//命令行参数具有最高优先级
		//params.putAll(CommadParams);
		if(StringUtils.isEmpty(Host)) {
			if(StringUtils.isNotEmpty(exportSocketIP) || StringUtils.isNotEmpty(exportHttpIP)) {
				if(!exportSocketIP.equals(Host) && !exportHttpIP.equals(Host) ) {
					Host = "0.0.0.0";
				}
			} else {
				List<String> ips = Utils.getIns().getLocalIPList();
		        if(ips.isEmpty()){
		        	throw new CommonException("IP not found");
		        }
		        Host = ips.get(0);
			}
		}
	}
	
	public static String getRegistryHost() {
		return RegistryHost;
	}
	
	/*public static String getRegistryPort() {
		return RegistryPort;
	}*/
	
	public static String getHost() {
		return Host;
	}
	
	public static boolean isClientOnly() {
		return contain(Constants.CLIENT_ONLY,CommadParams);
	}
	
	public static boolean isServerOnly() {
		return contain(Constants.SERVER_ONLY,CommadParams);
	}
	
	private static boolean contain(String key,Map<String,String> params) {
		boolean f = params.containsKey(key);
		if(!f) {
			f = params.containsKey(key);
		}
		return f;
	}
	
	public static <T> T getCommandParam(String key,Class<T> type,T defalutValue) {
		return getValue(CommadParams.get(key),type,defalutValue);
	}
	
	public static String getExtParam(String key) {
		return getMapVal(key,extConfig,null);
	}
	
	private static String getKey(String key) {
		if(key == null) {
			return null;
		}
		if(key.startsWith("/")) {
			key = key.substring(1).replace("/", "\\.");
		} else {
			key = "/" + key;
			key = key.replace("\\.", "/");
		}
		return key;
	}
	
	private static String getMapVal(String key,Map<String,String> map, String defalutValue) {
		String v =  map.get(key);
		if(StringUtils.isEmpty(v)) {
			v = map.get(getKey(key));
		}
		if(StringUtils.isEmpty(v)) {
			return defalutValue;
		} else {
			return v;
		}
	}
	
	public static String getCommandParam(String key) {
		return getMapVal(key,CommadParams,null);
	}
	
	public String getServiceParam(String key) {
		return getMapVal(key,servicesConfig,null);
	}
	
	public String getGlobalServiceParam(String key) {
		return getMapVal(key,globalConfig,null);
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
		return getValue(getValue(key,true),Integer.TYPE,defautl);
	}
	
	public String getString(String key,String defautl){
		return getValue(getValue(key,true),String.class,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return getValue(getValue(key,true),Boolean.TYPE,defautl);
	}

	public Float getFloat(String key,Float defautl){
		return getValue(getValue(key,true),Float.TYPE,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return getValue(getValue(key,true),Double.TYPE,defautl);
	}
	
	/**
	 * 3个优先级，从高到底分别为，命令行参数，服务级配置参数，全局配置参数, 系统环境变量
	 * 当命令行参数匹配成功时，不会再找其他参数，否则找服务级参数，最后找全局配置参数，都匹配不到，返回空。
	 * 1。命令行参数中查找，如果找不到，进入2
	 * 2。优先在服务配置中查找配置，如果找不到，进入3
	 * 3。在META-INF/jmicro/*.properties中查找
	 * 4。在全局配置中查找，如果找不到，进入4
	 * 5。在环境系统环境变量中找，如果没找到，返回NULL
	 * @param key
	 * @param useGlobalService 是否可以使用全局配置，true表示可以
	 * @return
	 */
	private String getValue(String key,boolean useGlobalService){
		
		String v = getCommandParam(key);
		
		if(v == null){
			v = this.getServiceParam(key);
		}
		
		if(v == null && useGlobalService){
			v = this.getGlobalServiceParam(key);
		}
		
		/*if(v == null){
			v = System.getProperty(key);
		}*/
		
		if(v == null){
			v = getEnvParam(key);
		}
		
		if(v == null){
			v = getExtParam(key);
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
		

		for(Map.Entry<String, String> e : extConfig.entrySet()) {
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
