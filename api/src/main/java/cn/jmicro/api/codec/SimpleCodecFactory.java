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

import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.codec.typecoder.ITypeCoder;
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
	
	
	private Map<String,Object> decodeExtra(JDataInput b) {
		Map<String,Object> ed = new HashMap<>();
		try {
			int eleNum = b.readByte(); //extra元素个数
			if(eleNum < 0) {
				eleNum += 256; //参考encode方法说明
			}
			
			if(eleNum == 0) return null;
			while(eleNum > 0) {
				String k = b.readUTF();
				Object v = Message.decodeVal(b);
				ed.put(k, v);
				eleNum--;
			}
			return ed;
		} catch (IOException e) {
			throw new CommonException("decodeExtra error:" + ed.toString() + " IOException: " + e.getMessage());
		}
	}

	private IDecoder<ByteBuffer> extraBufferDecoder = new IDecoder<ByteBuffer>(){
		@SuppressWarnings("unchecked")
		@Override
		public <R> R decode(ByteBuffer data, Class<R> clazz) {
			JDataInput in = new JDataInput(data);
			Map<String,Object> ps = decodeExtra(in);
			if(Map.class.isAssignableFrom(clazz)) {
				return (R)ps;
			}else {
				ClassLoader c = Thread.currentThread().getContextClassLoader();
				try {
					String json = JsonUtils.getIns().toJson(ps);
					Thread.currentThread().setContextClassLoader(cl);
					return JsonUtils.getIns().fromJson(json, clazz);
				}finally {
					Thread.currentThread().setContextClassLoader(c);
				}
			}
		}
	};
	
	private IEncoder<ByteBuffer> extraBufferEncoder = new IEncoder<ByteBuffer>(){
		@Override
		public ByteBuffer encode(Object obj) {
			try {
				JDataOutput dos = new JDataOutput(1);
				if(obj instanceof Map) {
					encodeExtraMap(dos,(Map<String, Object>)obj);
				} else {
					encodeByReflect(dos,obj);
				}
				dos.getBuf();
			} catch (Throwable e) {
				logger.error(e.getMessage() + ": " +JsonUtils.getIns().toJson(obj));
			}
			//bb.flip();
			return null;
		}
	};
	
	private void encodeExtraMap(JDataOutput b, Map<String, Object> extras) {
		try {
			b.writeByte((byte)extras.size());//如果大于127，写入在小是负数，解码端需要做转换，参数Message.decodeExtra
		
			if(extras.size() == 0) return;
			
			b.writeByte(Message.EXTRA_KEY_TYPE_STRING);
			
		} catch (IOException e2) {
			throw new CommonException("encodeExtra extra size error");
		}
		
		for(Map.Entry<String, Object> e : extras.entrySet()) {
			try {
				b.writeUTF(e.getKey());
				Message.encodeVal(b,e.getValue());
			} catch (IOException e1) {
				throw new CommonException("encodeExtra key: " + e.getKey() +",val"+  e.getValue(),e1);
			}
		}
	}
	
	public static void encodeByReflect(JDataOutput b, Object obj) throws IOException {

		// 进入此方法，obj必须不能为NULL
		Class<?> cls = obj.getClass();
		List<Field> fields = TypeCoderUtils.loadClassFieldsFromCache(cls);
		try {
			b.writeByte(fields.size());//如果大于127，写入的是负数，解码端需要做转换，参数Message.decodeExtra
			if(fields.size() == 0) return;
			b.writeByte(Message.EXTRA_KEY_TYPE_STRING);
		} catch (IOException e2) {
			throw new CommonException("encodeExtra extra size error");
		}
		
		Object v = null;
		for(Field f : fields) {
			try {
				b.writeUTF(f.getName());
				v = TypeUtils.getFieldValue(obj, f);
				Message.encodeVal(b,v);
			} catch (IOException e) {
				throw new CommonException("encodeExtra key: " + f.getName() +",val"+  (v == null?"":v.toString()),e);
			}
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
