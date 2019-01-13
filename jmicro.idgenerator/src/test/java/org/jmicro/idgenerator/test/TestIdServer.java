package org.jmicro.idgenerator.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.idgenerator.IIdServer;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;

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
