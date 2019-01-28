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

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.typecoder.TypeCoder;
import org.jmicro.common.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月8日 上午11:43:13
 */
@Component(value="prefixTypeDecoder",lazy=false)
public class PrefixTypeDecoder{
	
	private static final Logger logger = LoggerFactory.getLogger(PrefixTypeDecoder.class);
	
	//@Inject
	//private TypeCoderFactory typeCf;
	
	@SuppressWarnings("unchecked")
	public <V> V decode(ByteBuffer buffer) {
		
		buffer.mark();
		byte prefixCodeType = buffer.get();
		if(prefixCodeType == Decoder.PREFIX_TYPE_NULL){
			//空值直接返回
			return null;
		}
		buffer.reset();
		return (V)TypeCoderFactory.getDefaultCoder().decode(buffer, null, null);
	}
}
