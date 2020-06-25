package cn.jmicro.example.test.link;

import org.junit.Test;

import cn.jmicro.example.api.rpc.ISimpleRpc;
import cn.jmicro.test.JMicroBaseTestCase;

/**
 *  ISimpleRpc client MRpcItem instance with no reqParentId and with reqId 1; 
 *  ISimpleRpc server MRpcItem instance with no reqParentId and with reqId 1; 
 *  ISimpleRpc server as client call IRpcA with reqParentId 1 and with reqId 2;
 *  IRpcA server MRpcItem instance with reqParentId 1 and with reqId 2;
 *  IRpcA server as client call IRpcB with reqParentId 2 and with reqId 3;
 *  IRpcB server MRpcItem instance with reqParentId 2 and with reqId 3;
 *  
 * @author Yulei Ye
 * @date 2020年6月18日
 */
public class TestLinkRpc extends JMicroBaseTestCase{
	
	@Test
	public void testLinkRpc() {
		ISimpleRpc ms = of.getRemoteServie(ISimpleRpc.class.getName(), 
				"simpleRpc", "0.0.1", null);
		System.out.println(ms.linkRpc("Test link: "));
		this.waitForReady(1000);
	}
	
	@Test
	public void testLinkRpcLoop() {
		ISimpleRpc ms = of.getRemoteServie(ISimpleRpc.class.getName(), 
				"simpleRpc", "0.0.1", null);
		for(int i = 0; i < 10 ; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println(ms.linkRpc("Test link: "+i));
		}
		
		this.waitForReady(1000);
	}
	
}
