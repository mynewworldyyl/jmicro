package cn.jmicro.monitor.statis.api;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IStatisConfigService {
	
	Resp<List<StatisConfig>> query();
	
	Resp<Boolean> update(StatisConfig cfg);
	
	Resp<Boolean> delete(int id);
	
	Resp<StatisConfig> add(StatisConfig cfg);
	
	Resp<Boolean> enable(Integer id);
}
