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
package cn.jmicro.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.Decoder;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.gateway.ApiRequestJRso;
import cn.jmicro.api.gateway.ApiResponseJRso;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.Constants;

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
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_API_CLASS_REQ;
	}

	@Override
	public boolean onMessage(ISession session, Message msg) {
		
		msg.setType((byte)(msg.getType()+1));
		
		ApiRequestJRso req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				ApiRequestJRso.class, msg.getUpProtocol());
		
		Short type = (Short)req.getArgs()[0];
		
		Class<?> cls = Decoder.getClass(type);
		
		ApiResponseJRso resp = new ApiResponseJRso();
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
		
		msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getUpProtocol()));
		
		session.write(msg);
		return true;
	}

}
