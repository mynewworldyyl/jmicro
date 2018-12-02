package org.jmicro.api.registry;

import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;

public final class UniqueServiceKey {

	public static final String SEP = "##";
	
	private String instanceName;
	private String host;
	private int port;
	
	private String serviceName;
	private String namespace = Constants.DEFAULT_NAMESPACE;
	private String version = Constants.VERSION;
	
	public UniqueServiceKey() {}
	
	public UniqueServiceKey(String serviceName,String namespace,String version) {
		this.serviceName = serviceName;
		this.namespace = namespace;
		this.version = version;
	}
	
	public String toKey(boolean ins,boolean host,boolean port) {
		StringBuilder sb = new StringBuilder();
		serviceName(sb,this.serviceName);
		namespace(sb,this.namespace);
		version(sb,this.version);
		
		instanceName(ins,sb,this.instanceName);
		host(host,sb,this.host);
		port(port,sb,this.port);
		;
		return sb.substring(0, sb.length()-2);
	}
	
	public UniqueServiceKey fromKey(String key) {
		String[] strs = key.split(SEP);
		if(strs.length != 8 ) {
			throw new CommonException("Invalid unique service method key: " + key);
		}
		int idx = -1;
		this.serviceName = strs[++idx];
		this.namespace = strs[++idx];
		this.version = strs[++idx];
		this.instanceName = strs[++idx];
		this.host = strs[++idx];
		this.port = Integer.parseInt(strs[++idx]);
		
		return this;
	}
	
	public static StringBuilder appendOne(boolean flag,StringBuilder sb,String val) {
		if(flag) {
			sb.append(val==null?"":val);
		}
		sb.append(SEP);
		return sb;
	}
	
	public static StringBuilder instanceName(boolean ins,StringBuilder sb,String instanceName) {
		return appendOne(ins,sb,instanceName);
	}
	
	public static StringBuilder host(boolean ins,StringBuilder sb,String host) {
		return appendOne(ins,sb,host);
	}
	
	public static StringBuilder port(boolean ins,StringBuilder sb,int port) {
		return appendOne(ins,sb,port+"");
	}
	
	public static StringBuilder namespace(StringBuilder sb,String namespace){
		return appendOne(true,sb,namespace(namespace));
	}
	
	public static String namespace(String namespace){
		if(StringUtils.isEmpty(namespace)){
			namespace = Constants.DEFAULT_NAMESPACE;
		}
		return namespace;
	}
	
	public static String version(String version){
		if(StringUtils.isEmpty(version)){
			version = Constants.VERSION;
		}
		return version;
	}
	
	public static StringBuilder version(StringBuilder sb,String version){
		if(version == null || "".equals(version)){
			version = Constants.VERSION;
		}
		return appendOne(true,sb,version);
	}
	
	public static StringBuilder serviceName(StringBuilder sb,String serviceName){
		if(StringUtils.isEmpty(serviceName)) {
			throw new CommonException("Service name cannot be null");
		}
		return appendOne(true,sb,serviceName);
	}
	
	public static StringBuilder serviceName(String sn, String ns, String v) {
		return version(snnsPrefix(sn,ns),v);
	}
	
	public static StringBuilder snnsPrefix(String sn, String ns) {
		StringBuilder sb = new StringBuilder();
		serviceName(sb,sn);
		namespace(sb,ns);
		return sb;
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
	
	public String toString() {
		return toKey(true,true,true);
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
	public String getInstanceName() {
		return instanceName;
	}
	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
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

	@Override
	public int hashCode() {
		return this.toKey(true, true, true).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof UniqueServiceKey)) {
			return false;
		}
		return hashCode() == obj.hashCode();
	}

	@Override
	protected UniqueServiceKey clone() throws CloneNotSupportedException {
		return (UniqueServiceKey) super.clone();
	}
	
}
