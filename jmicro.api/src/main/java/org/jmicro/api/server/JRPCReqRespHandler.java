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
package org.jmicro.api.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItemHolderManager;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.api.servicemanager.ServiceLoader;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableRunnable;

/**
 * 请求响应式RPC请求
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:50:36
 */
@Component(active=true,value="JRPCReqRespHandler")
public class JRPCReqRespHandler implements IMessageHandler{

	public static final short TYPE = Message.MSG_TYPE_REQ_JRPC;
	
	static final Logger logger = LoggerFactory.getLogger(JRPCReqRespHandler.class);
	
	private volatile Map<String,IRequestHandler> handlers = new ConcurrentHashMap<>();
	
	@Inject(required=false)
	private SubmitItemHolderManager monitor;
	
	@Inject(required=true)
	private ServiceLoader serviceLoader;
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Cfg("/respBufferSize")
	private int respBufferSize;
	
	@Override
	public short type() {
		return TYPE;
	}

	@Override
	public void onMessge(IServerSession s, Message msg) {
		    s.setId(msg.getSessionId());
	        JMicroContext cxt = JMicroContext.get();
			cxt.setParam(JMicroContext.SESSION_KEY, s);
			
			RpcRequest req = new RpcRequest();
			req.decode(msg.getPayload());
			req.setSession(s);
			req.setMsg(msg);
			
			s.putParam(Constants.MONITOR_ENABLE_KEY,req.isMonitorEnable());
			cxt.configMonitor(req.isMonitorEnable()?1:0, 0);
			
			IResponse resp = null;
			boolean needResp = ServiceLoader.isNeedResponse(this.serviceLoader, req);
			MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_REQ_BEGIN, req,resp);
			try {
				
				if(!needResp){
					handler(req);
					MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_REQ_OK, req,resp);
					return;
				}

				msg.setReqId(req.getRequestId());
				msg.setSessionId(req.getSession().getId());
				msg.setVersion(req.getMsg().getVersion());
				
				if(req.isStream()){
					msg.setType(Message.MSG_TYPE_ASYNC_RESP);
					
					SuspendableRunnable r = () ->{
						JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK, new IWriteCallback(){
							@Override
							public void send(Object message) {
								RpcResponse resp = new RpcResponse(req.getRequestId(),message,respBufferSize);
								resp.setSuccess(true);
								//返回结果包
								msg.setId(idGenerator.getLongId(Message.class));
								msg.setPayload(resp.encode());
								msg.setType(Message.MSG_TYPE_ASYNC_RESP);
								if(s.isClose()){
									throw new CommonException("Session is closed while writing data");
								}
								s.write(msg.encode());
							}
						});
						 handler(req);
					};
					//异步响应
					new Fiber<Void>(r).start();
					
					/*Runnable run = ()->{
						JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK, new IWriteCallback(){
							@Override
							public void send(Object message) {
								RpcResponse resp = new RpcResponse(req.getRequestId(),message,respBufferSize);
								resp.setSuccess(true);
								//返回结果包
								msg.setId(idGenerator.getLongId(Message.class));
								msg.setPayload(resp.encode());
								msg.setType(Message.MSG_TYPE_ASYNC_RESP);
								if(s.isClose()){
									throw new CommonException("Session is closed while writing data");
								}
								s.write(msg.encode());
							}
						});
						 handler(req);
					};
					new Thread(run).start();*/
					
					
					//直接返回一个确认包
					resp = new RpcResponse(req.getRequestId(),null,respBufferSize);
					resp.setSuccess(true);
					
					msg.setType(Message.MSG_TYPE_RRESP_JRPC);
					msg.setPayload(resp.encode());
					msg.setId(idGenerator.getLongId(Message.class));
					s.write(msg.encode());
				
				} else {
					//同步响应
					resp = handler(req);
					if(resp == null){
						resp = new RpcResponse(req.getRequestId(),null,respBufferSize);
						resp.setSuccess(true);
					}
					msg.setPayload(resp.encode());
					msg.setType(Message.MSG_TYPE_RRESP_JRPC);
					msg.setId(idGenerator.getLongId(Message.class));
					s.write(msg.encode());
				}
				MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_REQ_OK, req,resp);
			} catch (Throwable e) {
				MonitorConstant.doSubmit(monitor,MonitorConstant.SERVER_REQ_ERROR, req,resp);
				logger.error("reqHandler error: ",e);
				if(needResp && req != null ){
					resp = new RpcResponse(req.getRequestId(),new ServerError(0,e.getMessage()),respBufferSize);
					resp.setSuccess(false);
				}
				s.close(true);
			}
	}

	private String reqMethodKey(RpcRequest req){
		StringBuffer sb = new StringBuffer(req.getServiceName());
		sb.append(req.getNamespace()).append(req.getVersion());
		
		Object[] args = req.getArgs();
		for(int i = 0; i < args.length; i++) {
			sb.append(args[i].toString());
		}
		return sb.toString();
	}
	
	@Suspendable
    private IResponse handler(RpcRequest req) {
		
    	IRequestHandler handler = null;
    	
    	String key = reqMethodKey(req);
    	if(handlers.containsKey(key)){
    		handler = this.handlers.get(key);
    	} else {
    		String handlerKey = JMicroContext.get().getString(Constants.DEFAULT_HANDLER,
    				Constants.DEFAULT_HANDLER);
    		handler = ComponentManager.getCommponentManager(IRequestHandler.class)
    				.getComponent(handlerKey);
    		if(handler == null){
    			handler = ComponentManager.getCommponentManager(IRequestHandler.class)
        				.getComponent(Constants.DEFAULT_HANDLER);
    		}
    		if(handler == null){
    			throw new CommonException("JRPC Handler ["+handlerKey + " not found]");
    		}
    		this.handlers.put(key, handler);
    	}
		
		IRequestHandler firstHandler = buildHanderChain(handler);
		if(firstHandler == null) {
			throw new CommonException("Handler is not found");
		}
		return firstHandler.onRequest(req);
	}
    
    private IRequestHandler buildHanderChain(IRequestHandler handler) {

		IInterceptor[] handlers = null;
		IInterceptor firstHandler = null;
		IInterceptor lastHandler = null;
		
		Collection<IInterceptor> hs = ComponentManager.getCommponentManager(IInterceptor.class)
				.getComponents();
		if(hs == null || hs.size() < 2) {
			throw new CommonException("IInterceptor is NULL");
		}
		
		int index = 1;
		handlers = new IInterceptor[hs.size()];
		
		for(Iterator<IInterceptor> ite = hs.iterator();ite.hasNext();){
			IInterceptor h = ite.next();
			Class<?> cls = ProxyObject.getTargetCls(h.getClass());
			if(cls.isAnnotationPresent(Interceptor.class)) {
				Interceptor ha = cls.getAnnotation(Interceptor.class);
				Component ca = cls.getAnnotation(Component.class);
				if(Constants.FIRST_INTERCEPTOR.equals(ha.value()) ||
						Constants.FIRST_INTERCEPTOR.equals(ca.value())){
					if(firstHandler != null){
						StringBuffer sb = new StringBuffer();
						sb.append("More than one ").append(Constants.FIRST_INTERCEPTOR).append(" found");
						sb.append(firstHandler.getClass().getName()).append(", ").append(ha.getClass().getName());
						throw new CommonException(sb.toString());
					}
					firstHandler = h;
				}else if(Constants.LAST_INTERCEPTOR.equals(ha.value()) ||
						Constants.LAST_INTERCEPTOR.equals(ca.value())){
					if(lastHandler != null){
						StringBuffer sb = new StringBuffer();
						sb.append("More than one [").append(Constants.LAST_INTERCEPTOR).append("] found");
						sb.append(lastHandler.getClass().getName()).append(", ").append(ha.getClass().getName());
						throw new CommonException(sb.toString());
					}
					lastHandler = h;
				} else {
					handlers[index] = h;
				}
			}else {
				handlers[index] = h;
			}
		}
		if(firstHandler == null){
			StringBuffer sb = new StringBuffer("Interceptor not found [")
					.append(Constants.FIRST_INTERCEPTOR)
					.append("]");
			throw new CommonException(sb.toString());
		}
		handlers[0] = firstHandler;
		
		if(lastHandler == null){
			StringBuffer sb = new StringBuffer("Interceptor not found [")
					.append(Constants.LAST_INTERCEPTOR)
					.append("]");
			throw new CommonException(sb.toString());
		}
		handlers[handlers.length-1] = lastHandler;
		
		IRequestHandler last = handler;
		for(int i = handlers.length-1; i >= 0; i--) {
			IInterceptor in = handlers[i];
			IRequestHandler next = last;
			last = new IRequestHandler(){
				@Override
				public IResponse onRequest(IRequest request) {
					return in.intercept(next, request);
				}
			};
		}
		return last;
	}

	private Boolean monitorEnable(IServerSession session) {
    	 Boolean v = (Boolean)session.getParam(Constants.MONITOR_ENABLE_KEY);
		 return v == null ? JMicroContext.get().isMonitor():v;
    }
}
