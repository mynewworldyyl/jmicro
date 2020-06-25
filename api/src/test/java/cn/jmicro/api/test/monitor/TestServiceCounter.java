package cn.jmicro.api.test.monitor;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.timer.TimerTicker;

public class TestServiceCounter {

	private final static Logger logger = LoggerFactory.getLogger(TestServiceCounter.class);
	
	@Test
	public void testServiceCounter() {
		Random r = new Random(1000);
		ServiceCounter count = new ServiceCounter("test",null,10,2,TimeUnit.SECONDS);
		short t = 1;
		count.addCounter(t);
		for(;true;) {
			count.add(t, 1);
			System.out.println("Total:"+count.get(t));
			System.out.println("Avg:"+count.getQps(TimeUnit.SECONDS,t));
			System.out.println("=============================");
			try {
				int tt = r.nextInt(1000);
				Thread.sleep((tt < 0 ? -tt : tt));
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
				MC.MS_TYPES_ARR,2,1,TimeUnit.SECONDS);
		while(true) {
			sc.increment(MC.MT_REQ_SUCCESS);
			Long succp = sc.get(MC.MT_REQ_SUCCESS);
			//Double qps = ServiceCounter.getData(sc,MonitorConstant.STATIS_QPS);
			logger.debug("treq:{}",succp);
		}
	
	}
	
	@Test
	public void testMutilThreadCounter() {
		final Random ran = new Random(500);
		ServiceCounter sc =  new ServiceCounter("testMutilThreadCounter", 
				MC.MS_TYPES_ARR,30000,1000,TimeUnit.MILLISECONDS);
		
		Runnable r = ()->{
			while(true) {
				sc.increment(MC.MT_REQ_START);
				try {
					Thread.sleep(ran.nextInt(50));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int v = ran.nextInt(10);
				v = v % 10;
				if(v < 9) {
					sc.increment(MC.MT_REQ_SUCCESS);
				}/*else if(v == 8) {
					
				} */else if(v == 9) {
					v = ran.nextInt(1);
					if(v == 0) {
						sc.increment(MC.MT_REQ_TIMEOUT);
					}else if(v == 1) {
						sc.increment(MC.MT_CLIENT_SERVICE_ERROR);
					}
				}
				
				/*Double treq = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_BEGIN);
				Double succp = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_OK);
				Double top = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_TIMEOUT);
				Double errp = sc.getValueWithEx(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
				logger.debug("treq:{}, suuc:{}, to:{}, err:{}",treq,succp,top,errp);*/
			}
		};
		
		TimerTicker.getDefault(1*1000L).addListener("testMutilThreadCounterTimer", null, (key,att)->{
			
			//Double failPercent = ServiceCounter.getData(sc, MonitorConstant.STATIS_TOTAL_FAIL_PERCENT);// sc.getTotal(MonitorConstant.CLIENT_REQ_BEGIN);
			//Double succPersent = ServiceCounter.getData(sc, MonitorConstant.STATIS_TOTAL_SUCCESS_PERCENT); //sc.getTotal(MonitorConstant.CLIENT_REQ_OK);
			
			//Double qps = ServiceCounter.getData(sc,MonitorConstant.STATIS_QPS);
			
			//logger.debug("qps:{}, succPersent:{}, failPercent:{}",qps,succPersent,failPercent);
			
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
				
				
		});
		
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
				new Short[] {MC.MT_REQ_SUCCESS},30000,100,TimeUnit.MILLISECONDS);
		
		Runnable r = ()->{
			while(true) {
				sc.increment(MC.MT_REQ_SUCCESS);
				try {
					Thread.sleep(ran.nextInt(50));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		TimerTicker.getDefault(1*1000L).addListener("testSingleThreadSingleCounterTimer", null, (key,att)->{
			
			//Double qps = ServiceCounter.getData(sc,MonitorConstant.STATIS_QPS);
			
			//logger.debug("qps:{}",qps);
				
		});
		
		new Thread(r,"testMutilThreadCounter1").start();
		new Thread(r,"testMutilThreadCounter2").start();
		new Thread(r,"testMutilThreadCounter3").start();
		new Thread(r,"testMutilThreadCounter5").start();
		new Thread(r,"testMutilThreadCounter6").start();
		
		JMicro.waitForShutdown();
	}
	
}
