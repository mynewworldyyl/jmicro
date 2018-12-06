package org.jmicro.example.provider;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.example.api.ISayHello;

@Service(maxSpeed="1ms")
@Component
public class SayHelloImpl implements ISayHello {

	@Override
	@SMethod(loggable=1)
	public String hello(String name) {
		if(SF.isLoggable(true,MonitorConstant.DEBUG)) {
			SF.doBussinessLog(MonitorConstant.DEBUG,SayHelloImpl.class,null, name);
		}
		System.out.println("Server hello: " +name);
		return "Server say hello to: "+name;
	}

	
}
