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
package org.jmicro.example.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.jmicro.api.JMicro;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.IDecoder;
import org.jmicro.api.codec.IEncoder;
import org.jmicro.api.codec.ISerializeObject;
import org.jmicro.api.codec.PrefixTypeDecoder;
import org.jmicro.api.codec.PrefixTypeEncoder;
import org.jmicro.api.codec.TypeCoderFactory;
import org.jmicro.api.codec.typecoder.ReflectTypeCoder;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.test.Person;
import org.jmicro.common.util.ReflectUtils;
import org.junit.Test;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

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
public class TestCodec {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testEncodeDecode() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[]{"-DinstanceName=TestCodec"});
		ICodecFactory codeFactory = of.get(ICodecFactory.class);
		
		IEncoder encoder = codeFactory.getEncoder(Message.PROTOCOL_BIN);
		
		RpcResponse resp = new RpcResponse(1,new Integer[]{1,2,3});
		resp.setId(33l);
		resp.setMonitorEnable(true);
		resp.setSuccess(true);
		resp.getParams().put("key01", 3);
		resp.getParams().put("key02","hello");
		resp.setMsg(new Message());
		
		ByteBuffer bb = (ByteBuffer) encoder.encode(resp);
		
		IDecoder decoder = codeFactory.getDecoder(Message.PROTOCOL_BIN);
		resp = (RpcResponse)decoder.decode(bb, RpcResponse.class);
		System.out.println(resp);
	}
	
	@Test
	public void testEncodeObjectByProxy() throws IOException {
		TypeCoderFactory.registClass(SerializeObject.class);
		TypeCoderFactory.registClass(Person.class);
		
		//SerializeObject obj = SerializeProxyFactory.(SerializeObject.class);
		SerializeObject obj = new SerializeObject();
		
		Person p = new Person();
		p.setId(222222);
		p.setUsername("测试用户名测试用户名测试用户名测试用户名测试用户名");
		obj.setv.add(p);
		/*obj.listv.add(p);
		obj.mapv.put("psssssssss", p);*/
		
		PrefixTypeEncoder encoder = new PrefixTypeEncoder();
		ByteBuffer bb = encoder.encode(obj);
		PrefixTypeDecoder decoder = new PrefixTypeDecoder();
		SerializeObject obj1 = decoder.decode(bb);
		
		System.out.println("PrefixTypeEncoder: " + bb.limit());
		
	}
	
	@Test
	public void testCompareEncodeObject() throws IOException {
		TypeCoderFactory.registCoder(new ReflectTypeCoder<SerializeObject>(TypeCoderFactory.type(), SerializeObject.class));
		SerializeObject obj = new SerializeObject();
		
		PrefixTypeEncoder encoder = new PrefixTypeEncoder();
		ByteBuffer bb = encoder.encode(obj);
		System.out.println("PrefixTypeEncoder: " + bb.limit());
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(obj);
		System.out.println("ObjectOutputStream: "+bos.size());
		
		//Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节
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
		SerializeObject obj = new SerializeObject();
		
		Person p = new Person();
		p.setId(222222);
		p.setUsername("测试用户名测试用户名测试用户名测试用户名测试用户名");
		/*obj.listv.add(p);
		obj.setv.add(p);
		obj.mapv.put("psssssssss", p);*/
		
		long startTime = System.currentTimeMillis();
		int cnt = 1000000;
		PrefixTypeEncoder encoder = new PrefixTypeEncoder();
		PrefixTypeDecoder decoder = new PrefixTypeDecoder();
		TypeCoderFactory.registCoder(new ReflectTypeCoder<SerializeObject>(TypeCoderFactory.type(), SerializeObject.class));
		Object v = null;
		for(int i = cnt; i > 0; i--) {
			ByteBuffer bb = encoder.encode(obj);
			//v = decoder.decode(bb);
		}
		System.out.println("PrefixTypeEncoder: " + (System.currentTimeMillis() - startTime));
		
		/*startTime = System.currentTimeMillis();
		for(int i = cnt; i > 0;i--) {
			ByteArrayOutputStream in = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(in);
			oos.writeObject(obj);
			
			ObjectInputStream ios = new ObjectInputStream(new ByteArrayInputStream(in.toByteArray()));
			v = ios.readObject();
			
		}
		System.out.println("ObjectOutputStream: " + (System.currentTimeMillis() - startTime));*/
		
		//Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节
		startTime = System.currentTimeMillis();
		Schema schema = RuntimeSchema.getSchema(obj.getClass());
		SerializeObject o = new SerializeObject();
		for(int i = cnt; i > 0;i--) {
			LinkedBuffer buf = LinkedBuffer.allocate(1024);
			byte[] data = ProtobufIOUtil.toByteArray(obj, schema, buf);
		}
		System.out.println("Protobuf: " + (System.currentTimeMillis() - startTime));
		
		Kryo kryo = new Kryo();  
        kryo.setReferences(false);  
        kryo.setRegistrationRequired(false);  
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());  
        kryo.register(Person.class);  
        
        startTime = System.currentTimeMillis();
        o = null;
        for (int i = cnt; i > 0;i--) {  
        	ByteArrayOutputStream bi = new ByteArrayOutputStream();
            Output output = new Output(bi);  
            kryo.writeObject(output, obj);  
            output.flush();
            output.close();
            
          /*  Input input = new Input(new ByteArrayInputStream(bi.toByteArray()));  
            o = kryo.readObject(input, SerializeObject.class);*/
        }  
        
        System.out.println("Kryo: " + (System.currentTimeMillis() - startTime));
	        
	}
	
	@Test
	public void testCompareEncodePerfermance1() throws IOException, ClassNotFoundException {
		
		SerializeObject obj = new SerializeObject();
		
		System.out.println(obj instanceof ISerializeObject);
		
		Person p = new Person();
		p.setId(222222);
		p.setUsername("测试用户名测试用户名测试用户名测试用户名测试用户名");
		obj.setv.add(p);
		/*obj.listv.add(p);
		obj.mapv.put("psssssssss", p);*/
		
		//obj.listv.add(p);
		
		
		int cnt = 1000000;
		PrefixTypeEncoder encoder = new PrefixTypeEncoder();
		PrefixTypeDecoder decoder = new PrefixTypeDecoder();

		TypeCoderFactory.registClass(SerializeObject.class);
		TypeCoderFactory.registClass(Person.class);
		
		Object v = null;
		ByteBuffer bb = null;
		
		long startTime = System.currentTimeMillis();
		for(int i = cnt; i > 0; i--) {
			 bb = encoder.encode(obj);
		}
		System.out.println("PrefixTypeEncoder: " + (System.currentTimeMillis() - startTime));
		
		/*startTime = System.currentTimeMillis();
		for(int i = cnt; i > 0;i--) {
			ByteArrayOutputStream in = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(in);
			oos.writeObject(obj);
			ObjectInputStream ios = new ObjectInputStream(new ByteArrayInputStream(in.toByteArray()));
			v = ios.readObject();
			
			
		}
		System.out.println("ObjectOutputStream: " + (System.currentTimeMillis() - startTime));*/
		
		//Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节
		Schema schema = RuntimeSchema.getSchema(obj.getClass());
		startTime = System.currentTimeMillis();
		for(int i = cnt; i > 0;i--) {
			LinkedBuffer buf = LinkedBuffer.allocate(1024);
			SerializeObject o = new SerializeObject();
			byte[] data = ProtobufIOUtil.toByteArray(obj, schema, buf);
		}
		System.out.println("Protobuf: " + (System.currentTimeMillis() - startTime));
		
		Kryo kryo = new Kryo();  
        kryo.setReferences(false);  
        kryo.setRegistrationRequired(false);  
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());  
        kryo.register(Person.class);  
        
        startTime = System.currentTimeMillis();
        for (int i = cnt; i > 0;i--) {  
        	
        	ByteArrayOutputStream bi = new ByteArrayOutputStream();
            Output output = new Output(bi);  
            kryo.writeObject(output, obj);  
            output.flush();
            output.close();
            
           Input input = new Input(new ByteArrayInputStream(bi.toByteArray()));  
            SerializeObject o = new SerializeObject();
            //o = kryo.readObject(input, SerializeObject.class);
        }  
        
        System.out.println("Kryo: " + (System.currentTimeMillis() - startTime));
	        
	}
	
	@Test
	public void testDesc2Class() throws ClassNotFoundException {
		String desc = "Ljava/util/Set<Lorg/jmicro/api/test/Person;>";
		Class<?> cls = ReflectUtils.desc2class(desc);
	}
	
	@Test
	public void testCompareDecodePerfermance1() throws IOException, ClassNotFoundException {
		
		SerializeObject obj = new SerializeObject();
		
		Person p = new Person();
		p.setId(222222);
		p.setUsername("测试用户名测试用户名测试用户名测试用户名测试用户名");
		obj.setv.add(p);
		/*obj.listv.add(p);
		obj.mapv.put("psssssssss", p);*/
		
		obj.listv.add(p);
		
		PrefixTypeEncoder encoder = new PrefixTypeEncoder();
		PrefixTypeDecoder decoder = new PrefixTypeDecoder();
		
		TypeCoderFactory.registClass(SerializeObject.class);
		TypeCoderFactory.registClass(Person.class);
		
		ByteBuffer bb = encoder.encode(obj);
		
		long startTime = System.currentTimeMillis();
		int cnt = 1000000;
		Object v = null;
		for(int i = cnt; i > 0; i--) {
			bb.mark();
			v = decoder.decode(bb);
			bb.reset();
		}
		System.out.println("PrefixTypeEncoder: " + (System.currentTimeMillis() - startTime));
		
		/*startTime = System.currentTimeMillis();
		for(int i = cnt; i > 0;i--) {
			ByteArrayOutputStream in = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(in);
			oos.writeObject(obj);
			ObjectInputStream ios = new ObjectInputStream(new ByteArrayInputStream(in.toByteArray()));
			v = ios.readObject();
			
			
		}
		System.out.println("ObjectOutputStream: " + (System.currentTimeMillis() - startTime));*/
		
		//Protobuf不序列化类型信息，其比PrefixTypeEncoder少5个字节
		
		Schema schema = RuntimeSchema.getSchema(obj.getClass());
		
		LinkedBuffer buf = LinkedBuffer.allocate(1024);
		SerializeObject o = new SerializeObject();
		byte[] data = ProtobufIOUtil.toByteArray(obj, schema, buf);
		
		startTime = System.currentTimeMillis();
		
		for(int i = cnt; i > 0;i--) {
			ProtobufIOUtil.mergeFrom(data, o, schema);
		}
		System.out.println("Protobuf: " + (System.currentTimeMillis() - startTime));
		
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
        for (int i = cnt; i > 0;i--) {  
            Input input = new Input(new ByteArrayInputStream(bi.toByteArray()));  
            o = new SerializeObject();
            //o = kryo.readObject(input, SerializeObject.class);
        }  
        
        System.out.println("Kryo: " + (System.currentTimeMillis() - startTime));
	        
	}
	

}
