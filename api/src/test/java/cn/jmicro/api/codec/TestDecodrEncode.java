package cn.jmicro.api.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TestDecodrEncode {

	@Test
	public void testEncode(){
		TestEntity e = new TestEntity();
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

}
