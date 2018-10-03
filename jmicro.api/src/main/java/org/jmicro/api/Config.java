package org.jmicro.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.exception.CommonException;
import org.jmicro.common.Constants;

@Component(value="default",lazy=false)
public class Config {
	
	private static String RegistryProtocol = "zookeeper";
	private static String RegistryHost = "localhost";
	private static String RegistryPort = "2180";
	
	private static URL RegistryUrl = null;
	
	private static String[] commandArgs = null;
	private static String ConfigRoot = "/jmicro/config";
	private static String[] BasePackages = {"org.jmicro"};
	
	private static Map<String,String> CommadParams = new HashMap<String,String>();
	
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
		RegistryUrl = new URL(RegistryProtocol,RegistryHost,Integer.parseInt(RegistryPort));
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
	
	@Cfg("/basePackages")
	private Collection<String> basePackages = null;
	
	private Map<String,String> params = new HashMap<String,String>();
	
	public Config() {}
	
	@JMethod("init")
	public void init(){
		
	}
	
	public static String getConfigRoot() {
		return ConfigRoot;
	}
	
	public static URL getRegistryUrl() {
		return RegistryUrl;
	}
	
	public static String[]  getBasePackages() {
		return BasePackages;
	}
	
	public void setBasePackages(Collection<String>  basePackages) {
		 setBasePackages0(basePackages);
	}
}
