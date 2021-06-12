
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
package cn.jmicro.gateway.lb;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.gateway.GatewayConstant;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.common.Constants;
import cn.jmicro.gateway.link.LinkMng;
import cn.jmicro.gateway.router.ComponentRouter;

/**
 * 
 *
 * @author Yulei Ye
 */
@Component(side = Constants.SIDE_PROVIDER,value="apiGatewayHandler")
public class ApigatewayMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApigatewayMessageHandler.class);
	
	private static final Class<?> TAG = ApigatewayMessageHandler.class;
	
	@Inject
	private ComponentSelector balance;
	
	@Inject(required=true)
	private ComponentSelector selector;
	
	@Inject
	private ComponentRouter cmpRouter;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private ProcessInfo pi;
	
	@Inject
	private SecretManager secretMng;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private LinkMng linkMng;
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Cfg("/ApiRawRequestMessageHandler/openDebug")
	private boolean openDebug = false;
	
	@Cfg("/gateway/level")
	private int level = 1;
	
	@Cfg(GatewayConstant.API_MODEL)
	private boolean isPre = GatewayConstant.API_MODEL_PRE;
	
	public void ready() {
		
	}
	
	@Override
	public Byte type() {
		return -1;
	}
	
	@Override
	public void onMessage(ISession session, Message msg) {
		MessageRouteRow r = findTarget(session,msg);
		if(r == null) {
			return;
		}
		
		ISession cs = sessionManager.getOrConnect(Config.getInstanceName(), r.getIp(), r.getPort());
		if(cs == null) {
			respError(session,msg,ServerError.SE_SERVICE_NOT_FOUND,"Connection refuse");
			return;
		}
		
		msg.setOuterMessage(false);//从网关转到内网，消息改为内网消息
		
		cs.write(msg);
		
		if(msg.getRespType() == Message.MSG_TYPE_NO_RESP) {
			//单向消息
			return;
		}
		
		linkMng.createLinkNode(session,msg);
		
		return;
	}

	private MessageRouteRow findTarget(ISession session, Message msg) {
		List<MessageRouteRow> mrrs = cmpRouter.doRoute(session, msg);
		if(mrrs == null || mrrs.isEmpty()) {
			respError(session,msg,ServerError.SE_SERVICE_NOT_FOUND,"route target not found for msg:"+msg.toString());
			return null;
		}
		
		MessageRouteRow mrr = this.selector.select(mrrs,session,msg);
		if(mrr == null) {
			respError(session,msg,ServerError.SE_SERVICE_NOT_FOUND,"balance target not found for msg:"+msg.toString());
			return null;
		}
		
		return mrr;
	}

	private void respError(ISession session, Message msg,int code,String errStr) {
		
		msg.setType((byte)(msg.getType()+1));
		msg.setError(true);//响应错误响应消息
		
		ServerError se = new ServerError(code,errStr);
		//错误信息下行全用json,不管客户端所需下行协议
		msg.setDownProtocol(Message.PROTOCOL_JSON);
		msg.setPayload(ICodecFactory.encode(codecFactory, se, Message.PROTOCOL_JSON));
		
		//错误不需要做加密或签名
		msg.setDownSsl(false);
		msg.setUpSsl(false);

		msg.setInsId(pi.getId());
		session.write(msg);
		
	}
	
}
