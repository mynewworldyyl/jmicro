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
package org.jmicro.api.client;

import org.jmicro.api.net.ISession;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:18
 */
public interface IClientSessionManager {
	
	public static final String SKEY = "_sessionKeyKey";
	
	public static final String CLOSE_SESSION_TIMER = "_closeSessionTimer";

	/**
	 * 同一个主机和端口连接，可以在不同的RPC之间做重用，提高请求性能，相当于keepalive属性
	 * @param host
	 * @param port
	 * @return
	 */
	IClientSession getOrConnect(String instanceName,String host,int port);
	
	void closeSession(ISession session);
	
}
