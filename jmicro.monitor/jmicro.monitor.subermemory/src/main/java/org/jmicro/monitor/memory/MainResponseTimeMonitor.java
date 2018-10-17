package org.jmicro.monitor.memory;

import org.jmicro.api.JMicro;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.common.Utils;

public class MainResponseTimeMonitor {

	public static void main(String[] args) {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		Utils.getIns().waitForShutdown();
	}

}
