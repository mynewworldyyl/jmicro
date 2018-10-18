package org.jmicro.api.codec;

import java.nio.ByteBuffer;

import org.jmicro.api.net.RpcResponse;
import org.jmicro.common.Utils;
import org.jmicro.common.codec.Decoder;
import org.jmicro.common.codec.Encoder;
import org.junit.Test;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;

public class TestFiber {

	@SuppressWarnings("serial")
	@Test
	public void helloFiber() {
		new Fiber<String>() {
			@Override
			protected String run() throws SuspendExecution, InterruptedException {
				System.out.println("Hello Fiber");
				return "Hello Fiber";
			}
			
		}.start();
		
		Utils.getIns().waitForShutdown();
	}
	
	
	@Test
	public void testEndoceArrayResult(){
		RpcResponse resp = new RpcResponse(1,new Integer[]{1,2,3});
		
		ByteBuffer dest = ByteBuffer.allocate(1024);
		Encoder.encodeObject(dest, resp);
		dest.flip();
		
		RpcResponse result = Decoder.decodeObject(dest);
		Object r = result.getResult();
		System.out.println(r);
	}
}
