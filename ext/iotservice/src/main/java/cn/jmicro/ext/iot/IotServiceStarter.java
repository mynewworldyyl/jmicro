package cn.jmicro.ext.iot;

import cn.jmicro.api.JMicro;
import cn.jmicro.common.Utils;

public class IotServiceStarter {

	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}

}
