package cn.jmicro.breaker.api;

import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IBreakerService {

	void onData(StatisData sc);
}
