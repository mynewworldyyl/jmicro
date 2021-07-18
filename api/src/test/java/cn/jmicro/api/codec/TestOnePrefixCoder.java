package cn.jmicro.api.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import cn.jmicro.api.gateway.ApiRequestJRso;
import cn.jmicro.api.gateway.ApiResponseJRso;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcResponseJRso;
import cn.jmicro.api.test.PersonJRso;
import cn.jmicro.common.Constants;

public class TestOnePrefixCoder {

	private OnePrefixTypeEncoder encoder = new OnePrefixTypeEncoder();
	private OnePrefixDecoder decoder = new OnePrefixDecoder();
	
	@Test
	public void testEndoceArrayResult(){
		RpcResponseJRso resp = new RpcResponseJRso(1,new Integer[]{1,2,3});
		resp.setSuccess(true);
		resp.getParams().put("key01", 3);
		resp.getParams().put("key02","hello");
		resp.setMsg(new Message());
		resp.setResult("Hell result");
		
		ByteBuffer dest = encoder.encode(resp);
		dest.flip();
		
		RpcResponseJRso result = decoder.decode(dest);
		Object r = result.getResult();
		
		System.out.println(r.toString());
	}

	public static final class EntityEndoceArrayResult {

		private long v=222;
		private String str = null;
		private Object hello = "Hello World";
		private PersonJRso p = new PersonJRso();
		String[] arrs = {"56","2","67"};
		private List<PersonJRso> persons = new ArrayList<PersonJRso>();
		{
			persons.add(new PersonJRso());
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
		private List<PersonJRso> persons = new ArrayList<PersonJRso>();
		{
			persons.add(new PersonJRso());
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
		List<PersonJRso> persons = new ArrayList<PersonJRso>();
		persons.add(new PersonJRso());
		
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
		Object[] arrs = {"56",new PersonJRso(),new TestEntity()};
		ByteBuffer bb = encoder.encode(arrs);
		bb.flip();
		System.out.println(bb.limit());
		System.out.println(bb.position());
		
		arrs = decoder.decode(bb);
		
		System.out.println(arrs);
	}
	
	public static final class EntityEndoceInnerArray {
		private Object[] arrs = {"56",new PersonJRso(),111};
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
		ApiRequestJRso req = new ApiRequestJRso();
		String[] args = new String[] {"hello smsg"};
		req.setArgs(args);
		/*req.setMethod("hello");
		req.setNamespace("testnamsp");
		req.setServiceName("ssss");
		req.setVersion("0.0.1");*/
		
		req.setReqId(22L);
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		//msg.setId(0);
		msg.setMsgId(0L);
		msg.setLinkId(0L);
		ByteBuffer payload = encoder.encode(req);
		payload.flip();
		msg.setPayload(payload);
		//msg.setVersion(Message.MSG_VERSION);
		
		ByteBuffer msgBuffer = encoder.encode(msg);
		msgBuffer.flip();
		
		Message respMsg = decoder.decode(msgBuffer);
		
		ApiRequestJRso respReq = decoder.decode((ByteBuffer)respMsg.getPayload());
		
		System.out.println(respReq.getReqId());
	}
	
	
	@Test
	public void testApiResponse(){
		ApiResponseJRso req = new ApiResponseJRso();
		req.setReqId(22L);
		req.setId(0L);
		req.setResult("result");
		req.setSuccess(true);
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		msg.setUpProtocol(Message.PROTOCOL_BIN);
		//msg.setId(0);
		msg.setMsgId(0L);
		msg.setLinkId(0L);
		ByteBuffer payload = encoder.encode(req);
		payload.flip();
		msg.setPayload(payload);
		//msg.setVersion(Message.MSG_VERSION);
		req.setMsg(msg);
		
		ByteBuffer msgBuffer = encoder.encode(msg);
		msgBuffer.flip();
		
		Message respMsg = decoder.decode(msgBuffer);
		
		ApiResponseJRso resp = decoder.decode((ByteBuffer)respMsg.getPayload());
		
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
			map.put("2",new PersonJRso());
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
		private PersonJRso p = new PersonJRso();
		//private Person p = null;
		private TestEntity e = null;
		
		private PersonJRso nullField = new PersonJRso();
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
		RpcResponseJRso si = new RpcResponseJRso();
		
		si.setResult(MC.MTMS_TYPES_ARR);
		
		ByteBuffer bb = encoder.encode(si);
		
		bb.flip();
		
		RpcResponseJRso si1 = decoder.decode(bb);
		
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
