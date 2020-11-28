package cn.jmicro.api.monitor;

import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IStatisDataSubscribe {

	void onData(StatisData sc);
}
