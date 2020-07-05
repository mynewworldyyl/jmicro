package cn.jmicro.api.executor;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.common.Constants;

@Service(monitorEnable=1, maxSpeed=-1, baseTimeUnit=Constants.TIME_MILLISECONDS, timeout=500, debugMode=1)
public interface IExecutorInfo {

	ExecutorInfo getInfo();
	
}
