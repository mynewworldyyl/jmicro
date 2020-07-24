package cn.jmicro.api.executor;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.common.Constants;

@Service(showFront=false,monitorEnable=1, maxSpeed=-1, baseTimeUnit=Constants.TIME_MILLISECONDS, timeout=500, debugMode=1)
@AsyncClientProxy
public interface IExecutorInfo {

	ExecutorInfo getInfo();
	
}
