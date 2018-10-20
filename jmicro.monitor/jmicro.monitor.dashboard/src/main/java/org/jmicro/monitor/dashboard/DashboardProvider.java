package org.jmicro.monitor.dashboard;

import org.jmicro.api.JMicro;
import org.jmicro.common.Utils;

public class DashboardProvider {

	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
		Utils.getIns().waitForShutdown();
	}

}
