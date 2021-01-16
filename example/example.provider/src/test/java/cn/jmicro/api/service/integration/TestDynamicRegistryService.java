package cn.jmicro.api.service.integration;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.example.api.IDynamicInterface;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestDynamicRegistryService extends JMicroBaseTestCase{

	private final static Logger logger = LoggerFactory.getLogger(TestDynamicRegistryService.class);
	
	/**
	 *测试 动态的，通过编程方式注册组件，注册服务，创建服务方法，订阅消息
	 *名称空间值加Config.getInstanceName()，是处理多实例时，动态注册的服务的唯一性
	 */
	@Test
	public void testAddServiceListener() {
		
		IDynamicInterface r = (data)->{
			System.out.println("runnalbe Rpc called: "+data);
		};
		of.regist(IDynamicInterface.class, r);
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(IDynamicInterface.class.getName(), Config.getInstanceName()+"_DynamicRegistryService", "0.0.1",
				IDynamicInterface.class.getName(),Config.getClientId());
		
		ServiceMethod sm = sl.createSrvMethod(si, "run", new Class[] {String.class});
		sm.setMonitorEnable(1);
		sm.setTopic("/jmicro/test/topic02");
		
		sl.registService(si,r);
		
		//sl.unregistService(si);
		
		JMicro.waitForShutdown();
		
	}
	
}
