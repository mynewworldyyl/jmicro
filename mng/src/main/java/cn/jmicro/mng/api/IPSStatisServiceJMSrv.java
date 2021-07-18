package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IPSStatisServiceJMSrv {

	RespJRso<Long> count(Map<String, String> queryConditions);
	
	RespJRso<List<Map<String,Object>>> query(Map<String,String> queryConditions,int pageSize,int curPage);
	
}
