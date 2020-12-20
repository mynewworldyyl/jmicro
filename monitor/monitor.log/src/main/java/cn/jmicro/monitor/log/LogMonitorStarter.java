package cn.jmicro.monitor.log;

import cn.jmicro.api.JMicro;
import cn.jmicro.common.Utils;

public class LogMonitorStarter {

	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}
}
