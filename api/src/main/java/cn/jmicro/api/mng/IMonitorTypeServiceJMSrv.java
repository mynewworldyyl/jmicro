package cn.jmicro.api.mng;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.monitor.MCConfigJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMonitorTypeServiceJMSrv {

	////////////////////GROBAL CONFIG////////////////////////////////
	RespJRso<List<MCConfigJRso>> getAllConfigs();
	
	RespJRso<Void> update(MCConfigJRso mc);
	
	RespJRso<Void> delete(short type);
	
	RespJRso<Void> add(MCConfigJRso mc);
	
	//////////////////////FOR MONITOR///////////////////////////////
	RespJRso<List<Short>> getConfigByMonitorKey(String key);
	
	//Resp<Void> add2Monitor(String key,Short[] type);
	
	//Resp<Void> removeFromMonitor(String key,Short[] type);
	
	RespJRso<Void> updateMonitorTypes(String key,Short[] adds,Short[] dels);
	
	RespJRso<Map<String,String>> getMonitorKeyList();
	
	
	//////////////////////FOR SERVICE METHOD MONITOR///////////////////////////////
	RespJRso<List<Short>> getConfigByServiceMethodKey(String key);
	
	RespJRso<Void> updateServiceMethodMonitorTypes(String key,Short[] adds,Short[] dels);
	
	RespJRso<List<MCConfigJRso>> getAllConfigsByGroup(String[] groups);
	
	////////////////////////Named types ///////////////////////////////////
	RespJRso<Void> addNamedTypes(String name);
	
	RespJRso<List<Short>> getTypesByNamed(String name);
	
	RespJRso<Void> updateNamedTypes(String name, Short[] adds, Short[] dels);
	
	RespJRso<List<String>> getNamedList();
}
