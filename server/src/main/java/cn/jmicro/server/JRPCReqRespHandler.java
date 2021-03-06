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
package cn.jmicro.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.exception.TimeoutException;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.JMLogItemJRso;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.InterceptorManager;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequestJRso;
import cn.jmicro.api.net.RpcResponseJRso;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/**
 * 请求响应式RPC请求
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:50:36
 */
@Component(active=true,value="JRPCReqRespHandler",side=Constants.SIDE_PROVIDER)
public class JRPCReqRespHandler implements IMessageHandler{

	public static final Byte TYPE = Constants.MSG_TYPE_REQ_JRPC;
	
	private static final Class<?> TAG = JRPCReqRespHandler.class;
	
	private static final Logger logger = LoggerFactory.getLogger(JRPCReqRespHandler.class);
	
	@Inject
	private SecretManager secretMng;
	
	@Inject
	private InterceptorManager interceptorManger;
	
	@Cfg("/JRPCReqRespHandler/openDebug")
	private boolean openDebug=false;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject(required=true)
	private ServiceLoader serviceLoader;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject(required=true)
	private IRegistry registry = null;
	
	@Inject
	private AccountManager accountManager;
	
	@Inject
	private PermissionManager pm;
	
	@Inject
	private ServiceManager srvMng;
	
	@Inject(required=true)
	private ProcessInfoJRso pi;
	
	@Override
	public Byte type() {
		return TYPE;
	}

	@Override
	public boolean onMessage(ISession s, Message msg) {
		
		RpcRequestJRso req = null;
		RpcResponseJRso resp =  new RpcResponseJRso();
		boolean finish[] = new boolean[] {false};
		
	    try {
	    	
	    	//req1为内部类访问
	    	final RpcRequestJRso req1 = ICodecFactory.decode(this.codeFactory, msg.getPayload(),
					RpcRequestJRso.class, msg.getUpProtocol());
	    	
	    	req1.setMsg(msg);
	    	
	    	req1.setSm(JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null));
	    	
	    	ServiceMethodJRso sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
	    	
	    	config(req1,resp,msg.getLinkId(),sm);
	    	
	    	if(msg.isDebugMode()) {
	    		JMicroContext.get().appendCurUseTime("Server end decode req",true);
    		}
	    	
	    	if(msg.isMonitorable()) {
				MT.rpcEvent(MC.MT_SERVER_JRPC_GET_REQUEST,1);
				MT.rpcEvent(MC.MT_SERVER_JRPC_GET_REQUEST_READ,msg.getLen());
			}
	    	
	    	if(LG.isLoggable(MC.LOG_DEBUG)) {
	    		LG.log(MC.LOG_DEBUG, TAG, "Got request: " + msg.getMsgId()+",from insId: " + msg.getInsId());
	    	}
	    	
	    	req = req1;
			req.setSession(s);
			//req.setMsg(msg);
			
			//logger.info(req.getServiceName()+" debugMode: " + msg.isDebugMode()+", method: " + msg.getMethod());
			
			if(req1.getMethod().equals("info")) {
				logger.debug("");
			}
	    	
	    	if(LG.isLoggable(MC.LOG_DEBUG)) {
	    		LG.log(MC.LOG_DEBUG, TAG, LG.reqMessage("",req1));
	    	}
	    	
			resp.setMsg(msg);
			resp.setSuccess(true);
			//resp.setId(idGenerator.getLongId(IResponse.class));
			//msg.setInsId(pi.getId());
			
			//msg.setMsgId(idGenerator.getLongId(Message.class));
	    	
	    	ActInfoJRso ai = null;
			
			if(req1.getParams().containsKey(JMicroContext.LOGIN_KEY)) {
				String lk = (String)req1.getParams().get(JMicroContext.LOGIN_KEY);
				if(StringUtils.isNotEmpty(lk)) {
					ai = this.accountManager.getAccount(lk);
					if(ai == null && sm.isNeedLogin()) {
						ServerError se = new ServerError(MC.MT_INVALID_LOGIN_INFO,"JRPC check invalid login key!"+",insId: " + msg.getInsId());
						resp.setResult(se);
						resp.setSuccess(false);
						LG.log(MC.LOG_ERROR, TAG,se.toString());
						MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
						resp2Client(resp,s,msg,sm);
						return true;
					} else if(ai != null) {
						JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
						JMicroContext.get().setAccount(ai);
					}
				}
			}
			
			if(sm.getMaxPacketSize() > 0 && req.getPacketSize() > sm.getMaxPacketSize()) {
	    		ServerError se = new ServerError(MC.MT_PACKET_TOO_MAX,"Packet too max "+req.getPacketSize()+ " limit size: " + sm.getMaxPacketSize()+",insId: " + msg.getInsId()+","+sm.getKey().getMethod());
				resp.setResult(se);
				resp.setSuccess(false);
				LG.log(MC.LOG_ERROR, TAG,se.toString());
				MT.rpcEvent(MC.MT_PACKET_TOO_MAX,1);
				resp2Client(resp,s,msg,sm);
				return true;
			}
			
			ActInfoJRso sai = null;
			if(req1.getParams().containsKey(JMicroContext.LOGIN_KEY_SYS)) {
				String slk = (String)req1.getParams().get(JMicroContext.LOGIN_KEY_SYS);
				if(StringUtils.isNotEmpty(slk)) {
					sai = this.accountManager.getAccount(slk);
					if(sai == null && sm.getForType() == Constants.FOR_TYPE_SYS) {
						ServerError se = new ServerError(MC.MT_INVALID_LOGIN_INFO,"Invalid system login key: " + slk+",insId: " + msg.getInsId());
						resp.setResult(se);
						resp.setSuccess(false);
						LG.log(MC.LOG_ERROR, TAG,se.toString());
						MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
						resp2Client(resp,s,msg,sm);
						return true;
					} else if(sai != null) {
						JMicroContext.get().setString(JMicroContext.LOGIN_KEY_SYS, slk);
						JMicroContext.get().setSysAccount(sai);
					}
				}
			}
			
			if(sai == null && sm.getForType() == Constants.FOR_TYPE_SYS) {
				ServerError se = new ServerError(MC.MT_INVALID_LOGIN_INFO,"Need system login: " + sm.getKey().fullStringKey()+",insId: " + msg.getInsId());
				resp.setResult(se);
				resp.setSuccess(false);
				LG.log(MC.LOG_ERROR, TAG,se.toString());
				MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
				resp2Client(resp,s,msg,sm);
				return true;
			}
			
			ServiceItemJRso si = JMicroContext.get().getParam(Constants.SERVICE_ITEM_KEY, null);
			
			ServerError se = pm.permissionCheck(sm,si.getClientId());
			
			if(se != null) {
				resp.setResult(se);
				resp.setSuccess(false);
				resp2Client(resp,s,msg,sm);
				return true;
			}
			
			JMicroContext.get().getAccount();
			
			IPromise<Object> rr = interceptorManger.handleRequest(req);
			
			if(!msg.isNeedResponse()){
				//无需返回值
				//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
				if(rr != null) {
					rr.then((rst,fail,resultCxt)->{
	    				 if(fail != null) {
	    					    LG.log(MC.LOG_ERROR, TAG,fail.toString());
	    						MT.rpcEvent(MC.MT_SERVER_ERROR);
	    						logger.error("JRPCReq error: ",fail.toString());
	    				 } else {
	    					 MT.rpcEvent(MC.MT_SERVER_JRPC_RESPONSE_SUCCESS);
	    				 }
	    				 submitItem();
					});
				} else {
					String errMsg = "Got null promise: " + sm.getKey().fullStringKey()+",insId: " + msg.getInsId();
					LG.log(MC.LOG_ERROR, TAG,errMsg);
					MT.rpcEvent(MC.MT_SERVER_ERROR);
					logger.error("JRPCReq error: ",errMsg);
					submitItem();
				}
				return true;
			}

			final RpcResponseJRso r = resp;
			
			if(rr != null) {
				rr.success((rst,resultCxt)->{
					if(finish[0]) {
						logger.warn("ReqId: " + req1.getRequestId() +", linkId: " + msg.getLinkId() + " has synchronized response!");
						return;
					}
					r.setSuccess(true);
					r.setResult(rst);
					msg.setError(false);
					resp2Client(r,s,msg,sm);
					finish[0]=true;
				})
				.fail((code,errorMsg,resultCxt)->{
					if(finish[0]) {
						return;
					}
					ServerError se0 = new ServerError(code,errorMsg);
					r.setSuccess(false);
					r.setResult(se0);
					msg.setError(true);
					resp2Client(r,s,msg,sm);
					finish[0]=true;
				});
			} else {
				if(finish[0]) {
					return true;
				}
				ServerError se0 = new ServerError(MC.MT_SERVER_ERROR,"Got null result!"+",insId: " + msg.getInsId());
				r.setSuccess(false);
				r.setResult(se0);
				msg.setError(true);
				resp2Client(r,s,msg,sm);
				finish[0]=true;
			}
		} catch (Throwable e) {
			if(!finish[0]) {
				finish[0]=true;
				doException(req,resp,s,msg,e);
			}
		}
	    return true;
	}
	
	private void doException(RpcRequestJRso req,RpcResponseJRso resp0, ISession s,Message msg,Throwable e) {

		//返回错误
		LG.log(MC.LOG_ERROR, TAG.getName(),"JRPCReq error",e);
		
		MT.rpcEvent(MC.MT_SERVER_ERROR);
		logger.error("JRPCReq error: ",e);
		logger.error("doException msg: "+msg);
		
		if(msg.isNeedResponse()) {
			//返回错误
			ServerError error = null;
			if(e instanceof CommonException) {
				CommonException ce = (CommonException)e;
				error = new ServerError(ce.getKey(),e.getMessage());
			}else {
				error = new ServerError(0,e.getMessage());
			}
			
			//错误下行数据全用JSON
			msg.setPayload(ICodecFactory.encode(codeFactory,error,Message.PROTOCOL_JSON));
			msg.setType((byte)(msg.getType()+1));
			msg.setInsId(pi.getId());
			msg.setUpSsl(false);
			msg.setDownSsl(false);
			msg.setSign(false);
			msg.setSec(false);
			msg.setSaltData(null);
			msg.setError(true);
			
			msg.setTime(TimeUtils.getCurTime());
			
			StackTraceElement se = Thread.currentThread().getStackTrace()[1];
			logger.debug(se.getLineNumber() + "　doException msg: "+msg);
			
			s.write(msg);
		}
		
		submitItem();
		
		if(!((e instanceof RpcException) || (e instanceof TimeoutException))) {
			s.close(true);
		}
	}
	
	
	private void resp2Client(IResponse resp, ISession s,Message msg,ServiceMethodJRso sm) {
		if(!msg.isNeedResponse()){
			submitItem();
			return;
		}
		
		if(msg.isDebugMode()) {
    		JMicroContext.get().appendCurUseTime("Service Return",true);
		}
		
		if(msg.isError()) {
			msg.setPayload(ICodecFactory.encode(codeFactory,resp,Message.PROTOCOL_JSON));
		} else {
			msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getDownProtocol()));
		}
		
		//请求类型码比响应类型码大1，
		msg.setType((byte)(msg.getType()+1));
		
		if(!msg.isError()) {
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
		} else {
			LG.log(MC.LOG_ERROR, TAG, "Request failure end: " + msg.getMsgId()+",insId: " + msg.getInsId());
			MT.rpcEvent(MC.MT_SERVER_ERROR,1);
			//错误不需要做加密或签名
			msg.setUpSsl(false);
			msg.setDownSsl(false);
			msg.setSign(false);
			msg.setSec(false);
			msg.setSaltData(null);
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
			logger.error("",e);
		}
	}
	
	private void submitItem() {
		if(JMicroContext.get().isDebug()) {
			JMicroContext.get().appendCurUseTime("Async respTime",false);
			//JMicroContext.get().debugLog(0);
		}
		JMicroContext.get().submitMRpcItem();
	}
	
	private void config(RpcRequestJRso req,RpcResponseJRso resp,Long linkId,ServiceMethodJRso sm) {
		
		Object obj = serviceLoader.getService(sm.getKey().getUsk().getSnvHash());
		if(obj == null){
			LG.log(MC.LOG_ERROR,JMicroContext.class," service INSTANCE not found");
			MT.nonRpcEvent(Config.getInstanceName(), MC.MT_PLATFORM_LOG);
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found,srv: "+sm.getKey().getSnvHash());
		}
		
		JMicroContext cxt = JMicroContext.get();
		cxt.setString(JMicroContext.CLIENT_SERVICE, req.getServiceName());
		cxt.setString(JMicroContext.CLIENT_NAMESPACE, req.getNamespace());
		cxt.setString(JMicroContext.CLIENT_METHOD, req.getMethod());
		cxt.setString(JMicroContext.CLIENT_VERSION, req.getVersion());
		//context.setLong(JMicroContext.REQ_PARENT_ID, req.getRequestId());
		cxt.setParam(JMicroContext.REQ_ID, req.getRequestId());
		
		cxt.setParam(JMicroContext.CLIENT_ARGSTR, UniqueServiceMethodKeyJRso.paramsStr(req.getArgs()));
		cxt.putAllParams(req.getRequestParams());
		
		ServiceItemJRso si = registry.getOwnItem(sm.getKey().getUsk().getSnvHash());
		if(si == null){
			if(LG.isLoggable(MC.LOG_ERROR,req.getLogLevel())) {
				LG.log(MC.LOG_ERROR,JMicroContext.class," service ITEM not found");
				MT.nonRpcEvent(Config.getInstanceName(), MC.MT_SERVICE_ITEM_NOT_FOUND);
			}
			//SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
			throw new CommonException("Service not found impl："+req.getSvnHash()+", srv: " + req.getServiceName());
		}
		
		//ServiceMethod sm = si.getMethod(req.getMethod(), req.getArgs());
		cxt.setObject(Constants.SERVICE_ITEM_KEY, si);
		cxt.setObject(Constants.SERVICE_METHOD_KEY, sm);
		cxt.setObject(Constants.SERVICE_OBJ_KEY, obj);
		
		JMLogItemJRso mi = cxt.getMRpcLogItem();
		
		if( mi != null) {
			mi.setReqParentId(req.getReqParentId());
			mi.setReqId(req.getRequestId());
			mi.setReq(req);
			mi.setImplCls(si.getImpl());
			mi.setSmKey(sm.getKey());
			mi.setResp(resp);
			mi.setLinkId(linkId==null?0:linkId);
		}
		
	}
	
}
