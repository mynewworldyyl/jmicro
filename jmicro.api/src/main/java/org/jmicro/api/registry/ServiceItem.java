package org.jmicro.api.registry;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.apache.dubbo.common.utils.StringUtils;
import org.jmicro.api.exception.CommonException;
import org.jmicro.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.Modifier;

public class ServiceItem{

	private final static Logger logger = LoggerFactory.getLogger(ServiceItem.class);
	
	public static final String ROOT="/jmicro/service";
	
	public static final String FILE_SEPERATOR="/";
	
	public static final String I_I_SEPERATOR="####";
	
	public static final String KV_SEPERATOR="=";
	public static final String VAL_SEPERATOR="&";
	
	protected String serviceName;
	
	protected String namespace;
	
	protected String version;
	
	private String host;
	
	private int port;
	
	private int retryCnt=-1; //method can retry times, less or equal 0 cannot be retry
	private int retryInterval=-1; // milliseconds how long to wait before next retry
	private int timeout=-1; // milliseconds how long to wait for response before timeout 
	
	/**
	 * Max failure time before downgrade the service
	 */
	private int maxFailBeforeDowngrade=-1;
	
	/**
	 * Max failure time before cutdown the service
	 */
	private int maxFailBeforeCutdown=-1;
	
	/**
	 * after the service cutdown, system can do testing weather the service is recovery
	 * with this arguments to invoke the service method
	 */
	private String testingArgs="";
	
	
	/**
	 * max qps
	 */
	private int maxSpeed=-1;
	
	/**
	 * min qps
	 * real qps less this value will downgrade service
	 */
	private int minSpeed=-1;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	private int avgResponseTime=-1;
	
	private Set<ServiceMethod> methods = new HashSet<>();
	
	public ServiceItem() {}
	
	public ServiceItem(String val) {
		//this.parseKey(key);
		this.parseVal(val);
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

	public void addMethod(ServiceMethod sm){
		methods.add(sm);
	}
	
	public Set<ServiceMethod> getMethods(){
		return methods;
	}
	
	public static String serviceName(String key) {
	    int i = key.indexOf(I_I_SEPERATOR);
		return key.substring(0, i);
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
	
	public String key(){
		StringBuffer sb = new StringBuffer(ROOT);
		if(!this.serviceName.startsWith(FILE_SEPERATOR)){
			sb.append(FILE_SEPERATOR);
		}
		sb.append(serviceName).append(I_I_SEPERATOR);
		
		StringBuffer val = new StringBuffer();
		
		val.append("host").append(KV_SEPERATOR).append(host).append(VAL_SEPERATOR)
		.append("port").append(KV_SEPERATOR).append(port).append(VAL_SEPERATOR)
		.append("namespace").append(KV_SEPERATOR).append(this.namespace).append(VAL_SEPERATOR)
		.append("version").append(KV_SEPERATOR).append(this.version).append(VAL_SEPERATOR)
		.append("time").append(KV_SEPERATOR).append(System.currentTimeMillis());

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
				sb.append(f.getName()).append(KV_SEPERATOR).append(v.toString()).append(VAL_SEPERATOR);
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
		
		return Utils.getIns().encode(sb.toString());
	}

	@Override
	public int hashCode() {
		return this.key().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof ServiceItem)) {
			return false;
		}
		return this.key().equals(((ServiceItem)obj).key());
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
		this.namespace = namespace;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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

	public void setMethods(Set<ServiceMethod> methods) {
		this.methods = methods;
	}
	
	
}
