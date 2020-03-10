package org.jmicro.objfactory.simple.integration.test;

import java.util.Random;
import java.util.Set;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.jmicro.example.api.rpc.ISimpleRpc;
import org.jmicro.example.comsumer.TestRpcClient;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestMonitorSubmiter extends JMicroBaseTestCase{
	
	private SubmitItem ssubItem() {
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.LINKER_ROUTER_MONITOR);
		si.setLevel(MonitorConstant.LOG_ERROR);
		si.setDesc("testSubmitItem");
		si.setLinkId(22L);
		return si;
	}
	
	@Test
	public void testSubmitItem() {
		IMonitorDataSubscriber m = of.getRemoteServie(IMonitorDataSubscriber.class.getName()
				, "printLogMonitor", "0.0.1", null,null);
		SubmitItem[] sis = new SubmitItem[1];
		sis[0] = ssubItem();
		m.onSubmit(sis);
		
		this.waitForReady(1000*10);
	}
	
	@Test
	public void testMonitorDataSubmiter() {
		IMonitorDataSubmiter m = of.get(IMonitorDataSubmiter.class);
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.REQ_START);
		m.submit(si);
		this.waitForReady(100);
	}
	
	@Test
	public void testSFSubmiter() {
		this.setSayHelloContext();
		//SF.doSubmit(MonitorConstant.CLIENT_REQ_BEGIN);
		this.waitForReady(100);
	}
	
	
	 @Test
		public void testMonitor01() {
			final Random ran = new Random();
			this.setSayHelloContext();
			this.waitForReady(10);
			
			//SF.doSubmit(MonitorConstant.CLIENT_REQ_OK);
			try {
				Thread.sleep(ran.nextInt(100));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			JMicro.waitForShutdown();
		}
	    
		@Test
		public void testMonitor02() {
			final Random ran = new Random();
			this.setSayHelloContext();
			
			for(;;){
				//MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_BEGIN, null, null);
				try {
					//SF.doSubmit(MonitorConstant.CLIENT_REQ_OK);
				} catch (Throwable e1) {
					e1.printStackTrace();
				}
				try {
					Thread.sleep(ran.nextInt(50));
					//Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
				//JMicro.waitForShutdown();
		}
		
		
		@Test
		public void testSubmitLog() {
			
			IMonitorDataSubscriber m = of.getRemoteServie(IMonitorDataSubscriber.class.getName()
					, "printLogMonitor", "0.0.1", null,null);
			
			SubmitItem[] sis = new SubmitItem[1];
			sis[0] = ssubItem();
		
			setSayHelloContext();
			
			for(;;){
				
				
				//logger.debug("testSubmitLog");
				
				try {
					//m.onSubmit(sis);
					SF.doServiceLog(MonitorConstant.LOG_DEBUG,this.getClass(),null,"testSubmitLog");
				} catch (Throwable e1) {
					e1.printStackTrace();
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
				
			//JMicro.waitForShutdown();
			
		}
		
		@Test
		public void testSayHelloToPrintRouterLog() {
			
			IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testSayHelloToPrintRouterLog","-Dclient=true"});
			
			JMicroContext.get().configMonitor(1, 1);
			ISimpleRpc sayHello = of.get(ISimpleRpc.class);
			IMonitorDataSubmiter monitor = of.get(IMonitorDataSubmiter.class);
			JMicroContext.get().setObject(JMicroContext.MONITOR, monitor);
			
			JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
			String result = sayHello.hello("Hello LOG");
			System.out.println(result);
			JMicro.waitForShutdown();
			
			
		}

		@Test
		public void testGetInteret() {
			
			final Random ran = new Random();
			
			IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testGetInteret","-Dclient=true"});
			
			JMicroContext.get().configMonitor(1, 1);
			Set<IMonitorDataSubscriber> ls = of.get(TestRpcClient.class).getSubmiters();
			
			Runnable r = ()->{
				while(true) {
					try {
						for(IMonitorDataSubscriber m : ls) {
							m.intrest();
							//System.out.println(Arrays.asList(m.intrest()).toString());
							try {
								//Thread.sleep(500000000);
								Thread.sleep(ran.nextInt(50));
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
			};
			
			new Thread(r).start();
			new Thread(r).start();
			new Thread(r).start();
			new Thread(r).start();
			new Thread(r).start();
			
			JMicro.waitForShutdown();
			
		}
		
}
