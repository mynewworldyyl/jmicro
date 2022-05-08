package cn.jmicro.ext.bbs;

import cn.jmicro.api.JMicro;
import cn.jmicro.common.Utils;

public class BbsStarter {

	public static void main(String[] args) {
		/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
		 Thread.currentThread().setContextClassLoader(cl);*/
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}

}
