package org.jmicro.api.service.integration;

import org.jmicro.example.provider.TestRpcServiceImpl;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTestRpcServiceImpl  extends JMicroBaseTestCase{

private final static Logger logger = LoggerFactory.getLogger(TestTestRpcServiceImpl.class);
	
	@Test
	public void testInjectMapConfig() {
		TestRpcServiceImpl rsi = of.get(TestRpcServiceImpl.class);
		Assert.assertNotNull(rsi.params);
		Assert.assertFalse(rsi.params.isEmpty());
		System.out.println(rsi.params);
	}
}
