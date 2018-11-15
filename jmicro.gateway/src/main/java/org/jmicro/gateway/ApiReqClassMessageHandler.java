
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
package org.jmicro.gateway;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.Decoder;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.TransforClassManager;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:19:51
 *
 */
@Component(side = Constants.SIDE_PROVIDER)
public class ApiReqClassMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApiReqClassMessageHandler.class);
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Override
	public Short type() {
		return Constants.MSG_TYPE_API_CLASS_REQ;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		
		msg.setType((short)(msg.getType()+1));
		
		Short type = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				Short.class, msg.getProtocol());
		Class<?> cls = Decoder.getClass(type);
		if(cls != null) {
			msg.setPayload(ICodecFactory.encode(codecFactory, cls.getName(), msg.getProtocol()));
		}else {
			msg.setPayload(ICodecFactory.encode(codecFactory, Void.class.getName(), msg.getProtocol()));
		}
		
		session.write(msg);
	}

}
