package cn.expjmicro.objfactory.simple.integration.test;

import org.junit.Test;

import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.ILogMonitorServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.JMLogItem;
import cn.jmicro.api.monitor.JMStatisItem;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestLogMonitor extends JMicroBaseTestCase{
	
	private JMLogItem logItem() {
		JMLogItem si = new JMLogItem();
		si.addOneItem(MC.LOG_DEBUG, TestLogMonitor.class.getName(),
				"Test Monitor server");
		si.setLinkId(22L);
		return si;
	}
	
	private JMStatisItem statisItem() {
		JMStatisItem si = new JMStatisItem();
		si.addType(MC.MT_REQ_END, 1);
		return si;
	}
	
	
	@Test
	public void testMonitorServer() {
		this.setSayHelloContextv2();
		ILogMonitorServer m = of.getRemoteServie(ILogMonitorServer.class.getName()
				, "monitorServer", "0.0.1",null);
		JMLogItem[] sis = new JMLogItem[1];
		sis[0] = logItem();
		m.submit(sis);
		this.waitForReady(1000*10);
	}
	
	@Test
	public void testMonitorData() {
		this.setSayHelloContextv2();
		ServiceMethod sm = sayHelloServiceMethod();
		IMonitorDataSubscriber dataServer = of.getRemoteServie(IMonitorDataSubscriber.class.getName()
				, "rpcStatisMonitor", "0.0.1",null);
		ReportData values = dataServer.getData(sm.getKey().toKey(true, true, true), MC.STATIS_TYPES_ARR,
				new String[] {MC.PREFIX_QPS});
		System.out.println(JsonUtils.getIns().toJson(values));
		this.waitForReady(1000*10);
	}
	
	@Test
	public void testMonitorDataSubmiter() {
		StatisMonitorClient m = of.get(StatisMonitorClient.class);
		m.readySubmit(statisItem());
		this.waitForReady(1000000);
	}
	
	@Test
	public void testSFSubmit() {
		//SF.doBussinessLog(MonitorConstant.LOG_ERROR, TestMonitorSubmiterV2.class, null, "Hello");
		//SF.netIo(MonitorConstant.LOG_ERROR, "testmonitor", TestMonitorSubmiterV2.class, null);
		LG.log(MC.LOG_ERROR,TestLogMonitor.class, "testmonitor");
		this.waitForReady(1000000);
	}
	
	@Test
	public void testSubmitLog() {
		
		for(;;){
			
			try {
				LG.log(MC.LOG_ERROR,TestLogMonitor.class, "testmonitorPresure");
			} catch (Throwable e1) {
				e1.printStackTrace();
			}
			
			try {
				Thread.sleep(50);
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
