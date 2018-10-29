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
package org.jmicro.api.idgenerator;

import org.jmicro.api.ClassScannerUtils;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月25日-下午10:54:58
 */
@Component(side=Constants.SIDE_PROVIDER)
public class IdRequestMessageHandler implements IMessageHandler{

	@Inject
	private IIdGenerator idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Override
	public Short type() {
		return Constants.MSG_TYPE_ID_REQ;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		
		IdRequest req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				IdRequest.class, msg.getProtocol());
		
		Class<?> cls = ClassScannerUtils.getIns().getClassByName(req.getClazz());
		
		Object result = null;
		
		switch(req.getType()){
			case IdRequest.BYte:
				result = this.idGenerator.getIntId(cls,req.getNum());
				break;
			case IdRequest.SHort:
				result = this.idGenerator.getIntId(cls,req.getNum());
				break;
			case IdRequest.INteger:
				result = this.idGenerator.getIntId(cls,req.getNum());
				break;
			case IdRequest.LOng:
				result = this.idGenerator.getLongId(cls,req.getNum());
				break;
			case IdRequest.STring:
				result = this.idGenerator.getStringId(cls,req.getNum());
				break;
		}
		
		msg.setType((short)(msg.getType()+1));
		msg.setPayload(ICodecFactory.encode(codecFactory, result, msg.getProtocol()));
		session.write(msg);
	}

}
