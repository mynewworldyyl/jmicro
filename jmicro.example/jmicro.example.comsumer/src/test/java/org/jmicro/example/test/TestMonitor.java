package org.jmicro.example.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;

public class TestMonitor {

	@Test
	public void testMonitor01() {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		
		JMicroContext.get().configMonitor(1, 1);
		IMonitorDataSubmiter monitor = of.get(IMonitorDataSubmiter.class);
		for(;;){
			//MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_BEGIN, null, null);
			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_OK, null, null);
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}
