package cn.expjmicro.objfactory.simple.integration.test;

import org.junit.Test;

import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestIdServer  extends JMicroBaseTestCase{

	@Test
	public void testGetID() {
		ComponentIdServer idServer = of.get(ComponentIdServer.class);
		String id = idServer.getStringId(Deployment.class);
		System.out.println(id);
	}
}
