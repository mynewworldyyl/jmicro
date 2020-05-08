package org.jmicro.main.monitor.v1;

import org.jmicro.api.JMicro;
import org.jmicro.common.Utils;

public class MonitorStarter {

	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {});
		 Utils.getIns().waitForShutdown();
	}
}
