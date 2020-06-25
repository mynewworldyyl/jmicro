package cn.jmicro.example.rpc.impl;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.rpc.IRpcA;
import cn.jmicro.example.api.rpc.IRpcB;

@Service(namespace="rpca", version="0.0.1", monitorEnable=1, maxSpeed=-1,
baseTimeUnit=Constants.TIME_SECONDS, clientId=1000)
@Component
public class RpcAImpl implements IRpcA {

	@Reference(namespace="rpcb", version="0.0.1")
	private IRpcB rpcb;
	
	@Override
	public String invokeRpcA(String aargs) {
		if(SF.isLoggable(MC.LOG_DEBUG)) {
			SF.eventLog(MC.MT_APP_LOG,MC.LOG_DEBUG,SimpleRpcImpl.class, aargs + ": invokeRpcA => invokeRpcB");
		}
		System.out.println("invokeRpcA: " + aargs);
		//return "invokeRpcA: " + aargs;
		return this.rpcb.invokeRpcB(aargs + " : invokeRpcA => invokeRpcB");
	}

}
