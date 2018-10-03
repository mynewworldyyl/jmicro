package org.jmicro.main;

import org.jmicro.api.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;

public class ServiceComsumer {

	public static void main(String[] args) {
		
		Config.parseArgs(args);
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start(args);
		
		//got remote service from object factory
		TestRpcClient src = of.get(TestRpcClient.class);
		//invoke remote service
		src.invokePersonService();
	}
}
