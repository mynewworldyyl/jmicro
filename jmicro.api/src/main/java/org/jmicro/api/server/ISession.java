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
package org.jmicro.api.server;

import java.nio.ByteBuffer;

import org.jmicro.api.IDable;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:27
 */
public interface ISession extends IDable{
	
	void close(boolean flag);
	
	Object getParam(String key);
	
	void putParam(String key,Object obj);
	
	ByteBuffer getReadBuffer();
	
	//server write response, or client write no need response request
	void write(ByteBuffer bb);
	
}
