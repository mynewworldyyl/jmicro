package org.jmicro.main.monitor.test;

import org.jmicro.api.JMicro;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SubmitItem;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;

public class TestServiceReqMonitor extends JMicroBaseTestCase{

	@Test
	public void testOnSubmitBySubmiter() {
		this.setSayHelloContext();
		this.waitForReady(5);
		//Assert.assertTrue(SF.doSubmit(MonitorConstant.CLIENT_REQ_OK));
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testOnSubmit() {
		
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.REQ_SUCCESS);
		si.setSm(this.sayHelloServiceMethod());
		SubmitItem[] sis = new SubmitItem[1];
		sis[0] = si;
		//of.get(ServiceReqMonitor.class).onSubmit(sis);
		
		JMicro.waitForShutdown();
	}
	
	@Test
	public void testGetData() {
		
		SubmitItem si = new SubmitItem();
		si.setType(MonitorConstant.REQ_SUCCESS);
		//si.setSm(this.sayHelloServiceMethod());
		
		//ServiceReqMonitor m = of.get(ServiceReqMonitor.class);
		
		/*SubmitItem[] sis = new SubmitItem[1];
		sis[0] = si;
		
		m.onSubmit(sis);
		
		String mkey = si.getSm().getKey().toKey(true,true,true);
		System.out.println("STATIS_FAIL_PERCENT: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_FAIL_PERCENT));
		System.out.println("STATIS_TOTAL_REQ: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_REQ));
		System.out.println("STATIS_TOTAL_RESP: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_RESP));
		System.out.println("STATIS_TOTAL_SUCCESS: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_SUCCESS));
		System.out.println("STATIS_TOTAL_FAIL: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_FAIL));
		System.out.println("STATIS_SUCCESS_PERCENT: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_SUCCESS_PERCENT));
		System.out.println("CLIENT_REQ_TIMEOUT_FAIL: "+m.getData(mkey, MonitorConstant.REQ_TOTAL_TIMEOUT_FAIL));
		System.out.println("STATIS_TIMEOUT_PERCENT: "+m.getData(mkey, MonitorConstant.STATIS_TOTAL_TIMEOUT_PERCENT));
		System.out.println("STATIS_QPS: "+m.getData(mkey, MonitorConstant.STATIS_QPS));
		*/
		//JMicro.waitForShutdown();
	}
}
