package cn.jmicro.example.rpc.impl;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.SBreakingRule;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.rpc.IRpcB;

@Service(namespace="rpcb", version="0.0.1", monitorEnable=1, maxSpeed=-1,
clientId=Constants.USE_SYSTEM_CLIENT_ID,
baseTimeUnit=Constants.TIME_SECONDS, external=true)
@Component
public class RpcBImpl implements IRpcB {

	@Override
	@SMethod(
			//breakingRule="1S 50% 500MS",
			//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
			breakingRule = @SBreakingRule(enable=true,percent=50,checkInterval=5000),
			logLevel=MC.LOG_DEBUG,
			testingArgs="[\"test invokeRpcB\"]",//测试参数
			monitorEnable=1,
			timeWindow=5*60000,//统计时间窗口5分钟
			slotInterval=100,
			checkInterval=5000,//采样周期2S
			timeout=5000,
			retryInterval=1000,
			debugMode=1,
			limitType = Constants.LIMIT_TYPE_SS,
			maxSpeed=30,
			baseTimeUnit=Constants.TIME_MILLISECONDS
	)
	public String invokeRpcB(String aargs) {
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG,SimpleRpcImpl.class, aargs + ": invokeRpcB return");
		}
		System.out.println("invokeRpcB: " + aargs);
		return aargs + " : invokeRpcB return";
	}

}
