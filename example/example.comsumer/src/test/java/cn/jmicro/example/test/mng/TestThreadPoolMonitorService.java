package cn.jmicro.example.test.mng;

import java.util.List;

import org.junit.Test;

import cn.jmicro.api.Resp;
import cn.jmicro.api.executor.ExecutorInfo;
import cn.jmicro.api.mng.IThreadPoolMonitor;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestThreadPoolMonitorService  extends JMicroBaseTestCase{

	@Test
	public void testServerList() throws InterruptedException {
		IThreadPoolMonitor ms = of.getRemoteServie(IThreadPoolMonitor.class.getName(), 
				"mng", "0.0.1", null);
		//for(;true;) {
			Resp<List<ExecutorInfo>> resp = ms.serverList();
			System.out.println(JsonUtils.getIns().toJson(resp));
			Thread.sleep(100);
		//}
		this.waitForReady(1000);
	}
}
