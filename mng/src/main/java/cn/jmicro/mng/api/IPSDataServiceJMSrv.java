package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IPSDataServiceJMSrv {

	RespJRso<Long> count(Map<String, String> queryConditions);
	
	RespJRso<List<PSDataVoJRso>> query(Map<String,String> queryConditions,int pageSize,int curPage);
	
/*	Resp<Map<String,Object>> queryDict();
	
	Resp<PSDataVo> getByLinkId(Long linkId);
	
	Resp<Integer> countLog(Map<String, String> queryConditions);
	Resp<List<LogItem>> queryLog(Map<String,String> queryConditions,int pageSize,int curPage);*/
	
}
