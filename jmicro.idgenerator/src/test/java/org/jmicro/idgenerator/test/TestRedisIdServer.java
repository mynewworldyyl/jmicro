package org.jmicro.idgenerator.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;

public class TestRedisIdServer {
	
	@Test
	public void testIdTestRedisIdServerId() {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {
				"-DinstanceName=testIdTestRedisIdServerId",
				"-Dclient=true",
				"-Dorg.jmicro.api.idgenerator.IIdClient=uniqueIdGenerator"});
		
		//IIdServer idServer = of.getServie(IIdServer.class.getName(), "RedisBaseIdServer", "0.0.1");
		ComponentIdServer idServer = of.get(ComponentIdServer.class);
		
		String[] longId = idServer.getStringIds(IRequest.class.getName(), 10);
		System.out.println(longId);

	}
	
}
