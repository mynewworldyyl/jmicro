package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.api.mng.LogEntry;
import cn.jmicro.api.mng.LogItem;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ILogService {

	Resp<Long> count(Map<String, String> queryConditions);
	
	Resp<List<LogEntry>> query(Map<String,String> queryConditions,int pageSize,int curPage);
	
	Resp<Map<String,Object>> queryDict();
	
	Resp<LogEntry> getByLinkId(Long linkId);
	
	Resp<Integer> countLog(Map<String, String> queryConditions);
	Resp<List<LogItem>> queryLog(Map<String,String> queryConditions,int pageSize,int curPage);
	
}
