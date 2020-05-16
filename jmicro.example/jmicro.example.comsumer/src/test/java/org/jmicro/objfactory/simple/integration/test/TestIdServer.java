package org.jmicro.objfactory.simple.integration.test;

import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.choreography.api.Deployment;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestIdServer  extends JMicroBaseTestCase{

	@Test
	public void testGetID() {
		ComponentIdServer idServer = of.get(ComponentIdServer.class);
		String id = idServer.getStringId(Deployment.class);
		System.out.println(id);
	}
}
