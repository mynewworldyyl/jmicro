package cn.jmicro.api.monitor;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IStatisDataSubscribe {

	IPromise<Void> onData(StatisData sc);
}
