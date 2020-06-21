package cn.jmicro.example.rpc.impl;

import java.util.Random;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SBreakingRule;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.api.test.Person;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.rpc.IRpcA;
import cn.jmicro.example.api.rpc.ISimpleRpc;

@Service(namespace="simpleRpc", version="0.0.1", monitorEnable=1, maxSpeed=-1,
baseTimeUnit=Constants.TIME_SECONDS, clientId=1000)
@Component
public class SimpleRpcImpl implements ISimpleRpc {

	@Reference(namespace="rpca", version="0.0.1")
	private IRpcA rpca;
	
	private Random r = new Random(100);
	
	@Override
	@SMethod(
			//breakingRule="1S 50% 500MS",
			//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
			breakingRule = @SBreakingRule(enable=true,percent=50,checkInterval=5000),
			logLevel=MC.LOG_DEBUG,	
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
		if(SF.isLoggable(MC.LOG_DEBUG)) {
			SF.doBussinessLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG,SimpleRpcImpl.class,null, name);
		}
		/*int rv = r.nextInt();
		if(rv < 50) {
			throw new CommonException("test breaker exception");
		}*/
		System.out.println("Server hello: " +name);
		return "Server say hello to: "+name;
	}
	
	@Override
	@SMethod(
			//breakingRule="1S 50% 500MS",
			//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
			breakingRule = @SBreakingRule(enable=true,percent=50,checkInterval=5000),
			logLevel=MC.LOG_DEBUG,	
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
	public String hi(Person name) {
		if(SF.isLoggable(MC.LOG_DEBUG)) {
			SF.doBussinessLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG,SimpleRpcImpl.class,null, name.getUsername());
		}
		return "Server say hello to: "+name;
	}

	@Override
	public String linkRpc(String msg) {
		if(SF.isLoggable(MC.LOG_DEBUG)) {
			SF.doBussinessLog(MC.MT_APP_LOG,MC.LOG_DEBUG,SimpleRpcImpl.class,null, "linkRpc call IRpcA with: " + msg);
		}
		System.out.println("linkRpc: " + msg);
		return this.rpca.invokeRpcA(msg+" linkRpc => invokeRpcA");
	}

	
}
