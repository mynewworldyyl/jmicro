package org.jmicro.example.provider;

import org.jmicro.api.JMicro;
import org.jmicro.common.Utils;

public class ServiceProvider {

	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
		Utils.getIns().waitForShutdown();
	}

}
