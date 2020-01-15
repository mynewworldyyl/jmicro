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
	
	@Test
	public void testAddServiceListener() {
		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(DynamicInterface.class.getName(), Config.getInstanceName()+"_DynamicRegistryService", "0.0.1", DynamicInterface.class.getName());
		
		ServiceMethod sm = sl.createSrvMethod(si, "run", new Class[] {PSData.class});
		sm.setTopic("/jmicro/test/topic01");
		
		sl.registService(si);
		
		DynamicInterface r = (data)->{
			System.out.println("runnalbe Rpc called: "+data.toString());
		};
		
		of.regist(DynamicInterface.class, r);
		
		JMicro.waitForShutdown();
		
	}
	
}
