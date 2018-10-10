package org.jmicro.example.comsumer;

import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;

public class ServiceComsumer {

	public static void main(String[] args) {
		
		Config.parseArgs(args);
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		
		//got remote service from object factory
		TestRpcClient src = of.get(TestRpcClient.class);
		//invoke remote service
		src.invokePersonService();
	}
}
