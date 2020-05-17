package cn.jmicro.idgenerator.test;

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.idgenerator.IIdServer;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.objectfactory.IObjectFactory;

public class TestIdServer {
	
	@Test
	public void testIdServerGetId() {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {
				"-DinstanceName=testIdServerGetId",
				"-Dclient=true",
				"-Dorg.jmicro.api.idgenerator.IIdClient=uniqueIdGenerator"});
		
		IIdServer idServer = of.getRemoteServie(IIdServer.class.getName(), "idServer", "0.0.1",null);
		
		String[] longId = idServer.getStringIds(IRequest.class.getName(), 10);
		System.out.println(longId);

	}
}
