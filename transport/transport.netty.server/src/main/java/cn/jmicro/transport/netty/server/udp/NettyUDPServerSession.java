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
package cn.jmicro.transport.netty.server.udp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.common.Constants;
import cn.jmicro.server.IServerSession;
import cn.jmicro.transport.netty.server.AbstractNettyServerSession;
import cn.jmicro.transport.netty.server.NettyServerSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月21日-下午9:15:12
 */
public class NettyUDPServerSession extends AbstractNettyServerSession implements IServerSession{

	static final Logger LOG = LoggerFactory.getLogger(NettyUDPServerSession.class);
	
	private final Map<InetSocketAddress,NettyServerSession> sessions = new HashMap<>();
	
	public NettyUDPServerSession(ChannelHandlerContext ctx,int readBufferSize,int hearbeatInterval) {
		super(ctx,readBufferSize,hearbeatInterval,Constants.TYPE_UDP);
	}

	public void receive(ByteBuffer msg, DatagramPacket dp, ChannelHandlerContext ctx) {
		NettyServerSession s = this.sessions.get(dp.sender());
		if(s == null) {
			s = new NettyServerSession(ctx, this.getReadBufferSize(), this.heardbeatInterval,Constants.TYPE_UDP);
			s.setLocalAddre(dp.recipient());
			s.setRemoteAddre(dp.sender());
			sessions.put(dp.sender(), s);
		}
		s.setReceiver(this.getReceiver());
		s.receive(msg);
	}
}
