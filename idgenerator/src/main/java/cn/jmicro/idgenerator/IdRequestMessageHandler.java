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
package cn.jmicro.idgenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.idgenerator.IdRequest;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月25日-下午10:54:58
 */
@Component(side=Constants.SIDE_PROVIDER,value="idRequestMessageHandler")
public class IdRequestMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(IdRequestMessageHandler.class);
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_ID_REQ;
	}

	@Override
	public boolean onMessage(ISession session, Message msg) {
		
		IdRequest req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				IdRequest.class, msg.getUpProtocol());
		
		String cls = req.getClazz();
		
		Object result = null;
		
		synchronized(cls) {
			//同种ID同时只能有一个请求进入
			switch(req.getType()){
			case IdRequest.BYte:
				result = this.idGenerator.getIntIds(cls,req.getNum());
				break;
			case IdRequest.SHort:
				result = this.idGenerator.getIntIds(cls,req.getNum());
				break;
			case IdRequest.INteger:
				result = this.idGenerator.getIntIds(cls,req.getNum());
				break;
			case IdRequest.LOng:
				result = this.idGenerator.getLongIds(cls,req.getNum());
				break;
			case IdRequest.STring:
				result = this.idGenerator.getStringIds(cls,req.getNum());
				break;
			}
		}
		
		msg.setType((byte)(msg.getType()+1));
		msg.setPayload(ICodecFactory.encode(codecFactory, result, msg.getUpProtocol()));
		session.write(msg);
		return true;
	}

}
