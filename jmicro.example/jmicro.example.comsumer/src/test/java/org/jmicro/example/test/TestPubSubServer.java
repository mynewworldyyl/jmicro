package org.jmicro.example.test;

import java.util.HashMap;

import org.jmicro.api.JMicro;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.pubsub.IInternalSubRpc;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.junit.Test;

public class TestPubSubServer {

	@Test
	public void testPublishStringMessage() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testPublishStringMessage"});
		of.start();
		PubSubManager psm = of.get(PubSubManager.class);
		psm.publish(new HashMap<String,String>(), "/jmicro/test/topic01", "test pubsub server");
	}
	
	@Test
	public void testPubSubServerMessage() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testPubSubServerMessage"});
		of.start();
		IInternalSubRpc psm = of.getServie(IInternalSubRpc.class.getName(), "org.jmicro.pubsub.DefaultPubSubServer", "0.0.1");
		PSData psd = new PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setTopic("/jmicro/test/topic01");
		psm.publishData(psd);
	}

	
}
