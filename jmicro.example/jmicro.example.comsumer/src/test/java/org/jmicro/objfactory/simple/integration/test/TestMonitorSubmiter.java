package org.jmicro.objfactory.simple.integration.test;

import java.util.HashSet;
import java.util.Set;

import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.IMonitorDataSubscriber;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestMonitorSubmiter extends JMicroBaseTestCase{
	
	@Test
	public void testSubmitItem() {
		IMonitorDataSubscriber m = of.getRemoteServie(IMonitorDataSubscriber.class.getName()
				, "serviceExceptionMonitor", "0.0.1", null,null);
		SubmitItem si = new SubmitItem();
		Set<SubmitItem> sis = new HashSet<>();
		sis.add(si);
		m.onSubmit(sis);
		
		this.waitForReady(100);
	}
	
	@Test
	public void testMonitorDataSubmiter() {
		IMonitorDataSubmiter m = of.get(IMonitorDataSubmiter.class);
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.CLIENT_REQ_BEGIN);
		m.submit(si);
		this.waitForReady(100);
	}
	
	@Test
	public void testSFSubmiter() {
		this.setSayHelloContext();
		SF.doSubmit(MonitorConstant.CLIENT_REQ_BEGIN);
		this.waitForReady(100);
	}
	
}
