package org.jmicro.example.rpc.impl;

import java.util.Random;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SBreakingRule;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SF;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.example.api.rpc.ISimpleRpc;


@Service(namespace="simpleRpc",version="0.0.1", monitorEnable=1,maxSpeed=-1,baseTimeUnit=Constants.TIME_SECONDS)
@Component
public class SimpleRpcImpl implements ISimpleRpc {

	private Random r = new Random(100);
	
	@Override
	@SMethod(
			//breakingRule="1S 50% 500MS",
			//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
			breakingRule = @SBreakingRule(enable=true,percent=50,checkInterval=5000),
			logLevel=MonitorConstant.LOG_DEBUG,	
			testingArgs="[\"test args\"]",//测试参数
			monitorEnable=1,
			timeWindow=5*60000,//统计时间窗口5分钟
			slotInterval=100,
			checkInterval=5000,//采样周期2S
			timeout=5000,
			retryInterval=1000,
			debugMode=0,
			maxSpeed=1000,
			baseTimeUnit=Constants.TIME_MILLISECONDS
	)
	public String hello(String name) {
		if(SF.isLoggable(MonitorConstant.LOG_DEBUG)) {
			SF.doBussinessLog(MonitorConstant.LOG_DEBUG,SimpleRpcImpl.class,null, name);
		}
		/*int rv = r.nextInt();
		if(rv < 50) {
			throw new CommonException("test breaker exception");
		}*/
		//System.out.println("Server hello: " +name);
		return "Server say hello to: "+name;
	}

}
