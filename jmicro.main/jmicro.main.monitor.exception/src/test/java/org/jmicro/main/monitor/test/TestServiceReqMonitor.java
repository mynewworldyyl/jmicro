package org.jmicro.main.monitor.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.main.monitor.ServiceReqMonitor;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Assert;
import org.junit.Test;

public class TestServiceReqMonitor extends JMicroBaseTestCase{

	@Test
	public void testOnSubmitBySubmiter() {
		this.setSayHelloContext();
		this.waitForReady(5);
		Assert.assertTrue(SF.doSubmit(MonitorConstant.CLIENT_REQ_OK));
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testOnSubmit() {
		
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.CLIENT_REQ_OK);
		si.setSm(this.sayHelloServiceMethod());
		
		of.get(ServiceReqMonitor.class).onSubmit(si);
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testGetData() {
		
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.CLIENT_REQ_OK);
		si.setSm(this.sayHelloServiceMethod());
		
		ServiceReqMonitor m = of.get(ServiceReqMonitor.class);
		m.onSubmit(si);
		
		String mkey = si.getSm().getKey().toKey(true,true,true);
		System.out.println("STATIS_FAIL_PERCENT: "+m.getData(mkey, MonitorConstant.STATIS_FAIL_PERCENT));
		System.out.println("STATIS_TOTAL_REQ: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_REQ));
		System.out.println("STATIS_TOTAL_RESP: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_RESP));
		System.out.println("STATIS_TOTAL_SUCCESS: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_SUCCESS));
		System.out.println("STATIS_TOTAL_FAIL: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_FAIL));
		System.out.println("STATIS_SUCCESS_PERCENT: "+m.getData(mkey, MonitorConstant.STATIS_SUCCESS_PERCENT));
		System.out.println("CLIENT_REQ_TIMEOUT_FAIL: "+m.getData(mkey, MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL));
		System.out.println("STATIS_TIMEOUT_PERCENT: "+m.getData(mkey, MonitorConstant.STATIS_TIMEOUT_PERCENT));
		System.out.println("STATIS_QPS: "+m.getData(mkey, MonitorConstant.STATIS_QPS));
		
		//JMicro.waitForShutdown();
	}
}
