package cn.jmicro.idgenerator.test;

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.objectfactory.IObjectFactory;

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
