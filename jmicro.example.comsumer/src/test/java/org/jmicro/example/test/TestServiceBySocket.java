package org.jmicro.example.test;

import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.common.Utils;
import org.jmicro.example.api.ClientSocket;
import org.junit.Test;

public class TestServiceBySocket {

	@Test
	public void testService01(){
		ClientSocket cs = new ClientSocket("localhost",9800,(msg)->{
			System.out.println(new String(msg.getPayload()));
		});
		
		 RpcRequest req = new RpcRequest();
		 //req.setSession();
		 
		 //req.setImpl("org.jmicro.objfactory.simple.TestRpcService");
		 req.setServiceName("org.jmicro.objfactory.simple.ITestRpcService");
		 req.setMethod("hello");
		 req.setArgs(new Object[]{"Yyl"});
		 req.setNamespace("test");
		 req.setVersion("1.0.0");
		 req.setRequestId(1000L);
		 
		 Message msg = new Message();
		 msg.setType(Message.PROTOCOL_TYPE_BEGIN);
		 msg.setId(req.getRequestId());
		 msg.setPayload(req.encode());
		 msg.setExt((byte)0);
		 msg.setReq(true);
		 msg.setSessionId(222L);
		 msg.setVersion(req.getVersion());
			
		cs.writeMessage(msg);
		
		Utils.getIns().waitForShutdown();
	}
	
}
