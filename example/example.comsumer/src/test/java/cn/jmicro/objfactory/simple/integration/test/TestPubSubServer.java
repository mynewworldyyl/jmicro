package cn.jmicro.objfactory.simple.integration.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.internal.pubsub.IInternalSubRpc;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.test.JMicroBaseTestCase;
import junit.framework.Assert;

public class TestPubSubServer extends JMicroBaseTestCase{
	
	@Test
	public void testPublishPSDatas() {
		IInternalSubRpc psm = of.getRemoteServie(IInternalSubRpc.class.getName(),
				Constants.DEFAULT_PUBSUB, "0.0.1",null);
		
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
				"cn.jmicro.pubsub.DefaultPubSubServer", "0.0.1",null);
		
		PSData psd = new PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setTopic(TOPIC);
		
		psm.publishItem(psd);
	}
	
	@Test
	public void testPublishArgs() {
		PubSubManager psm = of.get(PubSubManager.class);
		Object[] args = new String[] {"test publish args"};
		long msgid = psm.publish(TOPIC,args,PSData.flag(PSData.FLAG_PUBSUB,PSData.FLAG_MESSAGE_CALLBACK),null);
		System.out.println("pubsub msgID:"+msgid);
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishMessageWithCallback() {
		PubSubManager psm = of.get(PubSubManager.class);
		
		ServiceItem si = this.getServiceItem("cn.jmicro.example.pubsub.impl.SimplePubsubImpl");
		ServiceMethod sm = this.getServiceMethod(si, "notifyMessageStatu", new Class[] {Integer.TYPE,Long.TYPE,Map.class});
		
		PSData psd = new PSData();
		psd.setData("test publish args");
		psd.setTopic(TOPIC);
		psd.setFlag(PSData.flag(PSData.FLAG_PUBSUB,PSData.FLAG_MESSAGE_CALLBACK));
		psd.setCallback(sm.getKey().toKey(false, false, false));
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
					for(Short type : MC.MS_TYPES_ARR) {
						//Double v = new Double(counter.getAvgWithEx(type,TimeUtils.getTimeUnit(sm.getBaseTimeUnit())));
						data.put(type, 222.23D);
						//degradeManager.updateExceptionCnt(typeKey(key,type),v.toString());
					}
					
					data.put(MC.STATIS_TOTAL_RESP, 222D);
					data.put(MC.MT_REQ_START,  222D);
					data.put(MC.STATIS_SUCCESS_PERCENT,  222D);
					data.put(MC.STATIS_FAIL_PERCENT,  232D);
					
					PSData psData = new PSData();
					psData.setData(data);
					psData.setTopic(TOPIC);
					psData.put(Constants.SERVICE_METHOD_KEY, new ServiceMethod());
					
					psm.publish(psData);
					//System.out.println(psm.publish(psData));
					
					//Thread.sleep(2000);
					Thread.sleep(ran.nextInt(50));
					
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		
		new Thread(r).start();
		new Thread(r).start();
		new Thread(r).start();
		//new Thread(r).start();
		//new Thread(r).start();
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testPublishStringMessage() {
		PubSubManager psm = of.get(PubSubManager.class);
		psm.publish(TOPIC, "test pubsub server",PSData.FLAG_PUBSUB,null);
		JMicro.waitForShutdown();
	}

}
