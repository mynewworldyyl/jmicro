
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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.gateway.GatewayConstant;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IServer;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.ServerErrorJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.gateway.lb.ComponentSelector;
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
	private RpcClassLoader rpcClassloader;
	
	@Inject
	private ICache cache;
	
	@Inject
	private SecretManager secretMng;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject(required=true)
	private ComponentSelector selector;
	
	@Inject
	private ComponentRouter cmpRouter;
	
	@Inject
	private ProcessInfoJRso pi;
	
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
	
	public void jready() {}
	
	@Override
	public Byte type() {
		return -1;
	}
	
	@Override
	public boolean onMessage(ISession session, Message msg) {
		/*if("index".equals(msg.getExtra(Message.EXTRA_KEY_METHOD))) {
			logger.info("test cache");
		}*/
		msg.setFromApiGateway(true);
		
		if(fromCache(session,msg)) {
			return true;
		}
	
		MessageRouteRow r = findTarget(session,msg);
		if(r == null) {
			return true;
		}
		
		ISession cs = sessionManager.getOrConnect(r.getInsName(), r.getIp(), r.getPort());
		if(cs == null) {
			respError(session,msg,ServerErrorJRso.SE_SERVICE_NOT_FOUND,"Connection refuse");
			return true;
		}
		
		if(msg.getRespType() == Message.MSG_TYPE_NO_RESP) {
			msg.setMsgId(idGenerator.getLongId(Message.class));
			cs.write(msg);
			//单向消息
			return true;
		}
		
		Map<Byte,Object> extraMap = msg.getExtraMap();
		if(extraMap != null && (msg.isUpSsl() || msg.isDownSsl())) {
			extraMap.remove(Message.EXTRA_KEY_SALT);
			extraMap.remove(Message.EXTRA_KEY_SEC);
			extraMap.remove(Message.EXTRA_KEY_SIGN);
		}
		
		linkMng.createLinkNode(session,msg);
		msg.setInsId(pi.getId());
		msg.setOuterMessage(false);
		if(msg.isUpSsl() || msg.isDownSsl()) {
			this.secretMng.signAndEncrypt(msg, r.getInsId());
		}
		
		cs.write(msg);
		return true;
	}

	private MessageRouteRow findTarget(ISession session, Message msg) {
		List<MessageRouteRow> mrrs = cmpRouter.doRoute(session, msg);
		if(mrrs == null || mrrs.isEmpty()) {
			respError(session,msg,ServerErrorJRso.SE_SERVICE_NOT_FOUND,"route target not found for msg:"+msg.toString());
			return null;
		}
		
		if(mrrs.size() > 1) {
			MessageRouteRow mrr = this.selector.select(mrrs,session,msg);
			if(mrr == null) {
				respError(session,msg,ServerErrorJRso.SE_SERVICE_NOT_FOUND,"balance target not found for msg:"+msg.toString());
				return null;
			}
			return mrr;
		} else {
			//单实例
			return mrrs.get(0);
		}
	}

	private void respError(ISession session, Message msg,int code,String errStr) {
		logger.error(errStr);
		msg.setType((byte)(msg.getType()+1));
		msg.setError(true);//响应错误响应消息
		
		ServerErrorJRso se = new ServerErrorJRso(code,errStr);
		//错误信息下行全用json,不管客户端所需下行协议
		msg.setDownProtocol(Message.PROTOCOL_JSON);
		msg.setPayload(ICodecFactory.encode(codecFactory, se, Message.PROTOCOL_JSON));
		
		//错误不需要做加密或签名
		msg.setDownSsl(false);
		msg.setUpSsl(false);

		msg.setInsId(pi.getId());
		session.write(msg);
		
	}
	
	private boolean fromCache(ISession s, Message msg) {
		ServiceMethodJRso sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(sm == null || sm.getCacheType() == Constants.CACHE_TYPE_NO) return false;
		
		String ck = IServer.cacheKey(rpcClassloader,msg,sm,codecFactory);
		if(ck == null) return false;
		
		Object val = cache.get(ck);
		if(val != null) {
			//从优先从缓存中取数据
			logger.info("response from cache: " + sm.getKey().fullStringKey());
			
			msg.setPayload(val);
			msg.setType((byte)(msg.getType()+1));

			MT.rpcEvent(MC.MT_SERVER_JRPC_RESPONSE_SUCCESS,1);
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, TAG,"Request end: " + msg.getMsgId()+",insId: " + msg.getInsId());
	    	}
			
			//响应消息,只有成功的消息才需要加密，失败消息不需要
			msg.setUpSsl(sm.isUpSsl());
			msg.setDownSsl(sm.isDownSsl());
			msg.setEncType(sm.isRsa());
			if(sm.isUpSsl() || sm.isDownSsl()) {
				secretMng.signAndEncrypt(msg,msg.getInsId());
			}
		
			msg.setInsId(pi.getId());
			//msg.setFromWeb(false);
			msg.setOuterMessage(false);
			
			try {
				msg.setTime(TimeUtils.getCurTime());
				s.write(msg);
				MT.rpcEvent(MC.MT_SERVER_JRPC_RESPONSE_WRITE, msg.getLen());
				if(msg.isDebugMode()) {
					JMicroContext.get().appendCurUseTime("Server finish write",true);
				}
				submitItem();
			} catch (Throwable e) {
				//到这里不能再抛出异常，否则可能会造成重复响应
				logger.error("resp2Client",e);
			}
			
			return true;
		}
		return false;
	}
	
	private void submitItem() {
		if(JMicroContext.get().isDebug()) {
			JMicroContext.get().appendCurUseTime("Async respTime",false);
			//JMicroContext.get().debugLog(0);
		}
		JMicroContext.get().submitMRpcItem();
	}
}
