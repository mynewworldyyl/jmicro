package org.jmicro.example.comsumer;

import org.jmicro.api.JMicro;
import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;

public class ServiceComsumer {

	public static void main(String[] args) {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		
		//got remote service from object factory
		TestRpcClient src = of.get(TestRpcClient.class);
		//invoke remote service
		src.invokePersonService();
	}
}
