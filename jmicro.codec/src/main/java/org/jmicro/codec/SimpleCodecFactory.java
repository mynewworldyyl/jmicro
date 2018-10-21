package org.jmicro.codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.IDecoder;
import org.jmicro.api.codec.IEncoder;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.codec.Decoder;
import org.jmicro.common.codec.Encoder;
import org.jmicro.common.util.JsonUtils;

@Component(value=Constants.DEFAULT_CODEC_FACTORY)
public class SimpleCodecFactory implements ICodecFactory{

	private Map<Byte,IDecoder> decoders = new HashMap<>();
	
	private Map<Byte,IEncoder> encoders = new HashMap<>();
	
	@Cfg(value="/respBufferSize")
	private int defaultEncodeBufferSize = 1024*4;
	
	@Inject
	private IRegistry registry;
	
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
			return (R)Decoder.decodeObject(data);
		}
	};
	
	private IEncoder<ByteBuffer> byteBufferEncoder = new IEncoder<ByteBuffer>(){
		@Override
		public ByteBuffer encode(Object obj) {
			ByteBuffer bb = ByteBuffer.allocate(defaultEncodeBufferSize);
			Encoder.encodeObject(bb,obj);
			bb.flip();
			return bb;
		}
	};
	
	private IDecoder<String> jsonDecoder = new IDecoder<String>(){
		@Override
		public <R> R decode(String json, Class<R> clazz) {
			R obj = JsonUtils.getIns().fromJson(json, clazz);
			if(clazz == RpcRequest.class){
				RpcRequest r = (RpcRequest)obj;
				if(r.getArgs() == null || r.getArgs().length ==0){
					return obj;
				} else {
					int argLen = r.getArgs().length;
					ServiceItem item = registry.getServiceByImpl(r.getImpl());
					Object[] args = new Object[r.getArgs().length];
					for(ServiceMethod sm : item.getMethods()){
						if(sm.getMethodName().equals(r.getMethod()) &&
								argLen == sm.getMethodParamTypes().split("-").length){
							String[] clses = sm.getMethodParamTypes().split("-");
							int i = 0;
							for(; i < argLen; i++){
								try {
									Class<?> pt = Thread.currentThread().getContextClassLoader().loadClass(clses[i]);
									Object a = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(r.getArgs()[i]), pt);
									args[i] = a;
								} catch (ClassNotFoundException e) {
									e.printStackTrace();
									continue;
								}
							}
							if( i == argLen) {
								break;
							}
						}
					}
					r.setArgs(args);
				}
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
	
}
