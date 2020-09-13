package cn.jmicro.ext.bbs.api;

import cn.jmicro.api.JMicro;
import cn.jmicro.common.Utils;

public class BbsStarter {

	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}

}
