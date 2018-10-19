package org.jmicro.example.comsumer;

import org.jmicro.api.JMicro;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.example.api.ITestRpcService;

public class ServiceComsumer {

	public static void main(String[] args) {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		
		//got remote service from object factory
		ITestRpcService src = of.get(ITestRpcService.class);
		//invoke remote service
		System.out.println(src.hello("Hello JMicro"));
	}
}
