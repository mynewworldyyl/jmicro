package org.jmicro.example.test;

import org.apache.zookeeper.server.quorum.QuorumCnxManager.Message;
import org.jmicro.api.config.Config;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;
import org.junit.Test;

public class TestIdGenerator {

	@Test
	public void testLongIDGenerator(){
		Config.parseArgs(new String[0]);
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		
		IIdGenerator g = of.get(IIdGenerator.class);
		g.getLongId(Message.class);
	}
}
