package org.jmicro.api.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.net.RpcResponse;
import org.junit.Assert;
import org.junit.Test;

public class TestPrefixTypeED {

	private PrefixTypeEncoder encoder = new PrefixTypeEncoder();
	
	private PrefixTypeDecoder decoder = new PrefixTypeDecoder();
	
	@Test
	public void testPrivimiteInt() {
		
		int intArra = 3;
		
		ByteBuffer bb = encoder.encode(intArra);
		
		bb.flip();
		
		int intArr1 = decoder.decode(bb);
		
		System.out.println(intArr1);
		Assert.assertEquals(intArra, intArr1);
	}
	
	@Test
	public void testPrivimiteArray() {
		
		int[] intArra = {1,2,3};
		
		ByteBuffer bb = encoder.encode(intArra);
		
		bb.flip();
		
		int[] intArr1 = decoder.decode(bb);
		
		System.out.println(intArr1);
		Assert.assertArrayEquals(intArra, intArr1);
	}
	
	@Test
	public void testPrivimiteByteArray() {
		
		byte[] intArra = {1,2,3};
		
		ByteBuffer bb = encoder.encode(intArra);
		
		bb.flip();
		
		byte[] intArr1 = decoder.decode(bb);
		
		System.out.println(intArr1.length);;
		
		Assert.assertArrayEquals(intArra, intArr1);
	}
	
	@Test
	public void testSet() {
		
		Set<Integer> intArra = new HashSet<>();
		intArra.add(11);
		intArra.add(13);
		
		ByteBuffer bb = encoder.encode(intArra);
		
		bb.flip();
		
		Set<Integer> intArr1 = decoder.decode(bb);
		
		Assert.assertNotNull(intArr1);
		System.out.println(intArr1);;
		
	}
	
	@Test
	public void testList() {
		
		List<Integer> intArra = new ArrayList<>();
		intArra.add(11);
		intArra.add(13);
		
		ByteBuffer bb = encoder.encode(intArra);
		
		bb.flip();
		
		List<Integer> intArr1 = decoder.decode(bb);
		
		Assert.assertNotNull(intArr1);
		System.out.println(intArr1);
		
	}
	
	public static final class ListEntity{
		public List<Integer> intArra = new ArrayList<>();
	}
	
	@Test
	public void testListEntity() {
		ListEntity intArra = new ListEntity();
		intArra.intArra.add(11);
		intArra.intArra.add(13);
		
		ByteBuffer bb = encoder.encode(intArra);
		
		bb.flip();
		
		ListEntity intArr1 = decoder.decode(bb);
		
		Assert.assertNotNull(intArr1);
		System.out.println(intArr1.intArra);;
	}
	
	
	public static final class PrivimiteIntArrayEntity{
		private int[] int1 = {11,12};
		//private int[] int2 = {21,22};
	}
	
	@Test
	public void testPrivimite() {
		byte d = 127;
		System.out.println((byte)(1+d));
		
		PrivimiteIntArrayEntity pe = new PrivimiteIntArrayEntity();
		
		ByteBuffer bb = encoder.encode(pe);
		
		bb.flip();
		
		PrivimiteIntArrayEntity ped = decoder.decode(bb);
		
		System.out.println(ped);
		
	}
	
	public static final class ComplexEntity{
		
		public Map<String,Object> params = new HashMap<String,Object>();;
		private String serviceName;
		private String method;
		private Object[] args;
		private String namespace;
		private String version;
		private String impl;
		private String transport;
		protected Long reqId = -1L;
		private transient ISession session;
		private transient boolean isMonitorEnable = false;
		private transient Message msg;
		private transient boolean success = false;
		private transient boolean finish = false;
	}
	
	@Test
	public void testComplexEntity() {
		
		ComplexEntity pe = new ComplexEntity();
		/*pe.params = new HashMap<String,Object>();
		pe.params.put("objv", new Object());
		pe.params.put("sss", new ComplexEntity());*/
		
		ByteBuffer bb = encoder.encode(pe);
		
		bb.flip();
		
		ComplexEntity ped = decoder.decode(bb);
		
		System.out.println(ped);
		
	}
	
	public static final class ObjectEntity{		
		public Object result = null;
		//public int intv = 22;
	}

	@Test
	public void testObjectAsFieldEntity() {
		
		ObjectEntity pe = new ObjectEntity();
		//pe.result = new int[]{11};
		//pe.result = new int[]{11,22,33};
		
		pe.result = new Integer[]{11,22,33};
		//pe.result = 22;
		//pe.result = new Integer(22);
		
		/*ComplexEntity ce = new ComplexEntity();
		ce.params = new HashMap<String,Object>();
		ce.params.put("objv", new Object());
		ce.params.put("sss", new ComplexEntity());
		pe.result = ce;*/
		
		ByteBuffer bb = encoder.encode(pe);
		
		bb.flip();
		
		ObjectEntity ped = decoder.decode(bb);
		Integer[] rs = (Integer[])ped.result;
		System.out.println(ped);
		
	}
	
	
	@Test
	public void testRpcRequest() {
		
		RpcRequest pe = new RpcRequest();
		
		ByteBuffer bb = encoder.encode(pe);
		
		bb.flip();
		
		RpcRequest ped = decoder.decode(bb);
		
		System.out.println(ped);
		
	}
	
	@Test
	public void testRpcResponse() {
		
		RpcResponse pe = new RpcResponse();
		pe.setResult(new int[] {22,33,55});
		
		ByteBuffer bb = encoder.encode(pe);
		
		bb.flip();
		
		RpcResponse ped = decoder.decode(bb);
		int[] rs = (int[])ped.getResult();
		System.out.println(rs);
		
	}
	
	public static final class RpcResponse1{		
		private long id;
		
		private transient Message msg;
		
		private Long reqId;
		
	    private boolean isMonitorEnable = false;
		
		private boolean success = true;
		
		public Object result = new int[] {22,33,66};
		
		public Object result1 = new Integer[] {22,33,66};
	}
	
	@Test
	public void testRpcResponse1() {
		
		RpcResponse1 pe = new RpcResponse1();
		ByteBuffer bb = encoder.encode(pe);
		
		bb.flip();
		
		RpcResponse1 ped = decoder.decode(bb);
		int[] rs = (int[])ped.result;
		Integer[] rs2 = (Integer[])ped.result1;
		System.out.println(rs);
		
	}
}
