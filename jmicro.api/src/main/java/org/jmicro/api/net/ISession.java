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
package org.jmicro.api.net;

import java.nio.ByteBuffer;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:27
 */
public interface ISession{
	
	void close(boolean flag);
	
	Object getParam(String key);
	
	void putParam(String key,Object obj);
	
	//ByteBuffer getReadBuffer();
	int getReadBufferSize();
	
	//server write response, or client write no need response request
	void write(Message bb);
	
	public boolean isClose();
	
	void active();
	
	boolean isActive();
	
	String remoteHost();
	
	int remotePort();
	
	String localHost();
	
	int localPort();
	
    long getId();
	
	void setId(long id);
	
	void receive(ByteBuffer msg) ;
	
	void setDumpUpStream(boolean dump);
	
	void setDumpDownStream(boolean dump);
	
}
