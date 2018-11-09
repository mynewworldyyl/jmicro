package org.jmicro.api.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.monitor.SubmitItem;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcResponse;
import org.junit.Test;

public class TestOnePrefixCoder {

	private OnePrefixTypeEncoder encoder = new OnePrefixTypeEncoder();
	private OnePrefixDecoder decoder = new OnePrefixDecoder();
	
	@Test
	public void testEndoceArrayResult(){
		RpcResponse resp = new RpcResponse(1,new Integer[]{1,2,3});
		resp.setId(33l);
		resp.setMonitorEnable(true);
		resp.setSuccess(true);
		resp.getParams().put("key01", 3);
		resp.getParams().put("key02","hello");
		resp.setMsg(new Message());
		
		ByteBuffer dest = encoder.encode(resp);
		dest.flip();
		
		RpcResponse result = decoder.decode(dest);
		Object r = result.getResult();
		
		System.out.println(r.toString());
	}

	@Test
	public void testEncode(){
		Entity e = new Entity();
		ByteBuffer bb = encoder.encode(e);
		bb.flip();
		
		System.out.println(bb.limit());
		System.out.println(bb.array());
		
		Entity o = decoder.decode(bb);
		System.out.println(o);
		
	}
	
	@Test
	public void testEncodeString(){
		ByteBuffer bb = encoder.encode("Hello World");
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		String o = decoder.decode(bb);
		System.out.println(o);
	}
	
	@Test
	public void testEncodeList(){
		List<String> list = new ArrayList<>();
		list.add("1");
		list.add("2");
		list.add("3");
		
		ByteBuffer bb = encoder.encode(list);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		list = decoder.decode(bb);
		System.out.println(list);
	}
	
	@Test
	public void testEncodeArray(){
		String[] arrs = {"56","2","67"};
		ByteBuffer bb = encoder.encode(arrs);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		arrs = decoder.decode(bb);
		
		System.out.println(arrs);
	}
	
	@Test
	public void testEncodeMap(){
		ByteBuffer bb = null;
		
		Map<String,Long> map = new HashMap<>();
		{
			map.put("1",222L);
			map.put("2",333L);
			map.put("3",555L);
		}
		
		bb = encoder.encode(map);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		map = decoder.decode(bb);
		
		System.out.println(map);
	}
	
	@Test
	public void testEncodeSubmitItem(){
		SubmitItem si = new SubmitItem();
		si.setFinish(true);
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
		si.setResult("sfs");
		
		ByteBuffer bb = ByteBuffer.allocate(1024*4);
		Encoder.encodeObject(bb, si);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.array());
		
		Object o = Decoder.decodeObject(bb);
		System.out.println(o);
		
	}

}
