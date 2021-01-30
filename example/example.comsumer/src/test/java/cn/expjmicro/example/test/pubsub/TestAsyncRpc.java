package cn.expjmicro.example.test.pubsub;

import org.junit.Test;

import cn.jmicro.common.Utils;
import cn.expjmicro.example.comsumer.TestRpcClient;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestAsyncRpc extends JMicroBaseTestCase{
	
	@Test
	public void testAsyncCallRpc() {
		of.get(TestRpcClient.class).testCallAsyncRpc();
		Utils.getIns().waitForShutdown();
	}
	
}
