package org.jmicro.monitor.memory;

import org.jmicro.api.JMicro;
import org.jmicro.common.Utils;

public class MainResponseTimeMonitor {

	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}

}
