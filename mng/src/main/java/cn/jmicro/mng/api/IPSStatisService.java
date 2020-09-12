package cn.jmicro.mng.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IPSStatisService {

	Resp<Long> count(Map<String, String> queryConditions);
	
	Resp<List<Map<String,Object>>> query(Map<String,String> queryConditions,int pageSize,int curPage);
	
}
