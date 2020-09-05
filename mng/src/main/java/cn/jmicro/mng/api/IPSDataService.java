package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IPSDataService {

	Resp<Long> count(Map<String, String> queryConditions);
	
	Resp<List<PSDataVo>> query(Map<String,String> queryConditions,int pageSize,int curPage);
	
/*	Resp<Map<String,Object>> queryDict();
	
	Resp<PSDataVo> getByLinkId(Long linkId);
	
	Resp<Integer> countLog(Map<String, String> queryConditions);
	Resp<List<LogItem>> queryLog(Map<String,String> queryConditions,int pageSize,int curPage);*/
	
}
