package org.jmicro.example.test;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.config.Config;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItemHolderManager;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;
import org.junit.Test;

public class TestMonitor {

	@Test
	public void testMonitor01() {
		Config.parseArgs(new String[0]);
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		
		JMicroContext.get().configMonitor(1, 1);
		SubmitItemHolderManager monitor = of.get(SubmitItemHolderManager.class);
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
