/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.jmicro.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import cn.jmicro.api.codec.PrefixTypeEncoderDecoder;
import cn.jmicro.api.codec.TypeCoderFactory;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.test.Person;
import cn.jmicro.test.JMicroBaseTestCase;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * 
 * @author Yulei Ye
 *
 * @date: 2018年11月10日 下午9:23:25
 */
public class TestCodec  extends JMicroBaseTestCase{


	@Test
	public void testCompareEncodeObject() throws IOException {
	
		TestSerializeObject obj = new TestSerializeObject();
	
	    PrefixTypeEncoderDecoder decoder = of.get(PrefixTypeEncoderDecoder.class);

		ByteBuffer bb = decoder.encode(obj);
		System.out.println("PrefixTypeEncoder: " + bb.limit());

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(obj);
		System.out.println("ObjectOutputStream: " + bos.size());

		// Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节
		Schema schema = RuntimeSchema.getSchema(obj.getClass());
		byte[] dd = ProtobufIOUtil.toByteArray(obj, schema, LinkedBuffer.allocate(256));
		System.out.println("Protobuf: " + dd.length);

		Kryo kryo = new Kryo();
		kryo.setReferences(false);
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		kryo.register(Person.class);

		ByteArrayOutputStream bos1 = new ByteArrayOutputStream();

		Output output = new Output(bos1);
		kryo.writeObject(output, obj);
		output.flush();
		output.close();
		System.out.println("Kryo: " + bos1.size());
	}

	@Test
	public void testCompareEncodePerfermance() throws IOException, ClassNotFoundException {
		TestSerializeObject obj = this.getSO();
		String sp = "               ";
		System.out.println("Java对象序列化与反序列化工具性能比较");
		System.out.println("Count" + sp + "JMicro(MS)" + sp + "Protobuf(MS)" + sp + "JDK(MS)" + sp + "Kryo(MS)");

		int cnt = 10000;
		Long[] data = this.doDecodeEncode(cnt, obj);
		doPrint(data);

		cnt = 100000;
		data = this.doDecodeEncode(cnt, obj);
		doPrint(data);

		cnt = 1000000;
		data = this.doDecodeEncode(cnt, obj);
		doPrint(data);

		cnt = 10000000;
		data = this.doDecodeEncode(cnt, obj);
		doPrint(data);
	}

	private Long[] doDecodeEncode(long cnt, TestSerializeObject obj) throws IOException, ClassNotFoundException {

		Long[] data = new Long[5];
		data[0] = cnt;

		PrefixTypeEncoderDecoder decoder = of.get(PrefixTypeEncoderDecoder.class);

		Object v = null;
		ByteBuffer bb = null;

		long startTime = System.currentTimeMillis();
		for (long i = cnt; i > 0; i--) {
			bb = decoder.encode(obj);
			TestSerializeObject dob = decoder.decode(bb);
		}
		data[1] = System.currentTimeMillis() - startTime;

		// Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节
		Schema schema = RuntimeSchema.getSchema(obj.getClass());
		startTime = System.currentTimeMillis();
		for (long i = cnt; i > 0; i--) {
			LinkedBuffer buf = LinkedBuffer.allocate(1024);
			TestSerializeObject o = new TestSerializeObject();
			byte[] dataa = ProtobufIOUtil.toByteArray(obj, schema, buf);

			TestSerializeObject o1 = new TestSerializeObject();
			ProtobufIOUtil.mergeFrom(dataa, o1, schema);
			//System.out.println();
		}
		data[2] = System.currentTimeMillis() - startTime;

		startTime = System.currentTimeMillis();
		/*
		 * for(long i = cnt; i > 0;i--) { ByteArrayOutputStream in = new
		 * ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(in);
		 * oos.writeObject(obj); ObjectInputStream ios = new ObjectInputStream(new
		 * ByteArrayInputStream(in.toByteArray())); v = ios.readObject(); }
		 */
		data[3] = System.currentTimeMillis() - startTime;

		Kryo kryo = new Kryo();
		kryo.setReferences(true);
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		kryo.register(Person.class);
		kryo.register(TestSerializeObject.class);

		startTime = System.currentTimeMillis();
		for (long i = cnt; i > 0; i--) {

			ByteArrayOutputStream bi = new ByteArrayOutputStream();
			Output output = new Output(bi);
			kryo.writeObject(output, obj);
			output.flush();
			output.close();

			Input input = new Input(new ByteArrayInputStream(bi.toByteArray()));
			TestSerializeObject o = kryo.readObject(input, TestSerializeObject.class);
			//System.out.println();
		}

		data[4] = System.currentTimeMillis() - startTime;

		return data;

	}
	
	private TestSerializeObject getSO() {
		TestSerializeObject obj = new TestSerializeObject();
		obj.setArgs(new Object[] {"222","3333"});
		obj.setImpl("sssssss");
		obj.setMethod("method");
		obj.setNamespace("namespace");
		obj.setP(new Person());
		obj.setReqId(333L);
		obj.setServiceName("service");
		obj.setTransport("netty");
		obj.setVersion("0.0.1");
		return obj;
	}

	private String[] doEncode(int cnt, TestSerializeObject obj) throws IOException, ClassNotFoundException {

		String[] data = new String[5];

		data[0] = cnt + "";

		PrefixTypeEncoderDecoder decoder = new PrefixTypeEncoderDecoder();

		TypeCoderFactory.getIns().registClass(TestSerializeObject.class);
		TypeCoderFactory.getIns().registClass(Person.class);

		Object v = null;
		ByteBuffer bb = null;

		int size = 0;

		long startTime = System.currentTimeMillis();
		for (int i = cnt; i > 0; i--) {
			bb = decoder.encode(obj);
			size += bb.remaining();
		}
		data[1] = (System.currentTimeMillis() - startTime) + "," + size;
		// System.out.println(cnt + " PrefixTypeEncoder: " + (System.currentTimeMillis()
		// - startTime));

		// Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节
		size = 0;
		Schema schema = RuntimeSchema.getSchema(obj.getClass());
		startTime = System.currentTimeMillis();
		for (int i = cnt; i > 0; i--) {
			LinkedBuffer buf = LinkedBuffer.allocate(1024);
			TestSerializeObject o = new TestSerializeObject();
			byte[] dataa = ProtobufIOUtil.toByteArray(obj, schema, buf);
			size += dataa.length;
		}
		data[2] = (System.currentTimeMillis() - startTime) + "," + size;
		// System.out.println(cnt + " Protobuf: " + (System.currentTimeMillis() -
		// startTime));

		startTime = System.currentTimeMillis();
		size = 0;
		for (int i = cnt; i > 0; i--) {
			ByteArrayOutputStream in = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(in);
			oos.writeObject(obj);
			size += in.size();
			/*
			 * ObjectInputStream ios = new ObjectInputStream(new
			 * ByteArrayInputStream(in.toByteArray())); v = ios.readObject();
			 */
		}
		data[3] = (System.currentTimeMillis() - startTime) + "," + size;
		// System.out.println(cnt + " ObjectOutputStream: " +
		// (System.currentTimeMillis() - startTime));

		Kryo kryo = new Kryo();
		kryo.setReferences(false);
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		kryo.register(Person.class);

		size = 0;
		startTime = System.currentTimeMillis();
		for (int i = cnt; i > 0; i--) {

			ByteArrayOutputStream bi = new ByteArrayOutputStream();
			Output output = new Output(bi);
			kryo.writeObject(output, obj);
			output.flush();
			output.close();

			Input input = new Input(new ByteArrayInputStream(bi.toByteArray()));
			TestSerializeObject o = new TestSerializeObject();
			// o = kryo.readObject(input, SerializeObject.class);
			size += input.available();
		}
		data[4] = (System.currentTimeMillis() - startTime) + "," + size;
		// System.out.println(cnt + " Kryo: " + (System.currentTimeMillis() -
		// startTime));

		return data;
	}

	private void doPrint(Long[] data) {

		String sp = "               ";
		int cp = 0;
		int jp = cp + "Count".length() + sp.length();
		int pp = jp + "JMicro(MS)".length() + sp.length();
		int jdp = pp + "Protobuf(MS)".length() + sp.length();
		int kp = jdp + "JDK(MS)".length() + sp.length();

		StringBuffer sb = new StringBuffer(data[0] + "");

		for (; sb.length() < jp;) {
			sb.append(" ");
		}
		sb.append(data[1] + "");

		for (; sb.length() < pp;) {
			sb.append(" ");
		}
		sb.append(data[2] + "");

		for (; sb.length() < jdp;) {
			sb.append(" ");
		}
		sb.append(data[3] + "");

		for (; sb.length() < kp;) {
			sb.append(" ");
		}
		sb.append(data[4] + "");

		System.out.println(sb.toString());
	}

	private void doPrint(String[] data) {

		String sp = "               ";
		int cp = 0;
		int jp = cp + "Count".length() + sp.length();
		int pp = jp + "JMicro(MS)".length() + sp.length();
		int jdp = pp + "Protobuf(MS)".length() + sp.length();
		int kp = jdp + "JDK(MS)".length() + sp.length();

		StringBuffer sb = new StringBuffer(data[0] + "");

		for (; sb.length() < jp;) {
			sb.append(" ");
		}
		sb.append(data[1] + "");

		for (; sb.length() < pp;) {
			sb.append(" ");
		}
		sb.append(data[2] + "");

		for (; sb.length() < jdp;) {
			sb.append(" ");
		}
		sb.append(data[3] + "");

		for (; sb.length() < kp;) {
			sb.append(" ");
		}
		sb.append(data[4] + "");

		System.out.println(sb.toString());
	}

	@Test
	public void testCompareEncodePerfermance1() throws IOException, ClassNotFoundException {
		TestSerializeObject obj = this.getSO();
		String sp = "               ";
		System.out.println("Java对象序列化工具性能比较，时间意念毫秒，大小单位Byte");
		System.out.println("Count" + sp + "JMicro(MS)" + sp + "Protobuf(MS)" + sp + "JDK(MS)" + sp + "Kryo(MS)");

		int cnt = 10000;
		String[] data = this.doEncode(cnt, obj);
		doPrint(data);

		cnt = 100000;
		data = this.doEncode(cnt, obj);
		doPrint(data);

		cnt = 1000000;
		data = this.doEncode(cnt, obj);
		doPrint(data);

		/*
		 * cnt = 10000000; data = this.doEncode(cnt, obj); doPrint(data);
		 */

	}

	@Test
	public void testDesc2Class() throws ClassNotFoundException {
		String desc = "Ljava/util/Set<Lorg/jmicro/api/test/Person;>";
		Class<?> cls = ReflectUtils.desc2class(desc);
	}

	private Long[] doDecode(int cnt, TestSerializeObject obj) throws IOException, ClassNotFoundException {

		Long[] data = new Long[5];
		data[0] = new Long(cnt);

		PrefixTypeEncoderDecoder decoder = new PrefixTypeEncoderDecoder();

		TypeCoderFactory.getIns().registClass(TestSerializeObject.class);
		TypeCoderFactory.getIns().registClass(Person.class);

		ByteBuffer bb = decoder.encode(obj);

		long startTime = System.currentTimeMillis();
		Object v = null;
		for (int i = cnt; i > 0; i--) {
			bb.mark();
			v = decoder.decode(bb);
			bb.reset();
		}
		data[1] = System.currentTimeMillis() - startTime;

		// Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节

		Schema schema = RuntimeSchema.getSchema(obj.getClass());

		LinkedBuffer buf = LinkedBuffer.allocate(1024);
		TestSerializeObject o = null;
		byte[] dataa = ProtobufIOUtil.toByteArray(obj, schema, buf);

		startTime = System.currentTimeMillis();

		for (int i = cnt; i > 0; i--) {
			o = new TestSerializeObject();
			ProtobufIOUtil.mergeFrom(dataa, o, schema);
		}
		data[2] = System.currentTimeMillis() - startTime;

		startTime = System.currentTimeMillis();
		for (int i = cnt; i > 0; i--) {
			ByteArrayOutputStream in = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(in);
			oos.writeObject(obj);
			ObjectInputStream ios = new ObjectInputStream(new ByteArrayInputStream(in.toByteArray()));
			v = ios.readObject();
		}
		data[3] = System.currentTimeMillis() - startTime;

		Kryo kryo = new Kryo();
		kryo.setReferences(false);
		kryo.setRegistrationRequired(false);
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		kryo.register(Person.class);

		ByteArrayOutputStream bi = new ByteArrayOutputStream();
		Output output = new Output(bi);
		kryo.writeObject(output, obj);
		output.flush();
		output.close();

		startTime = System.currentTimeMillis();
		for (int i = cnt; i > 0; i--) {
			Input input = new Input(new ByteArrayInputStream(bi.toByteArray()));
			o = new TestSerializeObject();
			// o = kryo.readObject(input, SerializeObject.class);
		}

		data[4] = System.currentTimeMillis() - startTime;

		return data;
	}

	@Test
	public void testCompareDecodePerfermance1() throws IOException, ClassNotFoundException {

		TestSerializeObject obj = this.getSO();
		String sp = "               ";
		System.out.println("Java对象反序列化工具性能比较");
		System.out.println("Count" + sp + "JMicro(MS)" + sp + "Protobuf(MS)" + sp + "JDK(MS)" + sp + "Kryo(MS)");

		int cnt = 10000;
		Long[] data = this.doDecode(cnt, obj);
		doPrint(data);

		cnt = 100000;
		data = this.doDecode(cnt, obj);
		doPrint(data);

		cnt = 1000000;
		data = this.doDecode(cnt, obj);
		doPrint(data);

		cnt = 10000000;
		data = this.doDecode(cnt, obj);
		doPrint(data);

	}

	@Test
	public void testEncodeDecodeRpcRequest() throws IOException {
		RpcRequest req = new RpcRequest();
		req.setArgs(new Object[] { 1, "string" });
		req.setRequestId(111L);
		req.setImpl("tset");
		/*req.setMethod("method");
		req.setNamespace("ns");
		req.setServiceName("sn");
		req.setVersion("1");*/
		req.getParams().put("p2", "222");
		req.setSuccess(true);
		PrefixTypeEncoderDecoder decoder = new PrefixTypeEncoderDecoder();
		ByteBuffer bb = decoder.encode(req);

		RpcRequest obj1 = decoder.decode(bb);

		System.out.println("PrefixTypeEncoder: " + bb.limit());

	}
	
	

}
