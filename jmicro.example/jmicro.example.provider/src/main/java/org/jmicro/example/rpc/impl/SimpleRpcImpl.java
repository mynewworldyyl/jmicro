package org.jmicro.example.rpc.impl;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SBreakingRule;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.common.Constants;
import org.jmicro.example.api.rpc.ISimpleRpc;


@Service(maxSpeed=-1,baseTimeUnit=Constants.TIME_SECONDS)
@Component
public class SimpleRpcImpl implements ISimpleRpc {

	@Override
	@SMethod(
		//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
		breakingRule = @SBreakingRule(enable=true,breakTimeInterval=1000,percent=50,checkInterval=80),
		testingArgs="gv/9gwAQamF2YS5sYW5nLk9iamVjdAABgf/8AApBcmUgeW91IE9L",//测试参数
		monitorEnable=1,//启动监听
		timeWindow=30*1000,//统计时间窗口20S
		checkInterval=2000,//采样周期2S
		baseTimeUnit=Constants.TIME_MILLISECONDS
	)
	public String hello(String name) {
		if(SF.isLoggable(true,MonitorConstant.LOG_DEBUG)) {
			SF.doBussinessLog(MonitorConstant.LOG_DEBUG,SimpleRpcImpl.class,null, name);
		}
		
		System.out.println("Server hello: " +name);
		return "Server say hello to: "+name;
	}

}