package cn.jmicro.example.rpc.impl;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.SF;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.rpc.IRpcB;

@Service(namespace="rpcb", version="0.0.1", monitorEnable=1, maxSpeed=-1,
baseTimeUnit=Constants.TIME_SECONDS, clientId=1000)
@Component
public class RpcBImpl implements IRpcB {

	@Override
	public String invokeRpcB(String aargs) {
		if(SF.isLoggable(MC.LOG_DEBUG)) {
			SF.doBussinessLog(MC.MT_APP_LOG,MC.LOG_DEBUG,SimpleRpcImpl.class,null, aargs + ": invokeRpcB return");
		}
		System.out.println("invokeRpcB: " + aargs);
		return aargs + " : invokeRpcB return";
	}

}
