package cn.expjmicro.objfactory.simple.integration.test;

import java.util.Random;
import java.util.Set;

import org.junit.Test;

import cn.expjmicro.example.api.rpc.ISimpleRpc;
import cn.expjmicro.example.api.rpc.genclient.ISimpleRpc$JMAsyncClient;
import cn.expjmicro.example.comsumer.TestRpcClient;
import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.JMStatisItem;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestMonitorSubmiter extends JMicroBaseTestCase{
	
	private JMStatisItem ssubItem() {
		JMStatisItem si = new JMStatisItem();
		si.addType(MC.MT_REQ_END, 1);
		return si;
	}
	
	@Test
	public void testSubmitItem() {
		IMonitorDataSubscriber m = of.getRemoteServie(IMonitorDataSubscriber.class.getName()
				, "printLogMonitor", "0.0.1",null);
		JMStatisItem[] sis = new JMStatisItem[1];
		sis[0] = ssubItem();
		m.onSubmit(sis);
		
		this.waitForReady(1000*10);
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
					, "printLogMonitor", "0.0.1",null);
			
			JMStatisItem[] sis = new JMStatisItem[1];
			sis[0] = ssubItem();
		
			setSayHelloContext();
			
			for(;;){
				
				
				//logger.debug("testSubmitLog");
				
				try {
					//m.onSubmit(sis);
					//LG.eventLog(MC.MT_PLATFORM_LOG,MC.LOG_DEBUG,this.getClass(),"testSubmitLog");
					MT.nonRpcEvent(MC.MT_PLATFORM_LOG);
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
			/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
			 Thread.currentThread().setContextClassLoader(cl);*/
			IObjectFactory of = (IObjectFactory)JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testSayHelloToPrintRouterLog","-Dclient=true"});
			
			JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, true);
			ISimpleRpc$JMAsyncClient sayHello = (ISimpleRpc$JMAsyncClient)of.getRemoteServie(ISimpleRpc.class,"exampleProvider", null);
			JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
			String result = sayHello.hello("Hello LOG");
			System.out.println(result);
			JMicro.waitForShutdown();
			
			
		}

		@Test
		public void testGetInteret() {
			
			final Random ran = new Random();
			
			IObjectFactory of = (IObjectFactory)JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testGetInteret","-Dclient=true"});
			
			JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, false);;
			Set<IMonitorDataSubscriber> ls = of.get(TestRpcClient.class).getSubmiters();
			
			Runnable r = ()->{
				while(true) {
					try {
						for(IMonitorDataSubscriber m : ls) {
							//m.intrest();
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
