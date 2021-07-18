package cn.jmicro.api.codec;

import java.nio.ByteBuffer;

import org.junit.Test;

import cn.jmicro.api.codec.DecoderConstant;
import cn.jmicro.api.codec.Encoder;
import cn.jmicro.api.net.RpcResponseJRso;

public class TestFiber {

	@Test
	public void testEndoceArrayResult(){
		RpcResponseJRso resp = new RpcResponseJRso(1,new Integer[]{1,2,3});
		
		ByteBuffer dest = ByteBuffer.allocate(1024);
		Encoder.encodeObject(dest, resp);
		dest.flip();
		
		RpcResponseJRso result = Decoder.decodeObject(dest);
		Object r = result.getResult();
		System.out.println(r);
	}
	
	@Test
	public void testEndoceByteBuffer(){
		byte[] data = new byte[]{'a','b','c','d','e','f','g',};
		ByteBuffer src = ByteBuffer.wrap(data);
		ByteBuffer dest = ByteBuffer.allocate(64);
		Encoder.encodeObject(dest, src);
		dest.flip();
		
		ByteBuffer ebb = Decoder.decodeObject(dest);
		
		System.out.println(new String(ebb.array()));
	}
}
