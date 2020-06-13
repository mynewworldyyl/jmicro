package cn.jmicro.main.monitor.test;

import org.junit.Test;

import cn.jmicro.api.JMicro;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestServiceReqMonitor extends JMicroBaseTestCase{

	@Test
	public void testOnSubmitBySubmiter() {
		this.setSayHelloContext();
		this.waitForReady(5);
		//Assert.assertTrue(SF.doSubmit(MonitorConstant.CLIENT_REQ_OK));
		JMicro.waitForShutdown();
	}
	
}
