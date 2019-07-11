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
package org.jmicro.api.codec;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.IDecoder;
import org.jmicro.api.codec.IEncoder;
import org.jmicro.api.codec.OnePrefixDecoder;
import org.jmicro.api.codec.OnePrefixTypeEncoder;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月26日 下午8:51:47
 */
@Component(value=Constants.DEFAULT_CODEC_FACTORY,lazy=false)
public class SimpleCodecFactory implements ICodecFactory{

	private Map<Byte,IDecoder> decoders = new HashMap<>();
	
	private Map<Byte,IEncoder> encoders = new HashMap<>();
	
	@Cfg(value="/respBufferSize")
	private int defaultEncodeBufferSize = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	@Inject
	private IRegistry registry;
	
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
			return (R)prefixCoder.decode(data);
		}
	};
	
	private IEncoder<ByteBuffer> byteBufferEncoder = new IEncoder<ByteBuffer>(){
		@Override
		public ByteBuffer encode(Object obj) {
			ByteBuffer bb = null;
			//ByteBuffer.allocate(defaultEncodeBufferSize);
			//Encoder.encodeObject(bb,obj);
			bb = prefixCoder.encode(obj);
			//bb.flip();
			return bb;
		}
	};
	
	private IDecoder<String> jsonDecoder = new IDecoder<String>(){
		@Override
		public <R> R decode(String json, Class<R> clazz) {
			R obj = JsonUtils.getIns().fromJson(json, clazz);
			if(clazz == RpcRequest.class){
				RpcRequest r = (RpcRequest)obj;
				r.setArgs(getArgs(r.getServiceName(),r.getMethod(),r.getArgs()));
			} else if(clazz == ApiRequest.class) {
				ApiRequest r = (ApiRequest)obj;
				r.setArgs(getArgs(r.getServiceName(),r.getMethod(),r.getArgs()));
			}
			return obj;
		}
	};
	
	private IEncoder<String> jsonEncoder = new IEncoder<String>(){
		@Override
		public String encode(Object obj) {
			return JsonUtils.getIns().toJson(obj);
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
	
	private Object[] getArgs(String srvCls,String methodName,Object[] jsonArgs){

		if(jsonArgs== null || jsonArgs.length ==0){
			return new Object[0];
		} else {
			int argLen = jsonArgs.length;
			//ServiceItem item = registry.getServiceByImpl(r.getImpl());
			Class<?> srvClazz = JMicro.getObjectFactory().loadCls(srvCls);
			if(srvClazz == null) {
				throw new CommonException("Class ["+srvCls+"] not found");
			}
			
			Object[] args = new Object[jsonArgs.length];
			
			for(Method sm : srvClazz.getMethods()){
				if(sm.getName().equals(methodName) &&
						argLen == sm.getParameterCount()){
					Class<?>[] clses = sm.getParameterTypes();
					int i = 0;
					try {
						for(; i < argLen; i++){
							Class<?> pt = clses[i];
							Object a = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(jsonArgs[i]), pt);
							args[i] = a;
						}
					} catch (Exception e) {
						continue;
					}
					if( i == argLen) {
						break;
					}
				}
			}
			return args;
		}
	
	
	}
	
}
