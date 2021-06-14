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
package cn.jmicro.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.client.InvocationHandler;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.LogMonitorClient;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.JMLogItem;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.InterceptorManager;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:47
 */
@Component(value=Constants.DEFAULT_INVOCATION_HANDLER, lazy=false, side=Constants.SIDE_COMSUMER)
public class ServiceInvocationHandler implements InvocationHandler{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceInvocationHandler.class);
	
	private static final Class<?> TAG = ServiceInvocationHandler.class;
	
	@Cfg("/ServiceInvocationHandler/openDebug")
	private boolean openDebug;
	
	@Inject(required=true)
	private InterceptorManager intManager;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ProcessInfo pi;
	
	//private Set<Long> ids = new HashSet<>();
	
	public ServiceInvocationHandler(){}
	
	public <T> T invoke(Object proxy, String methodName, Object[] args){
		
		RpcRequest req = null;
		JMicroContext cxt = JMicroContext.get();
		ServiceMethod sm = null;
		try {
			
			ServiceItem si = cxt.getParam(Constants.SERVICE_ITEM_KEY, null);
			sm = cxt.getParam(Constants.SERVICE_METHOD_KEY, null);
			req = new RpcRequest();
			req.setSm(sm);
			req.setArgs(args);
			req.setRequestId(idGenerator.getLongId(IRequest.class));
			/*if(ids.contains(req.getRequestId())) {
				throw new CommonException("Reqeust ID repeated: " +methodName);
			} else {
				ids.add(req.getRequestId());
			}*/
			req.setTransport(Constants.TRANSPORT_NETTY);
			req.setSnvHash(si.getKey().getSnvHash());
			req.putObject(JMicroContext.LOGIN_KEY, cxt.getString(JMicroContext.LOGIN_KEY, null));
			
			if(JMicroContext.get().exists(TxConstants.TX_ID)) {
				req.putObject(TxConstants.TX_ID, JMicroContext.get().getLong(TxConstants.TX_ID, null));
				req.putObject(TxConstants.TX_SERVER_ID, JMicroContext.get().getInt(TxConstants.TX_SERVER_ID, null));
			}
			
			if(sm.getForType() == Constants.FOR_TYPE_SYS) {
				if(pi.isLogin()) {
					req.putObject(JMicroContext.LOGIN_KEY_SYS, pi.getAi().getLoginKey());
				} else {
					String desc = Config.getInstanceName() + " need system login for method: " + sm.getKey().toKey(false, false, false);
					LG.log(MC.LOG_ERROR, this.getClass(), desc);
					throw new CommonException(desc);
				}
			}
			
			req.setReqParentId(cxt.getLong(JMicroContext.REQ_PARENT_ID, -1L));
			
			if(!JMicroContext.existLinkId() && cxt.getByte(JMicroContext.SM_LOG_LEVEL, MC.LOG_NO) != MC.LOG_NO) {
				//新建一个RPC链路开始
				Long lid = JMicroContext.createLid();
				cxt.setParam(Constants.NEW_LINKID, true);
				//MT.rpcEvent(MC.MT_LINK_START);
				if(LG.isLoggable(MC.LOG_DEBUG)) {
					LG.log(MC.LOG_DEBUG,TAG,"Link start: " + lid+",reqId: " +req.getRequestId() );
				}
			} else {
				cxt.setParam(Constants.NEW_LINKID, false);
			}
			
			JMLogItem mi = cxt.getMRpcLogItem();
			if(mi != null) {
				mi.setReq(req);
				mi.setReqId(req.getRequestId());
				mi.setLinkId(JMicroContext.lid());
				mi.setReqParentId(req.getReqParentId());
				mi.setNl(cxt.getBoolean(Constants.NEW_LINKID, false));
			}
			
			/*MRpcStatisItem ms = cxt.getMRpcStatisItem();
			if(ms != null) {
				ms.setReq(req);
				ms.setReqId(req.getRequestId());
				ms.setLinkId(JMicroContext.lid());
				ms.setReqParentId(req.getReqParentId());
			}*/
			
			if(JMicroContext.get().isDebug()) {
				JMicroContext.get().getDebugLog()
				.append(methodName)
				.append(",reqID:").append(req.getRequestId())
				.append(",linkId:").append(JMicroContext.lid());
			}
			
			 return (T)this.intManager.handleRequest(req);
			
		} catch(Throwable ex) {
			//cxt.debugLog(0);
			cxt.submitMRpcItem();
			JMicroContext.clear();
			if(ex instanceof CommonException) {
				throw ex;
			} else {
				throw new CommonException("",ex);
			}
		}
	
	}
	
}
