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
import java.util.Collections;
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
import cn.jmicro.api.JMicro;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.utils.TimeUtils;
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
public class Config{
	
	private final static Logger logger = LoggerFactory.getLogger(Config.class);
	
	private static String RegistryProtocol = "zookeeper";
	
	private static String RegistryHost = "localhost:2181";
	
	//private static String RegistryPort = "2181";
	
	//private static String Host = "";
	
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
	
	public static final String AccountDir = BASE_DIR + "/accounts";
	
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
	private static String instanceName = "";
	
	//作为服务名称空间
	private static String instanceRrefix = "";
	
	private static String[] commandArgs = null;
	
	//private static String LocalDataDir = "";
	
	private static String  exportHttpIP = null;
	private static String  exportSocketIP = null;
	
	private static String  listenHttpIP = null;
	private static String  listenSocketIP = null;
	
	private static String  accountName = null;
	//private static String  adminAccountName = null;
	
	//服务在RAFT中的根目录
	//private static String RaftBaseDir = "";
	
	//服务实例配置目录,对应服务运行实例
	public static String ServiceConfigDir = null;
	
	public static long systemStartTime = TimeUtils.getCurTime();
	
	private static String[] BasePackages = {"cn.jmicro"};
	
	private static final Map<String,String> servicesConfig = new HashMap<>();
	
	private static final Map<String,String> globalConfig = new HashMap<>();
	
	private static Map<String,String> CommadParams = new HashMap<>();
	
	private static Map<String,String> extConfig = new HashMap<>();
	
	private Map<String,Set<IConfigChangeListener>> configChangeListeners = new HashMap<>();
	
	private Map<String,Set<IConfigChangeListener>> patternConfigChangeListeners = new HashMap<>();
	
	private IDataOperator dataOperator;

	private IDataListener dataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String val) {
			if(path.startsWith(GROBAL_CONFIG)) {
				String supath = path.substring(GROBAL_CONFIG.length(),path.length());
				globalConfig.put(supath, val);
				notifiListener(supath,val);
			}else if(path.startsWith(ServiceConfigDir)) {
				String supath = path.substring(ServiceConfigDir.length(),path.length());
				if(CONS_PARAMS_KEYS.containsKey(path)) {
					return;
				}
				servicesConfig.put(supath, val);
				notifiListener(supath,val);
			}else {
				throw new CommonException("Invalid path: " + path);
			}
		}
	};

	
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
			} else {
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
	
	private void initInstanceName() {
		
		String dataDir = getCommandParam(Constants.LOCAL_DATA_DIR);
		if(StringUtils.isEmpty(dataDir)) {
			throw new CommonException(dataDir + " cannot be NULL");
		}
		
		String insName = null;
		File ud = null;
		File dir = new File(dataDir);
		
		if(!dir.exists()) {
			dir.mkdirs();
		}
		
		instanceRrefix = this.getString(Constants.INSTANCE_PREFIX,null);
		
		if(Utils.isEmpty(instanceRrefix)) {
			throw new CommonException("Instance prefix name cannot be null!");
		}
		
		if(ExpUtils.isNumber(instanceRrefix.charAt(instanceRrefix.length()-1))) {
			throw new CommonException("Instance name cannot end with number:" + instanceRrefix);
		}
		
		//优先在本地目录中寻找
		for(File f : dir.listFiles()) {
			if(!f.isDirectory() || !f.getName().startsWith(instanceRrefix)) {
				continue;
			}
			
			/*File lockFile = new File(f.getAbsolutePath(),LOCK_FILE);
			if(lockFile.exists()) {
				//被同一台机上的不同实例使用
				continue;
			}*/
			
			String path = Config.InstanceDir + "/" + f.getName();
			if(!dataOperator.exist(path)) {
				doTag(dataOperator,f,path);
				insName = f.getName();
				break;
			}
		}
		
		if(insName == null) {
			//实例名前缀，默认前缀是instanceName，
			for(int i = 0; i < Integer.MAX_VALUE ; i++) {
				String name = instanceRrefix + i;
				ud = new File(dir,name);
				String path = Config.InstanceDir + "/" + name;
				if(!ud.exists() && !dataOperator.exist(path)) {
					doTag(dataOperator,ud.getAbsoluteFile(),path);
					insName = name;
					break;
				}
			}
		}
		
		if(StringUtils.isEmpty(insName)) {
			throw new CommonException("Fail to get instance name");
		}
		
		//String localDataDir = CommadParams.get(Constants.LOCAL_DATA_DIR);
		File f = new File(dataDir,insName);
		if(!f.exists()) {
			f.mkdir();
		}
		CommadParams.put(Constants.INSTANCE_DATA_DIR, f.getAbsolutePath());
		
		CommadParams.put(Constants.INSTANCE_PREFIX, insName);
		
		instanceName = insName;
	}
	
	private void doTag(IDataOperator dataOperator,File dir,String path) {
		if(!dir.exists()) {
			dir.mkdirs();
		}
		/*File lf = new File(dir,LOCK_FILE);
		try {
			lf.createNewFile();
		} catch (IOException e) {
			throw new CommonException(lf.getAbsolutePath(),e);
		}
		lf.deleteOnExit();*/
		//本地存在，ZK中不存在,也就是没有虽的机器在使用此目录
		dataOperator.createNodeOrSetData(path, "", true);
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
					logger.info("{}={}", key, p.getProperty(key, ""));
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
					
					/*if(!key.startsWith("/")) {
						key = "/" + key;
					}
					key = key.replaceAll("\\.", "/");*/
					
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
	
	/*public static byte getSystemLogLevel() {
		return getCommandParam(Constants.SYSTEM_LOG_LEVEL, Byte.class, MC.LOG_ERROR);
	}*/
	
	public static boolean isAdminSystem() {
		int clientId = getClientId();
		int adminClientId = getAdminClientId();
		boolean adminSystem = adminClientId > -1 && clientId == adminClientId;
		return adminSystem;
	}
	
	public static int getClientId() {
		int clientId = getCommandParam(Constants.CLIENT_ID,Integer.class,-1);
		/*if(clientId < 0) {
			throw new CommonException("Invalid clientId :" + clientId);
		}*/
		return clientId;
	}
	
	public static String getAccountName() {
		if(accountName != null) {
			return accountName;
		}
		accountName = getCommandParam(Constants.CLIENT_NAME,String.class,null);
		return accountName;
	}
	
	public static void setAccountName(String an) {
		 accountName = an;
		 CommadParams.put(Constants.CLIENT_NAME, an);
	}
	
	/*public static void setAdminAccountName(String an) {
		adminAccountName = an;
	}*/
	
	public static int getAdminClientId() {
		int adminClientId = getCommandParam(Constants.ADMIN_CLIENT_ID,Integer.class,-1);
		return adminClientId;
	}
	
	public static long getSystemStartTime() {
		return systemStartTime;
	}
	
	public static String getInstanceName(){
		if(StringUtils.isEmpty(instanceName)){
			throw new CommonException("InstanceName cannot be NULL");
		}
		return instanceName;
	}
	
	public static String getInstancePrefix(){
		if(StringUtils.isEmpty(instanceRrefix)){
			throw new CommonException("instanceRrefix cannot be NULL");
		}
		return instanceRrefix;
	}
	
	public static String getNamespace(){
		String prefix = getInstancePrefix();
		String resOwner = getCommandParam(ChoyConstants.RES_OWNER_ID);
		if(resOwner == null) {
			return prefix;
		}
		
		if(Config.getClientId() != Integer.parseInt(resOwner)) {
			return prefix + "." + getAccountName();
		}
		
		return prefix;
	}
	
	public static boolean isOwnerRes(){
		String resOwner = getCommandParam(ChoyConstants.RES_OWNER_ID);
		if(resOwner == null) {
			return true;
		}
		return Config.getClientId() == Integer.parseInt(resOwner);
	}
	
	public synchronized static void setBasePackages0(Collection<String>  basePackages) {
		if(basePackages == null || basePackages.size() == 0) {
			return;
		}
		StringBuffer sb = new StringBuffer();
		Set<String> set = new HashSet<>();
		for(String p: basePackages) {
			set.add(p.trim());
			sb.append(p.trim()).append(",");
		}
		for(String p: BasePackages) {
			set.add(p.trim());
			sb.append(p.trim()).append(",");
		}
		String[] pps = new String[set.size()];
		set.toArray(pps);
		BasePackages = pps;
		
		if(sb.length() > 0) {
			sb.deleteCharAt(sb.length()-1);
		}
		logger.info("Base package: " + Arrays.asList(BasePackages));
		CommadParams.put(Constants.BASE_PACKAGES_KEY, sb.toString());
	}
	
	private void createParam2Raft() {
		for(Map.Entry<String, String> e : CommadParams.entrySet()) {
			String p = e.getKey();
			if(!p.startsWith("/")) {
				p = "/" + p;
			}
			servicesConfig.put(p, e.getValue());
			String fullpath = ServiceConfigDir + p;
			this.dataOperator.createNodeOrSetData(fullpath, e.getValue(),true);
			if(!CONS_PARAMS_KEYS.containsKey(p)) {
				dataOperator.addDataListener(fullpath, this.dataListener);
			}
		}
		
		for(Map.Entry<String, String> e : extConfig.entrySet()) {
			String p = e.getKey();
			if(!p.startsWith("/")) {
				p = "/" + p;
			}
			if(!servicesConfig.containsKey(p)) {
				createConfig(e.getValue(),p,false,true);
			}
		}
	}

	public void createConfig(String value, String path, boolean isGlobal){
		createConfig(value,path,isGlobal,false);
	}
	
	public void createConfig(String value, String path, boolean isGlobal, boolean el){
		if(isGlobal) {
			if(!globalConfig.containsKey(path)) {
				globalConfig.put(path, value);
				this.dataOperator.createNodeOrSetData(GROBAL_CONFIG + path, value,el);
				if(!CONS_PARAMS_KEYS.containsKey(path)) {
					dataOperator.addDataListener(GROBAL_CONFIG + path, this.dataListener);
				}
			}
		} else if(!servicesConfig.containsKey(path)) {
			servicesConfig.put(path, value);
			this.dataOperator.createNodeOrSetData(ServiceConfigDir + path, value,el);
			if(!CONS_PARAMS_KEYS.containsKey(path)) {
				dataOperator.addDataListener(ServiceConfigDir + path, this.dataListener);
			}
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
		
		//LocalDataDir = this.getValue(Constants.LOCAL_DATA_DIR,true);
		
		List<String> ips = Utils.getIns().getLocalIPList();
        if(ips.isEmpty()){
        	throw new CommonException("IP not found");
        }
        
        String defHost = ips.get(0);
        
		exportHttpIP = getValue(Constants.ExportHttpIP,false);
		exportSocketIP = getValue(Constants.ExportSocketIP,false);
		
		listenHttpIP = getValue(Constants.ListenHttpIP,false);
		listenSocketIP = getValue(Constants.ListenSocketIP,false);
		
		if(StringUtils.isEmpty(listenHttpIP)) {
			listenHttpIP =  defHost;
		} 
		
		if(StringUtils.isEmpty(listenSocketIP)) {
			listenSocketIP =  defHost;
		} 
		
		if(StringUtils.isEmpty(exportHttpIP)) {
			exportHttpIP =  defHost;
		} 
		
		if(StringUtils.isEmpty(exportSocketIP)) {
			exportSocketIP =  defHost;
		} 
	}
	
	public static String getRegistryHost() {
		return RegistryHost;
	}
	
	/*public static String getRegistryPort() {
		return RegistryPort;
	}*/
	
	public static String getExportHttpHost() {
		return exportHttpIP;
	}
	
	public static String getExportSocketHost() {
		return exportSocketIP;
	}
	
	public static String getListenHttpHost() {
		return listenHttpIP;
	}
	
	public static String getListenSocketHost() {
		return listenSocketIP;
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
		return getValue(getCommandParam(key),type,defalutValue);
	}
	
	public static String getExtParam(String key) {
		String val = getServiceParam0(key);
		if(!Utils.isEmpty(val)) {
			return val;
		}
		return getMapVal(key,extConfig,null);
	}
	
	public static String getServiceParam0(String key) {
		if(!key.startsWith("/")) {
			key = "/" + key;
		}
		if(servicesConfig.containsKey(key)) {
			return servicesConfig.get(key);
		}
		
		String k0 = getKey(key);
		if(servicesConfig.containsKey(k0)) {
			return servicesConfig.get(k0);
		}
		
		return null;
	}
	
	/**
	 * 如果key 是 a.b.c格式，则转换为 /a/b/c
	 * 如果key 是/a/b/c格式，则转换为 a.b.c
	 * @param key
	 * @return
	 */
	private static String getKey(String key) {
		if(key == null) {
			return null;
		}
		if(key.contains("/")) {
			if(key.startsWith("/")) {
				key = key.substring(1);
			}
			key = key.replace("/", "\\.");
		} else {
			key = key.replace("\\.", "/");
			key = "/" + key;
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
		
		//因为命令行配置加载时统一存放到了服务配置列表中，所以如果服务配置列表中有，则直接使用
		String val = getServiceParam0(key);
		if(!Utils.isEmpty(val)) {
			return val;
		}
		return getMapVal(key,CommadParams,null);
		
		/*while(StringUtils.isEmpty(v)) {
			if(!key.contains("/")) {
				break;
			}
			
			key = key.substring(key.indexOf("/"));
			v = getMapVal(key,CommadParams,null);
			if(!StringUtils.isEmpty(v)) {
				break;
			}
			
			key = key.substring(1);
			v = getMapVal(key,CommadParams,null);
		}
		
		return v;
		*/
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
		
		for(Map.Entry<String, String> e : globalConfig.entrySet()) {
			if(e.getKey().startsWith(key)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		

		for(Map.Entry<String, String> e : extConfig.entrySet()) {
			if(e.getKey().startsWith(key)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		
		for(Map.Entry<String, String> e : servicesConfig.entrySet()) {
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
	
	private void loadOne(String root,String child,Map<String,String> params) {
		String fullpath = root+"/"+child;
		String data = dataOperator.getData(fullpath);
		if(StringUtils.isNotEmpty(data)){
			params.put("/"+child, data);
			dataOperator.addDataListener(fullpath, this.dataListener);
		} 
		Set<String> children = dataOperator.getChildren(fullpath,true);
		 for(String ch: children){
			 String pa = child + "/"+ch;
			 loadOne(root,pa,params);
		 }
	}

	public void loadConfig(IDataOperator dataOperator) {
		this.dataOperator = dataOperator;
		//加载全局配置
		/*for(IConfigLoader cl : configLoaders){
			cl.setDataOperator(this.dataOperator);
			cl.load(GROBAL_CONFIG,globalConfig);
			cl.setConfigChangeListener(this);
		}*/
		
		 Set<String> children = dataOperator.getChildren(GROBAL_CONFIG,true);
		 for(String child: children){
			 loadOne(GROBAL_CONFIG,child,globalConfig);
		 }
		
		initInstanceName();
		
		String path = Config.InstanceDir +"/"+Config.getInstanceName()+"_ipPort";
		if(dataOperator.exist(path)) {
			dataOperator.setData(path, "");
		}
		
		ServiceConfigDir = CfgDir + "/" + instanceName;
		
		//	/jmicro目录
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
		 Set<String> children0 = dataOperator.getChildren(ServiceConfigDir,true);
		 for(String child: children0){
			 loadOne(ServiceConfigDir,child,servicesConfig);
		 }
		 
		/*
		for(IConfigLoader cl : configLoaders){
			cl.load(ServiceConfigDir,servicesConfig);
		}*/
		
		init0();
		
		createParam2Raft();
	
	}
	
	public Map<String,String> getConsParamKeys() {
		return Collections.unmodifiableMap(CONS_PARAMS_KEYS);
	}
	
	@SuppressWarnings("serial")
	private static final Map<String,String> CONS_PARAMS_KEYS = new HashMap<String,String>() {
		{
			put("exportSocketPort", "");
			put("startSocket", "");
			put("enableMasterSlaveModel", "");
			put("instanceName", "");
			put("instanceDataDir", "");
			put("respBufferSize", "");
			put("defaultLimiterName", "");
			put("exportHttpPort", "");
			put("startHttp", "");
			put("registryUrl", "");
			put("clientId", "");
			put("nettyHttpPort", "");
			put("localDataDir", "");
			put("priKey", "");
			
			put(Constants.BASE_PACKAGES_KEY,"");
			put(Constants.INSTANCE_PREFIX,"");
		}
	};
	
}
