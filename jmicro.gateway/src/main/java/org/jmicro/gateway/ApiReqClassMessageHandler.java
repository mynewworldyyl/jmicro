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
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.idgenerator.IIdClient;
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
	
	@Inject("idClient")
	private IIdClient idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_API_CLASS_REQ;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		
		msg.setType((byte)(msg.getType()+1));
		
		ApiRequest req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				ApiRequest.class, msg.getProtocol());
		
		Short type = (Short)req.getArgs()[0];
		
		Class<?> cls = Decoder.getClass(type);
		
		ApiResponse resp = new ApiResponse();
		resp.setReqId(req.getReqId());
		resp.setMsg(msg);
		resp.setId(type.longValue());
		
		if(cls != null) {
			resp.setResult(cls.getName());
			resp.setSuccess(true);
		}else {
			resp.setSuccess(false);
			resp.setResult("");
		}
		
		msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
		
		session.write(msg);
	}

}
