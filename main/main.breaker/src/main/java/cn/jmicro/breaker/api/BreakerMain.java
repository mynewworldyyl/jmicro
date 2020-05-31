package cn.jmicro.breaker.api;

import cn.jmicro.api.JMicro;

public class BreakerMain {

	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
		JMicro.waitForShutdown();
	}
}
