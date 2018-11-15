package org.jmicro.api.test;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Service;

@Service(timeout=10*60*1000,maxSpeed="1s",namespace="testsayhello",version="0.0.1")
@Component
public class SayHelloImpl implements ISayHello {

	@Override
	public String hello(String name) {
		return "Server say hello to: "+name;
	}

	
}
