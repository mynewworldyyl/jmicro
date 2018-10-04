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
package org.jmicro.idgenerator;

import java.util.concurrent.atomic.AtomicLong;

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.annotation.Component;
import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:11:16
 */
@Component(Constants.DEFAULT_IDGENERATOR)
public class JMicroIdGenerator implements IIdGenerator {

	//private MysqlBaseIdMap mapper = new MysqlBaseIdMap();
	
	//private IIDGenerator gen = new BaseIDGenerator(mapper,"net.techgy",true);
	
	private AtomicLong idgenerator = new AtomicLong(1);
	
	@Override
	public long getLongId(Class<?> idType) {
		return idgenerator.getAndDecrement();
	}

	@Override
	public String getStringId(Class<?> idType) {
		return idgenerator.getAndDecrement()+"";
	}

}
