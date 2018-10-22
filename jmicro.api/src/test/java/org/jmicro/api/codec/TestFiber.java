package org.jmicro.api.codec;

import java.nio.ByteBuffer;

import org.jmicro.api.net.RpcResponse;
import org.jmicro.common.codec.Decoder;
import org.jmicro.common.codec.Encoder;
import org.junit.Test;

public class TestFiber {

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
