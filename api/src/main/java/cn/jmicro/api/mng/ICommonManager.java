package cn.jmicro.api.mng;

import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ICommonManager {
	
	public static final String SERVICE_NAMES = "serviceNames";
	
	public static final String SERVICE_NAMESPACES = "namespaces";
	
	public static final String SERVICE_VERSIONS = "versions";
	
	public static final String SERVICE_METHODS = "methods";
	
	public static final String INSTANCES = "instances";
	
	public static final String ALL_INSTANCES = "allInstances";
	
	public static final String NAMED_TYPES = "nameTypes";
	
	public static final String MONITOR_RESOURCE_NAMES = "resourceNames";

	Map<String,String> getI18NValues(String lang);

	boolean hasPermission(int per);
	
	boolean notLoginPermission(int per);
	
	Resp<Map<String,Object>> getDicts(String[] keys,String qry);
}
