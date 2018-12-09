package org.jmicro.gateway;

import org.jmicro.api.JMicro;
import org.jmicro.common.Utils;

public class ApiGatewayMain {

	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=ApiGateway","-Dclient=true"});
		 Utils.getIns().waitForShutdown();
	}
}
