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

import org.jmicro.api.annotation.CodecFactory;
import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:17
 */
@CodecFactory(Constants.DEFAULT_CODEC_FACTORY)
public class DefaultCodecFactory implements ICodecFactory{

	private Decoder dec = new Decoder();
	
	private Encoder enc = new Encoder();
	
	@Override
	public IDecoder getDecoder() {
		return dec;
	}

	@Override
	public IEncoder getEncoder() {
		return enc;
	}

}
