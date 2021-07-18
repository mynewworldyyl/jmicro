package cn.jmicro.api.monitor;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IStatisDataSubscribeJMSrv {

	IPromise<Void> onData(StatisDataJRso sc);
}
