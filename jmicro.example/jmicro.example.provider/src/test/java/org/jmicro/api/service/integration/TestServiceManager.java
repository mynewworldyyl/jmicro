package org.jmicro.api.service.integration;

import java.util.Set;

import org.jmicro.api.config.Config;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestServiceManager extends JMicroBaseTestCase{

	private final static Logger logger = LoggerFactory.getLogger(TestServiceManager.class);
	
	@Test
	public void testAddServiceListener() {
		IServiceListener lis = (type,item)->{
			logger.debug("testAddServiceListener type {},item:{}",type,item.key());
		};
		this.get(ServiceManager.class).addServiceListener(
				sayHelloServiceItem().path(Config.ServiceRegistDir),lis);
		this.get(ServiceManager.class).removeServiceListener(sayHelloServiceItem().path(Config.ServiceRegistDir), lis);
	}
	
	@Test
	public void testAddListener() {
		
		IServiceListener lis = (type,item)->{
			logger.debug("testAddListener type {},item:{}",type,item.key());
		};
		this.get(ServiceManager.class).addListener(lis);
		this.get(ServiceManager.class).removeListener(lis);
		
		sayHelloServiceItem();
		//最终一至性等待
		this.waitForReady(1);
	}
	
	@Test
	public void testUpdateOrCreate() {
		ServiceItem si = sayHelloServiceItem();
		this.get(ServiceManager.class).updateOrCreate(si, si.path(Config.ServiceRegistDir), true);
	}
	
	@Test
	public void testGetItem() {
		ServiceItem si = sayHelloServiceItem();
		Assert.assertNotNull(this.get(ServiceManager.class).getItem(si.path(Config.ServiceRegistDir)));
	}
	
	@Test
	public void testGetServiceItem() {
		sayHelloServiceItem();
		Set<ServiceItem> sis = this.get(ServiceManager.class).getAllItems();
		Assert.assertNotNull(sis);
		Assert.assertFalse(sis.isEmpty());
	}
	
	@Test
	public void testGetAllItems() {
		sayHelloServiceItem();
		Set<ServiceItem> sis = this.get(ServiceManager.class).getAllItems();
		Assert.assertNotNull(sis);
		Assert.assertFalse(sis.isEmpty());
	}
	
}
