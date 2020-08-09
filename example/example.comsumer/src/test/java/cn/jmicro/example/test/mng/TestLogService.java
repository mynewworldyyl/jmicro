package cn.jmicro.example.test.mng;

import org.junit.Test;

import cn.jmicro.api.Resp;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.example.api.DynamicInterface;
import cn.jmicro.mng.api.IAgentLogService;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestLogService   extends JMicroBaseTestCase{

	private void registService() {
		DynamicInterface r = (data)->{
			System.out.println(data);
		};
		of.regist(DynamicInterface.class, r);
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(DynamicInterface.class.getName(), Config.getInstanceName()+"_DynamicRegistryService", "0.0.1", DynamicInterface.class.getName());
		
		ServiceMethod sm = sl.createSrvMethod(si, "run", new Class[] {String.class});
		sm.setMonitorEnable(1);
		sm.setTopic("/450/logs/output.log");
		
		sl.registService(si,r);
	}
	
	@Test
	public void testStartLogMonitor() throws InterruptedException {
		registService();
		this.waitForReady(8);
		
		IAgentLogService ms = of.getRemoteServie(IAgentLogService.class.getName(), "mng", "0.0.1", null);
		
		Resp<Boolean> resp = ms.startLogMonitor("450", "output.log", "JMicroAgent0", 10);
		
		System.out.println(JsonUtils.getIns().toJson(resp));
		
		this.waitForReady(Long.MAX_VALUE);
	}
}
