package org.jmicro.breaker.api;

import org.jmicro.api.JMicro;

public class BreakerMain {

	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(new String[]{});
		JMicro.waitForShutdown();
	}
}
