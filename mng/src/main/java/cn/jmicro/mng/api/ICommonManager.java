package cn.jmicro.mng.api;

import java.util.Map;

import cn.jmicro.api.Resp;

public interface ICommonManager {

	Map<String,String> getI18NValues(String lang);
	

	boolean hasPermission(int per);
	
	boolean notLoginPermission(int per);
	
	Resp<Map<String,Object>> getDicts(String[] keys);
}
