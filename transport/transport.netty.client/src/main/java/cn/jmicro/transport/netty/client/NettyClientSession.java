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
package cn.jmicro.transport.netty.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.client.IClientSession;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:35
 */
public class NettyClientSession  extends AbstractNettyClientSession implements IClientSession{

	static final Logger LOG = LoggerFactory.getLogger(NettyClientSession.class);
	
	public NettyClientSession(ChannelHandlerContext ctx,int readBufferSize,int heardbeatInterval,int connType) {
		super(ctx,readBufferSize,heardbeatInterval, connType);
	}
	
	public boolean isServer() {
		return false;
	}
	
	
}
