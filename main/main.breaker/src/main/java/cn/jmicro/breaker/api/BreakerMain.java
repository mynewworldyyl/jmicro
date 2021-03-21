package cn.jmicro.breaker.api;

import cn.jmicro.api.JMicro;

public class BreakerMain {

	public static void main(String[] args) {
		/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
		 Thread.currentThread().setContextClassLoader(cl);*/
		JMicro.getObjectFactoryAndStart(args);
		JMicro.waitForShutdown();
	}
}
