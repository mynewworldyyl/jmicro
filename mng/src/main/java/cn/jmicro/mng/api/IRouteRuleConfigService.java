package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.route.RouteRule;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IRouteRuleConfigService {

	Resp<List<RouteRule>> query();
	
	Resp<Boolean> update(RouteRule cfg);
	
	Resp<Boolean> delete(String insName, int id);
	
	Resp<RouteRule> add(RouteRule cfg);
}
