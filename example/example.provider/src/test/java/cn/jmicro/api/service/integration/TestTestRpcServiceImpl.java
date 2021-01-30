package cn.jmicro.api.service.integration;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.expjmicro.example.provider.TestRpcServiceImpl;
import cn.jmicro.test.JMicroBaseTestCase;

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
