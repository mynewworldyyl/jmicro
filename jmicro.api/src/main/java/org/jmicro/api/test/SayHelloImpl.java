package org.jmicro.api.test;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Service;

@Service(maxSpeed="1s")
@Component
public class SayHelloImpl implements ISayHello {

	@Override
	public String hello(String name) {
		return "Server say hello to: "+name;
	}

	
}
