package cn.jmicro.api.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.gateway.ApiResponse;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcResponse;
import cn.jmicro.api.test.Person;
import cn.jmicro.common.Constants;

public class TestOnePrefixCoder {

	private OnePrefixTypeEncoder encoder = new OnePrefixTypeEncoder();
	private OnePrefixDecoder decoder = new OnePrefixDecoder();
	
	@Test
	public void testEndoceArrayResult(){
		RpcResponse resp = new RpcResponse(1,new Integer[]{1,2,3});
		resp.setSuccess(true);
		resp.getParams().put("key01", 3);
		resp.getParams().put("key02","hello");
		resp.setMsg(new Message());
		resp.setResult("Hell result");
		
		ByteBuffer dest = encoder.encode(resp);
		dest.flip();
		
		RpcResponse result = decoder.decode(dest);
		Object r = result.getResult();
		
		System.out.println(r.toString());
	}

	public static final class EntityEndoceArrayResult {

		private long v=222;
		private String str = null;
		private Object hello = "Hello World";
		private Person p = new Person();
		String[] arrs = {"56","2","67"};
		private List<Person> persons = new ArrayList<Person>();
		{
			persons.add(new Person());
		}
		@Override
		public String toString() {
			return "hello=" + this.hello +", value = "/*+ v+",list:"+list+",map: "+map*/;
		}
	}
	
	@Test
	public void testEncode(){
		EntityEndoceArrayResult e = new EntityEndoceArrayResult();
		ByteBuffer bb = encoder.encode(e);
		bb.flip();
		
		System.out.println(bb.limit());
		System.out.println(bb.array());
		
		EntityEndoceArrayResult o = decoder.decode(bb);
		System.out.println(o);
		
	}
	
	public static final class EntityEndoceArrayResult1 {
		private List<Person> persons = new ArrayList<Person>();
		{
			persons.add(new Person());
		}
		@Override
		public String toString() {
			return persons.toString();
		}
	}
	
	@Test
	public void testInnerListEncode1(){
		EntityEndoceArrayResult1 e = new EntityEndoceArrayResult1();
		ByteBuffer bb = encoder.encode(e);
		bb.flip();
		
		System.out.println(bb.limit());
		System.out.println(bb.array());
		
		EntityEndoceArrayResult1 o = decoder.decode(bb);
		System.out.println(o);
		
	}
	
	
	@Test
	public void testEncodeListEntity(){
		List<Person> persons = new ArrayList<Person>();
		persons.add(new Person());
		
		ByteBuffer bb = encoder.encode(persons);
		bb.flip();
		
		System.out.println(bb.limit());
		System.out.println(bb.array());
		
		persons = decoder.decode(bb);
		System.out.println(persons);
		
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
	public void testEncodeArray1(){
		Object[] arrs = {"56",new Person(),new TestEntity()};
		ByteBuffer bb = encoder.encode(arrs);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		arrs = decoder.decode(bb);
		
		System.out.println(arrs);
	}
	
	public static final class EntityEndoceInnerArray {
		private Object[] arrs = {"56",new Person(),111};
		@Override
		public String toString() {
			return arrs.toString();
		}
	}
	
	@Test
	public void testEncodeInnerArray1(){
		EntityEndoceInnerArray arrs = new EntityEndoceInnerArray();
		ByteBuffer bb = encoder.encode(arrs);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		arrs = decoder.decode(bb);
		
		System.out.println(arrs);
	}
	

	@Test
	public void testEncodeInnerArray2(){
		ApiRequest req = new ApiRequest();
		String[] args = new String[] {"hello smsg"};
		req.setArgs(args);
		req.setMethod("hello");
		req.setNamespace("testnamsp");
		req.setReqId(22L);
		req.setServiceName("ssss");
		req.setVersion("0.0.1");
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_RAW);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setId(0);
		msg.setReqId(0L);
		msg.setLinkId(0);
		ByteBuffer payload = encoder.encode(req);
		payload.flip();
		msg.setPayload(payload);
		msg.setVersion(Message.MSG_VERSION);
		
		ByteBuffer msgBuffer = encoder.encode(msg);
		msgBuffer.flip();
		
		Message respMsg = decoder.decode(msgBuffer);
		
		ApiRequest respReq = decoder.decode((ByteBuffer)respMsg.getPayload());
		
		System.out.println(respReq.getServiceName());
	}
	
	
	@Test
	public void testApiResponse(){
		ApiResponse req = new ApiResponse();
		req.setReqId(22L);
		req.setId(0L);
		req.setResult("result");
		req.setSuccess(true);
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_RAW);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		msg.setId(0);
		msg.setReqId(0L);
		msg.setLinkId(0);
		ByteBuffer payload = encoder.encode(req);
		payload.flip();
		msg.setPayload(payload);
		msg.setVersion(Message.MSG_VERSION);
		req.setMsg(msg);
		
		ByteBuffer msgBuffer = encoder.encode(msg);
		msgBuffer.flip();
		
		Message respMsg = decoder.decode(msgBuffer);
		
		ApiResponse resp = decoder.decode((ByteBuffer)respMsg.getPayload());
		
		System.out.println(resp.getResult());
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
	public void testEncodeMap1(){
		ByteBuffer bb = null;
		
		Map<String,Object> map = new HashMap<>();
		{
			map.put("1","testStringVal");
			map.put("2",new Person());
			map.put("3",new TestEntity());
		}
		
		bb = encoder.encode(map);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		map = decoder.decode(bb);
		
		System.out.println(map);
	}
	
	public static final class EntityEndoceNullField {
		private Person p = new Person();
		//private Person p = null;
		private TestEntity e = null;
		
		private Person nullField = new Person();
		{
			nullField.setId(null);
			nullField.setUsername(null);
		}
	}
	
	@Test
	public void testNullFieldObj(){
		ByteBuffer bb = encoder.encode(new EntityEndoceNullField());
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		EntityEndoceNullField e = decoder.decode(bb);
		
		System.out.println(e);
		
	}
	
	@Test
	public void testEncodeResponse(){
		RpcResponse si = new RpcResponse();
		
		si.setResult(MC.MTMS_TYPES_ARR);
		
		ByteBuffer bb = encoder.encode(si);
		
		bb.flip();
		
		RpcResponse si1 = decoder.decode(bb);
		
		Integer[]  arr = (Integer[])si1.getResult();
		
		System.out.println(arr);
		
	}
	
	@Test
	public void testIntegerArrayEncodeDecoder(){
		TestEntity si = new TestEntity();
		
		ByteBuffer bb = encoder.encode(si);
		
		bb.flip();
		
		TestEntity si1 = decoder.decode(bb);
		
		//Integer[]  arr = (Integer[])si1.types;
		//System.out.println(arr);
		
	}
	
	@Test
	public void testByteArrayEncodeDecoder(){
		TestEntity si = new TestEntity();
		
		ByteBuffer bb = encoder.encode(si);
		
		bb.flip();
		
		TestEntity si1 = decoder.decode(bb);
		
		System.out.println(si1.data);
		
	}

}
