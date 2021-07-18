package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.route.RouteRuleJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IRouteRuleConfigServiceJMSrv {

	RespJRso<List<RouteRuleJRso>> query();
	
	RespJRso<Boolean> update(RouteRuleJRso cfg);
	
	RespJRso<Boolean> delete(String insName, int id);
	
	RespJRso<RouteRuleJRso> add(RouteRuleJRso cfg);
}
