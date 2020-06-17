package cn.jmicro.example.test.monitor;

import org.junit.Test;

import cn.jmicro.api.monitor.IMonitorServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcItem;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestMonitorServer extends JMicroBaseTestCase{
	
	@Test
	public void testAsyncCallRpc() {
		IMonitorServer ms = of.getRemoteServie(IMonitorServer.class.getName(), 
				"monitorServer", "0.0.1", null);
		
		MRpcItem mi = new MRpcItem();
		mi.addOneItem(MC.MT_PLATFORM_LOG, TestMonitorServer.class.getName());
		ms.submit(new MRpcItem[] {mi,mi});
		
		this.waitForReady(1000);
	}
	
}
