package org.jmicro.common.test;

import java.nio.ByteBuffer;
import java.util.List;

import org.jmicro.common.Utils;
import org.jmicro.common.codec.Decoder;
import org.jmicro.common.codec.Encoder;
import org.junit.Test;

public class TestClass {

	@Test
	public void testGetLocalIp(){
		 List<String> ips = Utils.getIns().getLocalIPList();
		 System.out.println(ips);
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
