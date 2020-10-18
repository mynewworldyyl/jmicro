package cn.jmicro.main.monitor.v1;

import cn.jmicro.api.JMicro;
import cn.jmicro.common.Utils;

public class MonitorStarter {

	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}
}
