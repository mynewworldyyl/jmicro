package org.jmicro.example.test;

import java.nio.ByteBuffer;

import org.jmicro.api.JMicro;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.net.Message;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;

public class TestIdGenerator {

	@Test
	public void testLongIDGenerator(){
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[0]);
		of.start();
		
		IIdGenerator g = of.get(IIdGenerator.class);
		g.getLongId(Message.class);
	}
	
	@Test
	public void testByte2Short() {
		ByteBuffer bb = ByteBuffer.allocate(1);
		short v = 256;
		Message.writeUnsignedByte(bb, v);
		bb.flip();
		short vv = Message.readUnsignedByte(bb);
		System.out.println(vv);
	}
	
	@Test
	public void testShort2Int() {
		ByteBuffer bb = ByteBuffer.allocate(2);
		int v = 65529;
		Message.writeUnsignedShort(bb, v);
		bb.flip();
		int vv = Message.readUnsignedShort(bb);
		System.out.println(vv);
	}
	
	@Test
	public void testInt2Long() {
		ByteBuffer bb = ByteBuffer.allocate(4);
		long v = 4294967294L;
		Message.writeUnsignedInt(bb, v);
		bb.flip();
		long vv = Message.readUnsignedInt(bb);
		System.out.println(vv);
	}
}
