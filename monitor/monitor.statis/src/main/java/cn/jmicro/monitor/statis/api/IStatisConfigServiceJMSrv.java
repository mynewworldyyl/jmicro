package cn.jmicro.monitor.statis.api;

import java.util.List;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.monitor.StatisConfigJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IStatisConfigServiceJMSrv {
	
	RespJRso<List<StatisConfigJRso>> query();
	
	RespJRso<Boolean> update(StatisConfigJRso cfg);
	
	RespJRso<Boolean> delete(int id);
	
	RespJRso<StatisConfigJRso> add(StatisConfigJRso cfg);
	
	RespJRso<Boolean> enable(Integer id);
}
