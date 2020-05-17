package cn.jmicro.objfactory.simple.integration.test;

import org.junit.Test;

import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.v1.MonitorConstant;
import cn.jmicro.api.monitor.v1.SF;
import cn.jmicro.api.monitor.v2.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.v2.IMonitorServer;
import cn.jmicro.api.monitor.v2.MRpcItem;
import cn.jmicro.api.monitor.v2.MonitorClient;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestMonitorSubmiterV2 extends JMicroBaseTestCase{
	
	private MRpcItem ssubItem() {
		MRpcItem si = new MRpcItem();
		si.addOneItem(MonitorConstant.LINKER_ROUTER_MONITOR, TestMonitorSubmiterV2.class.getName(),
				"Test Monitor server");
		si.setLinkId(22L);
		return si;
	}
	
	@Test
	public void testSubmitItem() {
		this.setSayHelloContextv2();
		IMonitorDataSubscriber m = of.getRemoteServie(IMonitorDataSubscriber.class.getName()
				, "printLogMonitor", "0.0.1",null);
		MRpcItem[] sis = new MRpcItem[1];
		sis[0] = ssubItem();
		m.onSubmit(sis);
		
		this.waitForReady(1000*10);
	}
	
	@Test
	public void testMonitorServer() {
		this.setSayHelloContextv2();
		IMonitorServer m = of.getRemoteServie(IMonitorServer.class.getName()
				, "monitorServer", "0.0.1",null);
		MRpcItem[] sis = new MRpcItem[1];
		sis[0] = ssubItem();
		m.submit(sis);
		this.waitForReady(1000*10);
	}
	
	@Test
	public void testMonitorData() {
		this.setSayHelloContextv2();
		ServiceMethod sm = sayHelloServiceMethod();
		IMonitorDataSubscriber dataServer = of.getRemoteServie(IMonitorDataSubscriber.class.getName()
				, "rpcStatisMonitor", "0.0.1",null);
		ReportData values = dataServer.getData(sm.getKey().toKey(true, true, true), MonitorConstant.STATIS_TYPES,
				new String[] {MonitorConstant.PREFIX_QPS});
		System.out.println(JsonUtils.getIns().toJson(values));
		this.waitForReady(1000*10);
	}
	
	@Test
	public void testMonitorDataSubmiter() {
		MonitorClient m = of.get(MonitorClient.class);
		m.readySubmit(ssubItem());
		this.waitForReady(1000000);
	}
	
	@Test
	public void testSFSubmit() {
		//SF.doBussinessLog(MonitorConstant.LOG_ERROR, TestMonitorSubmiterV2.class, null, "Hello");
		//SF.netIo(MonitorConstant.LOG_ERROR, "testmonitor", TestMonitorSubmiterV2.class, null);
		SF.doServiceLog(MonitorConstant.LOG_ERROR,TestMonitorSubmiterV2.class,null, "testmonitor");
		this.waitForReady(1000000);
	}
	
	@Test
	public void testSubmitLog() {
		
		//setSayHelloContext();
		
		MRpcItem mi= ssubItem();
		
		for(;;){
			
			try {
				SF.doServiceLog(MonitorConstant.LOG_ERROR,TestMonitorSubmiterV2.class,null, "testmonitor");
			} catch (Throwable e1) {
				e1.printStackTrace();
			}
			
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/* @Test
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
		}*/
	    
		/*@Test
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
			
		}*/
		
}
