package org.jmicro.example.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.example.api.ISayHello;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestMonitor {

    private static final Logger logger = LoggerFactory.getLogger(TestMonitor.class);
    
	@Test
	public void testMonitor01() {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testMonitor01","-Dclient=true"});
		
		JMicroContext.get().configMonitor(1, 1);
		IMonitorDataSubmiter monitor = of.get(IMonitorDataSubmiter.class);
		JMicroContext.get().setObject(JMicroContext.MONITOR, monitor);
		
		for(;;){
			//MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_BEGIN, null, null);
			SF.doSubmit(MonitorConstant.CLIENT_REQ_OK);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	@Test
	public void testSubmitLog() {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testSubmitLog","-Dclient=true"});
		
		JMicroContext.get().configMonitor(1, 1);
		IMonitorDataSubmiter monitor = of.get(IMonitorDataSubmiter.class);
		JMicroContext.get().setObject(JMicroContext.MONITOR, monitor);
		for(;;){
			SF.doServiceLog(MonitorConstant.DEBUG,this.getClass(), 1L, ISayHello.class.getName(), "testsayhello", 
					"0.0.1", "hello", new String[] {"Hello"},null);
			//logger.debug("testSubmitLog");
			try {
				Thread.sleep(500);
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
		ISayHello sayHello = of.get(ISayHello.class);
		IMonitorDataSubmiter monitor = of.get(IMonitorDataSubmiter.class);
		JMicroContext.get().setObject(JMicroContext.MONITOR, monitor);
		
		JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
		String result = sayHello.hello("Hello LOG");
		System.out.println(result);
		JMicro.waitForShutdown();
		
		/*for(;;){
			JMicroContext.get().removeParam(JMicroContext.LINKER_ID);
			String result = sayHello.hello("Hello LOG");
			System.out.println(result);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}*/
	}


}
