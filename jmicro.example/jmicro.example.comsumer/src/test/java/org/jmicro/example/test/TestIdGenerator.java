package org.jmicro.example.test;

import java.nio.ByteBuffer;

import org.jmicro.api.JMicro;
import org.jmicro.api.idgenerator.IdClient;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.Message;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.common.Utils;
import org.junit.Test;

public class TestIdGenerator {

	@Test
	public void testLongIDGenerator(){
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {"-DinstanceName=testLongIDGenerator",
				"-Dorg.jmicro.api.idgenerator.IIdClient=idClient"});
		IdClient g = of.get(IdClient.class);
		g.getLongId(Message.class.getName());
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
	
	@Test
	public void testInt2Hex() {
		System.out.println(Integer.toBinaryString(0x80));
	}
	
	@Test
	public void testIdClientGetId() {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[] {
				"-DinstanceName=testIdClientGetId",
				"-Dclient=true"});
		
		IdClient idClient = of.getByName("idClient");
		
		String[] longId = idClient.getStringIds(IRequest.class.getName(), 3);
		System.out.println(Utils.getIns().toString(longId));

	}
}
