package org.jmicro.api.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.Encoder;
import org.jmicro.api.monitor.SubmitItem;
import org.junit.Test;

public class TestDecodrEncode {

	@Test
	public void testEncode(){
		Entity e = new Entity();
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		Encoder.encodeObject(bb, e);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.array());
		
		Object o = Decoder.decodeObject(bb);
		System.out.println(o);
		
	}
	
	@Test
	public void testEncodeString(){
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		Encoder.encodeObject(bb, "Hello World");
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		String o = Decoder.decodeObject(bb);
		System.out.println(o);
		
	}
	
	@Test
	public void testEncodeList(){
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		List<String> list = new ArrayList<>();
		list.add("1");
		list.add("2");
		list.add("3");
		Encoder.encodeObject(bb, list);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		list = Decoder.decodeObject(bb);
		
		System.out.println(list);
	}
	
	@Test
	public void testEncodeArray(){
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		String[] arrs = {"56","2","67"};
		Encoder.encodeObject(bb, arrs);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		arrs = Decoder.decodeObject(bb);
		
		System.out.println(arrs);
	}
	
	@Test
	public void testEncodeMap(){
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		
		Map<String,Long> map = new HashMap<>();
		{
			map.put("1",222L);
			map.put("2",333L);
			map.put("3",555L);
		}
		
		Encoder.encodeObject(bb, map);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		map = Decoder.decodeObject(bb);
		
		System.out.println(map);
	}
	
	@Test
	public void testEncodeSubmitItem(){
		SubmitItem si = new SubmitItem();
		/*si.setFinish(true);
		si.setType(1);
		si.setReqId(1);
		si.setSessionId(1);
		si.setNamespace("sss");
		si.setVersion("fsa");
		si.setReqArgs("fsf");
		si.setMethod("sfs");
		si.setMsgId(1);
		si.setOthers("fsf");
		si.setRespId(1L);
		si.setResult("sfs");*/
		
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		Encoder.encodeObject(bb, si);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.array());
		
		Object o = Decoder.decodeObject(bb);
		System.out.println(o);
		
	}
}
