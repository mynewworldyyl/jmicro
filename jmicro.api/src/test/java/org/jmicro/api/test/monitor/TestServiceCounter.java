package org.jmicro.api.test.monitor;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.jmicro.api.JMicro;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.ServiceCounter;
import org.jmicro.api.net.ISession;
import org.jmicro.api.timer.TimerTicker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceCounter {

	private final static Logger logger = LoggerFactory.getLogger(TestServiceCounter.class);
	
	@Test
	public void testServiceCounter() {
		Random r = new Random(1000);
		ServiceCounter count = new ServiceCounter("test", 10, TimeUnit.SECONDS);
		count.addCounter(1, 10);
		for(;true;) {
			count.add(1, 1);
			System.out.println("Total:"+count.get(1));
			System.out.println("Avg:"+count.getQpsWithEx(1, TimeUnit.SECONDS));
			System.out.println("=============================");
			try {
				int t = r.nextInt(1000);
				Thread.sleep((t < 0 ? -t : t));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testServiceCounterSingleVal() {
		final Random ran = new Random(100);
		ServiceCounter sc =  new ServiceCounter("testServiceCounterSingleVal", 
				ISession.STATIS_TYPES,2,2,TimeUnit.SECONDS);
		while(true) {
			sc.increment(MonitorConstant.CLIENT_REQ_OK);
			Double succp = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_OK);
			Double qps = ServiceCounter.getData(sc,MonitorConstant.STATIS_QPS);
			logger.debug("treq:{}",succp);
		}
	
	}
	
	@Test
	public void testMutilThreadCounter() {
		final Random ran = new Random(500);
		ServiceCounter sc =  new ServiceCounter("testMutilThreadCounter", 
				ISession.STATIS_TYPES,30000,100,TimeUnit.MILLISECONDS);
		
		Runnable r = ()->{
			while(true) {
				sc.increment(MonitorConstant.CLIENT_REQ_BEGIN);
				try {
					Thread.sleep(ran.nextInt(50));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int v = ran.nextInt(10);
				v = v % 10;
				if(v < 9) {
					sc.increment(MonitorConstant.CLIENT_REQ_OK);
				}/*else if(v == 8) {
					
				} */else if(v == 9) {
					v = ran.nextInt(1);
					if(v == 0) {
						sc.increment(MonitorConstant.CLIENT_REQ_TIMEOUT);
					}else if(v == 1) {
						sc.increment(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
					}
				}
				
				/*Double treq = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_BEGIN);
				Double succp = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_OK);
				Double top = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_TIMEOUT);
				Double errp = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
				logger.debug("treq:{}, suuc:{}, to:{}, err:{}",treq,succp,top,errp);*/
			}
		};
		
		TimerTicker.getDefault(1*1000L).addListener("testMutilThreadCounterTimer", (key,att)->{
			
			Double failPercent = ServiceCounter.getData(sc, MonitorConstant.STATIS_FAIL_PERCENT);// sc.getTotal(MonitorConstant.CLIENT_REQ_BEGIN);
			Double succPersent = ServiceCounter.getData(sc, MonitorConstant.STATIS_SUCCESS_PERCENT); //sc.getTotal(MonitorConstant.CLIENT_REQ_OK);
			
			Double qps = ServiceCounter.getData(sc,MonitorConstant.STATIS_QPS);
			
			logger.debug("qps:{}, succPersent:{}, failPercent:{}",qps,succPersent,failPercent);
			
			   /* Double treq = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_BEGIN);
				Double succp = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_OK);
				Double top = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_TIMEOUT);
				Double errp = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
				logger.debug("treq:{}, suuc:{}, to:{}, err:{}",treq,succp,top,errp);*/
				
				/*Double rtotal = sc.getTotal(MonitorConstant.CLIENT_REQ_BEGIN);
				Double stotal= sc.getTotal(MonitorConstant.CLIENT_REQ_OK);
				Double ttotal = sc.getTotal(MonitorConstant.CLIENT_REQ_TIMEOUT);
				Double etotal = sc.getTotal(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
				logger.debug("req:{}, resp:{}",rtotal,stotal+ttotal+etotal);*/
				
				
		}, null);
		
		new Thread(r,"testMutilThreadCounter1").start();
		new Thread(r,"testMutilThreadCounter2").start();
		new Thread(r,"testMutilThreadCounter3").start();
		new Thread(r,"testMutilThreadCounter5").start();
		new Thread(r,"testMutilThreadCounter6").start();
		
		JMicro.waitForShutdown();
	}
	
	
	@Test
	public void testSingleThreadSingleCounter() {
		final Random ran = new Random(1000);
		ServiceCounter sc =  new ServiceCounter("testSingleThreadSingleCounter", 
				new Integer[] {MonitorConstant.CLIENT_REQ_OK},30000,100,TimeUnit.MILLISECONDS);
		
		Runnable r = ()->{
			while(true) {
				sc.increment(MonitorConstant.CLIENT_REQ_OK);
				try {
					Thread.sleep(ran.nextInt(50));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		TimerTicker.getDefault(1*1000L).addListener("testSingleThreadSingleCounterTimer", (key,att)->{
			
			Double qps = ServiceCounter.getData(sc,MonitorConstant.STATIS_QPS);
			
			logger.debug("qps:{}",qps);
				
		}, null);
		
		new Thread(r,"testMutilThreadCounter1").start();
		new Thread(r,"testMutilThreadCounter2").start();
		new Thread(r,"testMutilThreadCounter3").start();
		new Thread(r,"testMutilThreadCounter5").start();
		new Thread(r,"testMutilThreadCounter6").start();
		
		JMicro.waitForShutdown();
	}
	
}
