package org.jmicro.example.provider;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Service;
import org.jmicro.example.api.ISayHello;

@Service(maxSpeed="1ms")
@Component
public class SayHelloImpl implements ISayHello {

	@Override
	public String hello(String name) {
		System.out.println("Server hello: " +name);
		return "Server say hello to: "+name;
	}

	
}
