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
package cn.jmicro.api.codec;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.codec.typecoder.TypeCoderUtils;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月26日 下午8:51:47
 */
@Component(value=Constants.DEFAULT_CODEC_FACTORY,lazy=false)
public class SimpleCodecFactory implements ICodecFactory{

	private final static Logger logger = LoggerFactory.getLogger(SimpleCodecFactory.class);
	
	private Map<Byte,IDecoder> decoders = new HashMap<>();
	
	private Map<Byte,IEncoder> encoders = new HashMap<>();
	
	@Cfg(value="/respBufferSize")
	private int defaultEncodeBufferSize = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	@Inject
	private IRegistry registry;
	
	@Inject
	private RpcClassLoader cl;
	
	/*@Inject
	private OnePrefixTypeEncoder onePrefixTypeEncoder;
	
	@Inject
	private OnePrefixDecoder onePrefixDecoder;*/
	
/*	@Inject
	private PrefixTypeEncoder prefixTypeEncoder;*/
	
	@Inject
	private PrefixTypeEncoderDecoder prefixCoder;
	
	public SimpleCodecFactory(){}
	
	@JMethod("init")
	public void init(){
		this.registDecoder(Message.PROTOCOL_BIN, byteBufferDecoder);
		this.registEncoder(Message.PROTOCOL_BIN, byteBufferEncoder);
		
		this.registDecoder(Message.PROTOCOL_JSON, jsonDecoder);
		this.registEncoder(Message.PROTOCOL_JSON, jsonEncoder);
		
		this.registDecoder(Message.PROTOCOL_EXTRA, extraBufferDecoder);
		this.registEncoder(Message.PROTOCOL_EXTRA, extraBufferEncoder);
	}
	
	private IDecoder<ByteBuffer> byteBufferDecoder = new IDecoder<ByteBuffer>(){
		@SuppressWarnings("unchecked")
		@Override
		public <R> R decode(ByteBuffer data,Class<R> clazz) {
			//return (R)Decoder.decodeObject(data);
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(cl);
				return (R)prefixCoder.decode(data);
			}catch (Throwable e) {
				logger.error("",e);
				throw e;
			} finally {
				Thread.currentThread().setContextClassLoader(c);
			}
		}
	};
	
	private IEncoder<ByteBuffer> byteBufferEncoder = new IEncoder<ByteBuffer>(){
		@Override
		public ByteBuffer encode(Object obj) {
			ByteBuffer bb = null;
			//ByteBuffer.allocate(defaultEncodeBufferSize);
			//Encoder.encodeObject(bb,obj);
			try {
				bb = prefixCoder.encode(obj);
			} catch (Throwable e) {
				logger.error(e.getMessage() + ": " +JsonUtils.getIns().toJson(obj));
				throw e;
			}
			//bb.flip();
			return bb;
		}
	};
	
	private IDecoder<Object> jsonDecoder = new IDecoder<Object>(){
		@Override
		public <R> R decode(Object data, Class<R> clazz) {
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			try {
				String json = "";
				if(data instanceof ByteBuffer) {
					ByteBuffer bb = (ByteBuffer)data;
					try {
						byte[] byteData = new byte[bb.remaining()];
						bb.get(byteData, 0, bb.remaining());
						json = new String(byteData,0,byteData.length,Constants.CHARSET);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}else if(data.getClass().isArray()) {
					byte[] arr = (byte[])data;
					try {
						json = new String(arr,0,arr.length,Constants.CHARSET);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}else if(data instanceof String) {
					json = (String)data;
				}
				Thread.currentThread().setContextClassLoader(cl);
				return JsonUtils.getIns().fromJson(json, clazz);
			}finally {
				Thread.currentThread().setContextClassLoader(c);
			}
			
		}
	};
	
	private IEncoder<ByteBuffer> jsonEncoder = new IEncoder<ByteBuffer>(){
		@Override
		public ByteBuffer encode(Object obj) {
			String json = JsonUtils.getIns().toJson(obj);
			byte[] data;
			try {
				data = json.getBytes(Constants.CHARSET);
				return ByteBuffer.wrap(data);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return null;
		}
	};
	
	private static Object decodeMap(JDataInput b) throws IOException {
		
		Map<String,Object> ed = new HashMap<>();

		int eleNum = b.readShort(); //extra元素个数
		if(eleNum < 0) {
			eleNum += 65537; //参考encode方法说明
		}
		
		if(eleNum == 0) return null;
		while(eleNum > 0) {
			String k = b.readUTF();
			Object v = doExtraDecode(b);
			ed.put(k, v);
			eleNum--;
		}
		return ed;
	}

	public static Object doExtraDecode(JDataInput b) throws IOException {
		
		byte type = b.readByte();
		
		if(type == DecoderConstant.PREFIX_TYPE_NULL) {
			return null;
		}else if(DecoderConstant.PREFIX_TYPE_BYTEBUFFER == type){
			int len = b.readUnsignedShort();
			if(len == 0) {
				return new byte[0];
			}
			byte[] arr = new byte[len];
			b.readFully(arr, 0, len);
			return arr;
		}else if(type == DecoderConstant.PREFIX_TYPE_INT){
			return b.readInt();
		}else if(DecoderConstant.PREFIX_TYPE_BYTE == type){
			return b.readByte();
		}else if(DecoderConstant.PREFIX_TYPE_SHORTT == type){
			return b.readUnsignedShort();
		}else if(DecoderConstant.PREFIX_TYPE_LONG == type){
			return b.readLong();
		}else if(DecoderConstant.PREFIX_TYPE_FLOAT == type){
			return b.readFloat();
		}else if(DecoderConstant.PREFIX_TYPE_DOUBLE == type){
			return b.readDouble();
		}else if(DecoderConstant.PREFIX_TYPE_BOOLEAN == type){
			return b.readBoolean();
		}else if(DecoderConstant.PREFIX_TYPE_CHAR == type){
			return b.readChar();
		}else if(DecoderConstant.PREFIX_TYPE_STRINGG == type){
			return JDataInput.readString(b);
		}else if(DecoderConstant.PREFIX_TYPE_MAP == type){
			return decodeMap(b);
		}else if(DecoderConstant.PREFIX_TYPE_SET == type){
			Set<Object> s = new HashSet<>();
			decodeColl(b,s);
			return s;
		}else if(DecoderConstant.PREFIX_TYPE_LIST == type){
			List<Object> l = new ArrayList<>();
			decodeColl(b,l);
			return l;
		}else if(DecoderConstant.PREFIX_TYPE_PROXY == type){
			return decodeMap(b);
		} else {
			throw new CommonException("not support header type: " + type);
		}
	}
	
	private static void decodeColl(JDataInput b, Collection<Object> s) throws IOException {
		int size = b.readShort();
		if(size < 0) {
			size += 66637;
		}
		while(size-- > 0) {
			Object v = doExtraDecode(b);
			if(v != null) {
				s.add(v);
			}
		}
	}

	private IDecoder<ByteBuffer> extraBufferDecoder = new IDecoder<ByteBuffer>(){
		@SuppressWarnings("unchecked")
		@Override
		public <R> R decode(ByteBuffer data, Class<R> clazz) {
			if(data == null || data.remaining() == 0) return null;
			JDataInput in = new JDataInput(data);
			//Map<String,Object> ps = decodeExtra(in);
			try {
				Object ps = doExtraDecode(in);
				if(Map.class.isAssignableFrom(clazz) 
					||Collection.class.isAssignableFrom(clazz)){
					return (R)ps;
				} else {
					ClassLoader c = Thread.currentThread().getContextClassLoader();
					try {
						String json = JsonUtils.getIns().toJson(ps);
						Thread.currentThread().setContextClassLoader(cl);
						return JsonUtils.getIns().fromJson(json, clazz);
					}finally {
						Thread.currentThread().setContextClassLoader(c);
					}
				}
			} catch (IOException e) {
				throw new CommonException("extraBufferDecoder", e);
			}
		}
	};
	
	private IEncoder<ByteBuffer> extraBufferEncoder = new IEncoder<ByteBuffer>(){
		@Override
		public ByteBuffer encode(Object obj) {
			try {
				JDataOutput dos = new JDataOutput(1);
				doExtraEncode(dos,obj);
				return dos.getBuf();
			} catch (Throwable e) {
				logger.error(e.getMessage() + ": " +JsonUtils.getIns().toJson(obj));
			}
			//bb.flip();
			return null;
		}
	};
	
	@SuppressWarnings("unchecked")
	public static void doExtraEncode(JDataOutput b, Object v) throws IOException {
		
		if(v == null) {
			b.writeByte(DecoderConstant.PREFIX_TYPE_NULL);
			return;
		}
		
		Class<?> cls = v.getClass();
		
		if(v instanceof Map) {
			b.write(DecoderConstant.PREFIX_TYPE_MAP);
			encodeExtraMap(b,(Map<String, Object>)v);
		} else if(v instanceof List) {
			b.write(DecoderConstant.PREFIX_TYPE_LIST);
			encodeColl(b, (List<Object>)v);
		}else if(v instanceof Set) {
			b.write(DecoderConstant.PREFIX_TYPE_SET);
			encodeColl(b, (Set<Object>)v);
		} else if(cls.isArray()){
			//数组只持byte[]
			if(!(cls.getComponentType() == Byte.class || cls.getComponentType() == Byte.TYPE)) {
				throw new CommonException("Only support byte array not: " + cls.getName());
			}
			b.writeByte(DecoderConstant.PREFIX_TYPE_BYTEBUFFER);
			byte[] arr = (byte[])v;
			b.writeUnsignedShort(arr.length);
			b.write(arr);
		}else if(cls == String.class) {
			b.writeByte(DecoderConstant.PREFIX_TYPE_STRINGG);
			String str = v.toString();
			JDataOutput.writeString(b, str);
		}else if(cls == int.class || cls == Integer.class || cls == Integer.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_INT);
			b.writeInt((Integer)v);
		}else if(cls == byte.class || cls == Byte.class || cls == Byte.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_BYTE);
			b.writeByte((Byte)v);
		}else if(cls == short.class || cls == Short.class || cls == Short.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_SHORTT);
			b.writeUnsignedShort((Short)v);
		}else if(cls == long.class || cls == Long.class || cls == Long.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_LONG);
			b.writeLong((Long)v);
		}else if(cls == float.class || cls == Float.class || cls == Float.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_BYTE);
			b.writeFloat((Byte)v);
		}else if(cls == double.class || cls == Double.class || cls == Double.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_DOUBLE);
			b.writeDouble((Double)v);
		}else if(cls == boolean.class || cls == Boolean.class || cls == Boolean.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_BOOLEAN);
			b.writeByte((Boolean)v?(byte)1:(byte)0);
		}else if(cls == char.class || cls == Character.class || cls == Character.TYPE){
			b.writeByte(DecoderConstant.PREFIX_TYPE_CHAR);
			b.writeChar((Character)v);
		} else  {
			b.writeByte(DecoderConstant.PREFIX_TYPE_PROXY);
			encodeByReflect(b,v);
		}
	}
	
	private static void encodeExtraMap(JDataOutput b, Map<String, Object> extras) throws IOException {
		b.writeShort(extras.size());//如果大于127，写入在小是负数，解码端需要做转换，参数Message.decodeExtra
		if(extras.size() == 0) return;
		//b.writeByte(Message.EXTRA_KEY_TYPE_STRING);//Map的Key的类型，此方法只支持字符串Key
		for(Map.Entry<String, Object> e : extras.entrySet()) {
			b.writeUTF(e.getKey());
			//encodeVal(b,e.getValue());
			doExtraEncode(b,e.getValue());
		}
	}

	private static void encodeColl(JDataOutput b, Collection<Object> s) throws IOException {
		b.writeShort(s.size());
		if(s.isEmpty()) return;
		Iterator<Object> ite = s.iterator();
		while(ite.hasNext()) {
			doExtraEncode(b,ite.next());
		}
	}

	private static void encodeByReflect(JDataOutput b, Object obj) throws IOException {

		// 进入此方法，obj必须不能为NULL
		Class<?> cls = obj.getClass();
		List<Field> fields = TypeCoderUtils.loadClassFieldsFromCache(cls);
		
		b.writeShort(fields.size());//如果大于127，写入的是负数，解码端需要做转换，参数Message.decodeExtra
		if(fields.size() == 0) return;
		//b.writeByte(Message.EXTRA_KEY_TYPE_STRING);//KEY的类型
		
		Object v = null;
		for(Field f : fields) {
			b.writeUTF(f.getName());
			v = TypeUtils.getFieldValue(obj, f);
			//Message.encodeVal(b,v);
			doExtraEncode(b,v);
		}
	}
	
	@Override
	public IDecoder getDecoder(Byte protocol) {
		if(decoders.containsKey(protocol)){
			return decoders.get(protocol);
		}
		return byteBufferDecoder;
	}

	@Override
	public IEncoder getEncoder(Byte protocol) {
		if(encoders.containsKey(protocol)){
			return encoders.get(protocol);
		}
		
		return byteBufferEncoder;
	}

	@Override
	public void registDecoder(Byte protocol,IDecoder<?> decoder) {
		if(decoders.containsKey(protocol)){
			IDecoder<?> d = decoders.get(protocol);
			throw new CommonException("Protocol ["+protocol+
					" have exists decoder [" + d.getClass().getName() + "]" );
		}
		decoders.put(protocol, decoder);
	}

	@Override
	public void registEncoder(Byte protocol,IEncoder encoder) {
		if(encoders.containsKey(protocol)){
			IEncoder e = encoders.get(protocol);
			throw new CommonException("Protocol ["+protocol+
					" have exists decoder [" + e.getClass().getName() + "]" );
		}
		encoders.put(protocol, encoder);
	}
	
}
