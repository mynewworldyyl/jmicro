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
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.codec.typecoder.TypeCoder;
import cn.jmicro.common.CommonException;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月8日 上午11:43:13
 */
@Component(value="prefixTypeDecoder",lazy=false)
public class PrefixTypeEncoderDecoder{
	
	private static final Logger logger = LoggerFactory.getLogger(PrefixTypeEncoderDecoder.class);
	
	//@Inject
	//private TypeCoderFactory typeCf;
	
	TypeCoder<Object> dc = TypeCoderFactory.getDefaultCoder();
	
	@SuppressWarnings("unchecked")
	public <V> V decode(ByteBuffer buffer) {
		
		byte prefixCodeType = buffer.get(buffer.position());
		if(prefixCodeType == Decoder.PREFIX_TYPE_NULL){
			//空值直接返回
			return null;
		}
		JDataInput input = new JDataInput(buffer);
		
		return (V)dc.decode(input, null, null);
	}
	
	
	@Cfg(value="/OnePrefixTypeEncoder",defGlobal=true,required=true)
	private int encodeBufferSize = 4092;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ByteBuffer encode(Object obj) {
		if(obj == null) {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			buffer.put(Decoder.PREFIX_TYPE_NULL);
			//空值直接返回
			return buffer;
		} 

		//buffer = ByteBuffer.allocate(encodeBufferSize);
		//入口从Object的coder开始
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		//field declare as Object.class in order to put type info any way
		//从此进入时,字段声明及泛型类型都是空,区别于从反射方法进入
		try {
			JDataOutput dos = new JDataOutput(1);
			
			/*dos.write(Decoder.PREFIX_TYPE_PROXY);
			short code = TypeCoderFactory.getCodeByClass(obj.getClass());
			dos.writeShort(code);
			SerializeObject so = SerializeProxyFactory.getSerializeCoder(obj.getClass());
			so.encode(dos,obj);*/
			
			coder.encode(dos, obj, null,null);
			return  dos.getBuf();
		} catch (IOException e) {
			throw new CommonException("encode error:"+obj.toString(),e);
		}finally {
			
		}
	}
	
}
