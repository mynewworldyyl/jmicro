package org.jmicro.monitor.memory;

import org.jmicro.api.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Utils;

public class MainResponseTimeMonitor {

	public static void main(String[] args) {
		
		Config.parseArgs(args);
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		Utils.waitForShutdown();
	}

}
