package org.jmicro.example.test;

import java.util.HashMap;

import org.jmicro.api.JMicro;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.PubSubManager;
import org.junit.Test;

public class TestPubSubServer {

	@Test
	public void testCheckable() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		PubSubManager psm = of.get(PubSubManager.class);
		psm.publish(new HashMap<String,String>(), "/jmicro/test/topic01", "test pubsub server");
	}
	

	
}
