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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.classloader.RpcClassLoader;
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
	}
	
	private IDecoder<ByteBuffer> byteBufferDecoder = new IDecoder<ByteBuffer>(){
		@Override
		public <R> R decode(ByteBuffer data,Class<R> clazz) {
			//return (R)Decoder.decodeObject(data);
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(cl);
				return (R)prefixCoder.decode(data);
			}catch (Throwable e) {
				logger.error(e.getMessage() + ": " + clazz.getName());
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
