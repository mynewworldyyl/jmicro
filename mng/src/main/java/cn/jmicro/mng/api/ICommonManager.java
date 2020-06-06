package cn.jmicro.mng.api;

import java.util.Map;

public interface ICommonManager {

	Map<String,String> getI18NValues(String lang);
	

	boolean hasPermission(int per);
	
	boolean notLoginPermission(int per);
}
