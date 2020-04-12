package org.jmicro.objfactory.simple.integration.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.JMicro;
import org.jmicro.api.monitor.v1.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.pubsub.IInternalSubRpc;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

import junit.framework.Assert;

public class TestPubSubServer extends JMicroBaseTestCase{

	@Test
	public void testPublishPSDatas() {
		IInternalSubRpc psm = of.getRemoteServie(IInternalSubRpc.class.getName(),
				Constants.DEFAULT_PUBSUB, "0.0.1",null,null);
		
		PSData psd = new PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setTopic(TOPIC);
		
		int rst = psm.publishItems(TOPIC,new PSData[] {psd,psd});
		Assert.assertTrue(rst == PubSubManager.PUB_OK);
		
		//psm.publishData(psd);
		
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testPubSubServerMessage() {
		IInternalSubRpc psm = of.getRemoteServie(IInternalSubRpc.class.getName(),
				"org.jmicro.pubsub.DefaultPubSubServer", "0.0.1",null,null);
		
		PSData psd = new PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setTopic(TOPIC);
		
		
		psm.publishItem(psd);
	}
	
	@Test
	public void testPublishArgs() {
		PubSubManager psm = of.get(PubSubManager.class);
		Object[] args = new String[] {"test publish args"};
		long msgid = psm.publish(TOPIC,PSData.flag(PSData.FLAG_PUBSUB,PSData.FLAG_MESSAGE_CALLBACK),args);
		System.out.println("pubsub msgID:"+msgid);
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishMessageWithCallback() {
		PubSubManager psm = of.get(PubSubManager.class);
		
		ServiceItem si = this.getServiceItem("org.jmicro.example.pubsub.impl.SimplePubsubImpl");
		ServiceMethod sm = this.getServiceMethod(si, "notifyMessageStatu", new Class[] {Integer.TYPE,Long.TYPE,Map.class});
		
		PSData psd = new PSData();
		psd.setData("test publish args");
		psd.setTopic(TOPIC);
		psd.setFlag(PSData.flag(PSData.FLAG_PUBSUB,PSData.FLAG_MESSAGE_CALLBACK));
		psd.setCallback(sm.getKey());
		psd.put("tst", 222);
		psd.put("22f", "String");
		psd.put("sm2", sm);
		
		long msgid = psm.publish(psd);
		System.out.println("pubsub msgID:"+msgid);
		
		JMicro.waitForShutdown();
	}
	

	@Test
	public void testPresurePublish() {
		
		final Random ran = new Random();
		
		PubSubManager psm = of.get(PubSubManager.class);
		
		AtomicInteger id = new AtomicInteger(0);
		
		Runnable r = ()->{
			while(true) {
				try {
					/*
					long msgid = psm.publish(new HashMap<String,Object>(), TOPIC, 
							"test pubsub server id: "+id.getAndIncrement(),PSData.FLAG_QUEUE);*/

					//long msgid = psm.publish("/jmicro/test/topic02",PSData.FLAG_PUBSUB,new String[] {"test pubsub server id: "+id.getAndIncrement()});
					//System.out.println("pubsub msgID:"+msgid);
					
					Map<Short,Double> data = new HashMap<>();
					for(Short type : AbstractMonitorDataSubscriber.YTPES) {
						//Double v = new Double(counter.getAvgWithEx(type,TimeUtils.getTimeUnit(sm.getBaseTimeUnit())));
						data.put(type, 222.23D);
						//degradeManager.updateExceptionCnt(typeKey(key,type),v.toString());
					}
					
					data.put(MonitorConstant.STATIS_TOTAL_RESP, 222D);
					data.put(MonitorConstant.REQ_START,  222D);
					data.put(MonitorConstant.STATIS_TOTAL_SUCCESS_PERCENT,  222D);
					data.put(MonitorConstant.STATIS_TOTAL_FAIL_PERCENT,  232D);
					
					PSData psData = new PSData();
					psData.setData(data);
					psData.setTopic(TOPIC);
					psData.put(Constants.SERVICE_METHOD_KEY, new ServiceMethod());
					
					psm.publish(psData);
					//System.out.println(psm.publish(psData));
					
					//Thread.sleep(2000);
					Thread.sleep(ran.nextInt(100));
					
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		//new Thread(r).start();
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishStringMessage() {
		PubSubManager psm = of.get(PubSubManager.class);
		psm.publish(new HashMap<String,Object>(), TOPIC, "test pubsub server",PSData.FLAG_PUBSUB);
		JMicro.waitForShutdown();
	}
}
