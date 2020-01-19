package org.jmicro.api.service.integration;

import org.jmicro.api.JMicro;
import org.jmicro.api.config.Config;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.service.ServiceLoader;
import org.jmicro.example.api.DynamicInterface;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDynamicRegistryService extends JMicroBaseTestCase{

	private final static Logger logger = LoggerFactory.getLogger(TestDynamicRegistryService.class);
	
	/**
	 *测试 动态的，通过编程方式注册组件，注册服务，创建服务方法，订阅消息
	 *名称空间值加Config.getInstanceName()，是处理多实例时，动态注册的服务的唯一性
	 */
	@Test
	public void testAddServiceListener() {
		
		DynamicInterface r = (data)->{
			System.out.println("runnalbe Rpc called: "+data);
		};
		of.regist(DynamicInterface.class, r);
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(DynamicInterface.class.getName(), Config.getInstanceName()+"_DynamicRegistryService", "0.0.1", DynamicInterface.class.getName());
		
		ServiceMethod sm = sl.createSrvMethod(si, "run", new Class[] {String.class});
		sm.setMonitorEnable(1);
		sm.setTopic("/jmicro/test/topic02");
		
		sl.registService(si,r);
		
		//sl.unregistService(si);
		
		JMicro.waitForShutdown();
		
	}
	
}
