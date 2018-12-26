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

import java.nio.ByteBuffer;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:25
 */
@Component(value="prefixTypeEncoder",lazy=false)
public class PrefixTypeEncoder implements IEncoder<ByteBuffer>{

	private static final Logger logger = LoggerFactory.getLogger(PrefixTypeEncoder.class);
	
	@Cfg(value="/OnePrefixTypeEncoder",defGlobal=true,required=true)
	private int encodeBufferSize = 4096;
	
	@Inject
	private TypeCoderFactory typeCf;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ByteBuffer encode(Object obj) {
		ByteBuffer buffer = null;
		if(obj == null) {
			buffer = ByteBuffer.allocate(32);
			TypeCoder<Void> coder = typeCf.getByClass(Void.class);
			coder.encode(buffer, null, Void.class,null);
		}else {
			buffer = ByteBuffer.allocate(encodeBufferSize);
			//入口从Object的coder开始
			TypeCoder coder = typeCf.getByClass(Object.class);
			//field declare as Object.class in order to put type info any way
			coder.encode(buffer, obj, Object.class,null);
		}
		return buffer;
	}
	
}
