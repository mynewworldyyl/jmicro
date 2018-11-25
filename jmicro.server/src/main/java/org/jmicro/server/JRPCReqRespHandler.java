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
package org.jmicro.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcRequest;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IInterceptor;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IRequestHandler;
import org.jmicro.api.server.IResponse;
import org.jmicro.api.server.IWriteCallback;
import org.jmicro.common.CommonException;
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
@Component(active=true,value="JRPCReqRespHandler",side=Constants.SIDE_PROVIDER)
public class JRPCReqRespHandler implements IMessageHandler{

	public static final short TYPE = Constants.MSG_TYPE_REQ_JRPC;
	
	private static final Class<?> TAG = JRPCReqRespHandler.class;
	
	static final Logger logger = LoggerFactory.getLogger(JRPCReqRespHandler.class);
	
	private volatile Map<String,IRequestHandler> handlers = new ConcurrentHashMap<>();
	
	@Cfg("/JRPCReqRespHandler/openDebug")
	private boolean openDebug;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Inject(required=true)
	private ServiceLoader serviceLoader;
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Cfg("/respBufferSize")
	private int respBufferSize = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	@Inject(required=true)
	private IRegistry registry = null;
	
	@Override
	public Short type() {
		return TYPE;
	}

	@Override
	public void onMessage(ISession s, Message msg) {
		   
		RpcRequest req = null;
		boolean needResp = true;
		IResponse resp = null;
	    try {

	    	final RpcRequest req1 = ICodecFactory.decode(this.codeFactory,msg.getPayload(),
					RpcRequest.class,msg.getProtocol());
			req = req1;
			req.setSession(s);
			req.setMsg(msg);
			
			JMicroContext.get().mergeParams(req.getRequestParams());
			
			ServiceItem si = registry.getServiceByImpl(req.getImpl());
			if(si == null){
				SF.doRequestLog(MonitorConstant.ERROR, msg.getLinkId(), TAG, req,null," service ITEM not found");
				SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,req,null);
				throw new CommonException("Service not found impl："+req.getImpl());
			}
			
			ServiceMethod sm = si.getMethod(req.getMethod(), req.getArgs());
			
			JMicroContext.get().setObject(Constants.SERVICE_ITEM_KEY, si);
			JMicroContext.get().setObject(Constants.SERVICE_METHOD_KEY, sm);
			
			JMicroContext.get().setString(JMicroContext.CLIENT_SERVICE, si.getServiceName());
			JMicroContext.get().setString(JMicroContext.CLIENT_NAMESPACE, si.getNamespace());
			JMicroContext.get().setString(JMicroContext.CLIENT_METHOD, req.getMethod());
			JMicroContext.get().setString(JMicroContext.CLIENT_VERSION, si.getVersion());
			
			Object obj = serviceLoader.getService(req.getImpl());
			if(obj == null){
				SF.doRequestLog(MonitorConstant.ERROR, msg.getLinkId(), TAG, req,null," service INSTANCE not found");
				SF.doSubmit(MonitorConstant.SERVER_REQ_SERVICE_NOT_FOUND,
						req,null);
				throw new CommonException("Service not found");
			}
			
			JMicroContext.get().setObject(Constants.SERVICE_OBJ_KEY, obj);
			
			needResp = sm.needResponse;
			SF.doSubmit(MonitorConstant.SERVER_REQ_BEGIN, req,resp,null);
			if(!needResp){
				handler(req);
				SF.doSubmit(MonitorConstant.SERVER_REQ_OK, req,resp,null);
				return;
			}

			msg.setReqId(req.getRequestId());
			//msg.setSessionId(req.getSession().getId());
			msg.setVersion(req.getMsg().getVersion());
				
			if(openDebug) {
				SF.doRequestLog(MonitorConstant.DEBUG,msg.getLinkId(), TAG, req,null,"got REQUEST");
			}
			
			JMicroContext jc = JMicroContext.get();
			
			if(req.isStream()){
				msg.setType(Constants.MSG_TYPE_ASYNC_RESP);
				SuspendableRunnable r = () -> {
					JMicroContext.get().mergeParams(jc);
					JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK_SERVICE, new IWriteCallback(){
						@Override
						public boolean send(Object message) {
							if(s.isClose()){
								s.close(true);
								return false;
							}
							RpcResponse resp = new RpcResponse(req1.getRequestId(),message);
							resp.setSuccess(true);
							//返回结果包
							msg.setId(idGenerator.getLongId(Message.class));
							msg.setPayload(codeFactory.getEncoder(msg.getProtocol()).encode(resp));
							msg.setType(Constants.MSG_TYPE_ASYNC_RESP);
							
							if(openDebug) {
								SF.doResponseLog(MonitorConstant.DEBUG, msg.getLinkId(), TAG, resp,null,"STREAM");
							}
							s.write(msg);
							return true;
						}
					});
					 handler(req1);
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
				resp = new RpcResponse(req.getRequestId(),null);
				resp.setSuccess(true);
				
				msg.setType(Constants.MSG_TYPE_RRESP_JRPC);
				msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getProtocol()));
				msg.setId(idGenerator.getLongId(Message.class));
				
				if(openDebug) {
					SF.doMessageLog(MonitorConstant.DEBUG, TAG, msg,null,"STREAM Confirm");
				}
				
				s.write(msg);
			
			} else {
				//同步响应
				resp = handler(req);
				if(resp == null){
					resp = new RpcResponse(req.getRequestId(),null);
					resp.setSuccess(true);
				}
				msg.setPayload(ICodecFactory.encode(codeFactory,resp,msg.getProtocol()));
				msg.setType(Constants.MSG_TYPE_RRESP_JRPC);
				msg.setId(idGenerator.getLongId(Message.class));
				
				if(openDebug) {
					SF.doResponseLog(MonitorConstant.DEBUG,msg.getLinkId(), TAG, resp,null);
				}
				
				s.write(msg);
			}
			SF.doSubmit(MonitorConstant.SERVER_REQ_OK, req,resp,null);
		} catch (Throwable e) {
			SF.doMessageLog(MonitorConstant.ERROR, TAG, msg,e);
			SF.doSubmit(MonitorConstant.SERVER_REQ_ERROR, req,resp,null);
			logger.error("reqHandler error: ",e);
			if(needResp && req != null ){
				resp = new RpcResponse(req.getRequestId(),new ServerError(0,e.getMessage()));
				resp.setSuccess(false);
			}
			s.close(true);
		}
	}

	private String reqMethodKey(RpcRequest req){
		StringBuffer sb = new StringBuffer(req.getServiceName());
		sb.append(req.getNamespace()).append(req.getVersion())
		.append(req.getMethod());
		
		if(req.getArgs() != null){
			Object[] args = req.getArgs();
			for(int i = 0; i < args.length; i++) {
				sb.append(args[i].toString());
			}
		}
		
		return sb.toString();
	}
	
	@Suspendable
    private IResponse handler(RpcRequest req) {
		
    	IRequestHandler handler = null;
    	
    	String key = reqMethodKey(req);
    	 Map<String,IRequestHandler> hs = this.handlers;
    	if(hs.containsKey(key)){
    		handler = hs.get(key);
    	} else {
    		String handlerKey = JMicroContext.get().getString(Constants.DEFAULT_HANDLER,
    				Constants.DEFAULT_HANDLER);
    		handler = JMicro.getObjectFactory().getByName(handlerKey);
    		if(handler == null){
    			handler = JMicro.getObjectFactory().getByName(Constants.DEFAULT_HANDLER);
    		}
    		if(handler == null){
    			throw new CommonException("JRPC Handler ["+handlerKey + " not found]");
    		}
    		hs.put(key, handler);
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
		
		Collection<IInterceptor> hs = JMicro.getObjectFactory().getByParent(IInterceptor.class);
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
