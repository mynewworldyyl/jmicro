package cn.expjmicro.example.test.monitor;

import org.junit.Test;

import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.JMLogItem;
import cn.jmicro.api.monitor.JMStatisItem;
import cn.jmicro.api.monitor.genclient.ILogMonitorServer$JMAsyncClient;
import cn.jmicro.api.monitor.genclient.IStatisMonitorServer$JMAsyncClient;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestMonitorServer extends JMicroBaseTestCase{
	
	@Test
	public void testAsyncSubmitLog() {
		ILogMonitorServer$JMAsyncClient ms = of.getRemoteServie(ILogMonitorServer$JMAsyncClient.class.getName(), 
				"monitorServer", "0.0.1", null);
		
		JMLogItem mi = new JMLogItem();
		mi.addOneItem(MC.LOG_DEBUG, "test","test desc");
		
		ms.submitJMAsync(new JMLogItem[] { mi })
		.success((rst,cxt)->{
			System.out.println("Success: " + rst);
		})
		.fail((code,rst,cxt)->{
			System.out.println("Fail: " + rst);
		})
		;
		
		this.waitForReady(1000000);
	}
	
	@Test
	public void testLGSubmitLog() {
        LG.log(MC.LOG_ERROR, TestMonitorServer.class,"Hello log monitor server!");
		this.waitForReady(1000000);
	}
	
	
	@Test
	public void testAsyncSubmitStatisItem() {
		IStatisMonitorServer$JMAsyncClient ms = of.getRemoteServie(IStatisMonitorServer$JMAsyncClient.class.getName(), 
				"monitorServer", "0.0.1", null);
		
		JMStatisItem mi = new JMStatisItem();
		mi.addType(MC.EP_START, 1);
		
		ms.submitJMAsync(new JMStatisItem[] { mi })
		.success((rst,cxt)->{
			System.out.println("Success: " + rst);
		})
		.fail((code,rst,cxt)->{
			System.out.println("Fail: " + rst);
		})
		;
		
		this.waitForReady(1000000);
	}
	
}
