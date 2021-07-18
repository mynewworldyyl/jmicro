package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.mng.LogEntry;
import cn.jmicro.api.monitor.JMFlatLogItemJRso;
import cn.jmicro.api.monitor.JMLogItemJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ILogServiceJMSrv {

	RespJRso<Long> count(Map<String, String> queryConditions);
	
	RespJRso<List<LogEntry>> query(Map<String,String> queryConditions,int pageSize,int curPage);
	
	RespJRso<Map<String,Object>> queryDict();
	
	RespJRso<LogEntry> getByLinkId(Long linkId);
	
	RespJRso<Integer> countLog(int showType,Map<String, String> queryConditions);
	//Resp<List<LogItem>> queryLog(Map<String,String> queryConditions,int pageSize,int curPage);
	
	RespJRso<List<JMLogItemJRso>> queryLog(Map<String,String> queryConditions,int pageSize,int curPage);
	
	RespJRso<List<JMFlatLogItemJRso>> queryFlatLog(Map<String, String> queryConditions, int pageSize, int curPage);
}
