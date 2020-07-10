package cn.jmicro.api.mng;

import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ICommonManager {

	Map<String,String> getI18NValues(String lang);
	

	boolean hasPermission(int per);
	
	boolean notLoginPermission(int per);
	
	Resp<Map<String,Object>> getDicts(String[] keys);
}
