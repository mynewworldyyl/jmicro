package org.jmicro.pubsub.test;

import org.junit.Test;

import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestPubsubManager extends JMicroBaseTestCase{

	protected String[] getArgs() {
		return new String[] {"-DinstanceName=TestPubSubServer"};
	}
	
	@Test
	public void testPubsubManagerPublish() {
		PubSubManager m = of.get(PubSubManager.class);
		PSDataJRso d = new PSDataJRso();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(PubSubManager.PUB_OK == m.publish(d));
		//JMicro.waitForShutdown();
	}
	
	@Test
	public void testPubsubManagerPublishString() {
		PubSubManager m = of.get(PubSubManager.class);
		PSDataJRso d = new PSDataJRso();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(PubSubManager.PUB_OK == m.publish(TOPIC,"testPubsubManagerPublishString",PSDataJRso.FLAG_PUBSUB,null));
		//JMicro.waitForShutdown();
	}
	
	@Test
	public void testPubsubManagerPublishBytes() {
		PubSubManager m = of.get(PubSubManager.class);
		PSDataJRso d = new PSDataJRso();
		d.setTopic(TOPIC);
		d.setData("testSubcribe");
		org.junit.Assert.assertTrue(PubSubManager.PUB_OK == m.publish(TOPIC,"testPubsubManagerPublishString".getBytes(),PSDataJRso.FLAG_PUBSUB,null));
		//JMicro.waitForShutdown();
	}
	
	/*@Test
	public void testPubsubAddSubListener() {
		PubSubServer m = of.get(PubSubServer.class);
		m.addSubsListener((type,topic,key,ctx) -> {
			System.out.println("topic:"+topic+", type:"+type + ", key:"+key.toKey(true, true, true));
		});
	}
	
	@Test
	public void testPubsubAddUnSubListener() {
		PubSubServer m = of.get(PubSubServer.class);
		ISubsListener sub = (type,topic,key,ctx) -> {
			System.out.println("topic:"+topic+", type:"+type + ", key:"+key.toKey(true, true, true));
		};
		m.addSubsListener(sub);
		m.removeSubsListener(sub);
	}*/
	
	/*@Test
	public void testSubcribe() {
		PubSubManager m = of.get(PubSubManager.class);
		Assert.assertTrue(m.subscribe(null, "/jmicro/test/topic01", 
				"org.jmicro.example.api.ITestRpcService", "testrpc","0.0.1", "helloTopic"));
		
		this.waitForReady(1);
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testUnsubcribe() {
		PubSubManager m = of.get(PubSubManager.class);
		Assert.assertTrue(m.subscribe(null, "/jmicro/test/topic01", 
				"org.jmicro.example.api.ITestRpcService", "testrpc","0.0.1", "helloTopic"));
		
		this.waitForReady(3);
		
		m.unsubcribe(null, "/jmicro/test/topic01",
				"org.jmicro.example.api.ITestRpcService", "testrpc","0.0.1", "helloTopic");
		
		//JMicro.waitForShutdown();
	}*/
	
	
	
}
