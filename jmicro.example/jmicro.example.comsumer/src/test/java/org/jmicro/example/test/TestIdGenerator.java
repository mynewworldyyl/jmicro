package org.jmicro.example.test;

import org.apache.zookeeper.server.quorum.QuorumCnxManager.Message;
import org.jmicro.api.JMicro;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;

public class TestIdGenerator {

	@Test
	public void testLongIDGenerator(){
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		
		IIdGenerator g = of.get(IIdGenerator.class);
		g.getLongId(Message.class);
	}
}
