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
package org.jmicro.api.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.exception.FusingException;
import org.jmicro.api.fusing.FuseManager;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItemHolderManager;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.server.RpcResponse;
import org.jmicro.api.server.ServerError;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:47
 */
@Component(value=Constants.DEFAULT_INVOCATION_HANDLER,lazy=false,side=Constants.SIDE_COMSUMER)
public class ServiceInvocationHandler implements InvocationHandler, IMessageHandler{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceInvocationHandler.class);
	
	private final Map<Long,IResponseHandler> waitForResponse = new ConcurrentHashMap<>();
	
	@Cfg("/respBufferSize")
	private int respBufferSize;
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Inject(required=true)
	private ISelector selector;
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject(required=false)
	private SubmitItemHolderManager monitor;
	
	@Inject
	private FuseManager fuseManager;
	
	@Inject
	private AsyncMessageHandler asyncMessageHandler;
	
	private Object target = new Object();
	
	public ServiceInvocationHandler(){
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		AbstractServiceProxy po = (AbstractServiceProxy)proxy;
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return target.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return target.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return target.equals(args[0]);
        }

        Class<?> clazz = method.getDeclaringClass();
        //String syncMethodName = methodName.substring(0, methodName.length() - Constants.ASYNC_SUFFIX.length());
        //Method syncMethod = clazz.getMethod(syncMethodName, method.getParameterTypes());
        
        try {
			return this.doRequest(method,args,clazz,po);
		} catch (FusingException e) {
			logger.error(e.getMessage(), e);
			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_SERVICE_FUSING,null, null, e);
			return fuseManager.onFusing(method, args, ((FusingException)e).getSis());
		}
    
	}

	private Object doRequest(Method method, Object[] args, Class<?> srvClazz,AbstractServiceProxy proxy) {
		//System.out.println(req.getServiceName());
		ServiceItem poItem = proxy.getItem();
		if(poItem == null){
			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_SERVICE_NOT_FOUND
					,null, null,method.getDeclaringClass().getName(),method.getName());
			throw new CommonException("cls["+method.getDeclaringClass().getName()+"] method ["+method.getName()+"]");
		}
		ServiceMethod poSm = poItem.getMethod(method.getName(), args);
		
		JMicroContext.get().configMonitor(poSm.getMonitorEnable()
				,poItem.getMonitorEnable());
		
		RpcRequest req = new RpcRequest();
        req.setMethod(method.getName());
        req.setServiceName(method.getDeclaringClass().getName());
        req.setArgs(args);
        req.setNamespace(poItem.getNamespace());
        req.setVersion(poItem.getVersion());
        req.setMonitorEnable(JMicroContext.get().isMonitor());
        
        ServerError se = null;
        		
        ServiceItem si = null;
        ServiceMethod sm = null;
        
        int retryCnt = -1;
        int interval = -1;
        int timeout = -1;
        boolean isFistLoop = true;
        
        do {
        	
        	String sn = ProxyObject.getTargetCls(srvClazz).getName();
			req.getNamespace();
			req.getVersion();
			//System.out.println(selector);
			
        	si = selector.getService(sn,method.getName(),args,req.getNamespace(),req.getVersion());
        	
        	if(si ==null) {
        		MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_SERVICE_NOT_FOUND, req, null);
    			throw new CommonException("Service [" + srvClazz.getName() + "] not found!");
    		}
        	
        	if(isFistLoop){
        		String t = ServiceMethod.methodParamsKey(args);
        		for(ServiceMethod m : si.getMethods()){
        			if(m.getMethodName().equals(method.getName()) 
        					&& m.getMethodParamTypes().equals(t)){
        				sm=m;
        				break;
        			}
        		}
        		if(sm == null){
        			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_METHOD_NOT_FOUND, req, null);
        			throw new CommonException("Service method ["+method.getName()+"] class [" + srvClazz.getName() + "] not found!");
        		}
        		retryCnt = sm.getRetryCnt();
        		if(retryCnt < 0){
        			retryCnt = si.getRetryCnt();
        		}
        		interval = sm.getRetryInterval();
    			if(interval < 0){
    				interval = si.getRetryInterval();
    			}
    			timeout = sm.getTimeout();
				if(timeout < 0){
					timeout = si.getTimeout();
				}
        	}
    		
    		//req.setImpl(si.getImpl());
    		req.setRequestId(idGenerator.getLongId(IRequest.class));  
    		req.setNamespace(si.getNamespace());
    		req.setVersion(si.getVersion());
    		
    		IClientSession session = this.sessionManager.connect(si.getHost(), si.getPort());
    		req.setSession(session);
    		
    		if(isFistLoop){
    			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_BEGIN, req, null);
    		}
    		
    		Message msg = new Message();
    		msg.setType(Message.MSG_TYPE_REQ_JRPC);
    		
    		msg.setId(this.idGenerator.getLongId(Message.class));
    		msg.setReqId(req.getRequestId());
    		msg.setSessionId(session.getId());
    		msg.setPayload(req.encode());
    		msg.setVersion(Message.VERSION_STR);
    		
    		//byte flag = sm.async ? Message.FLAG_ASYNC : 0;
    		boolean stream = !StringUtils.isEmpty(sm.streamCallback);
    		//boolean async = !StringUtils.isEmpty(sm.streamCallback);
    		
    		byte flag = stream ? Message.FLAG_STREAM : 0 ; 
    		//如果是流，一定需要响应
    		flag |=  sm.needResponse || stream ? Message.FLAG_NEED_RESPONSE:0;
    		
    		msg.setFlag(flag);
    		req.setMsg(msg);
    		
    		session.write(msg.encode());
    		
    		if(!sm.needResponse && !stream) {
    			//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
    			//this.sessionManager.write(msg, null,retryCnt);
    			return null;
    		}
    		
    		if(stream){
    			this.asyncMessageHandler.onRequest(session, req, sm);
    		}
    		
    		//保存返回结果
    		final Map<String,Object> result = new HashMap<>();
    		if(isFistLoop){
    			//超时重试不需要重复注册监听器
    			waitForResponse.put(req.getRequestId(), (message)->{
    				result.put("msg", message);
    				synchronized(req) {
        				req.notify();
        			}
    			});
    		}
    		
    		isFistLoop = false;
    		
    		synchronized(req) {
    			try {
    				if(timeout > 0){
    					req.wait(timeout);
    				}else {
    					req.wait();
    				}
    			} catch (InterruptedException e) {
    				logger.error("timeout: ",e);
    			}
    		}
    		
    		Message respMsg = (Message)result.get("msg");
    		result.clear();
    		RpcResponse resp = null;
    		if(respMsg != null){
    			resp = new RpcResponse(respBufferSize);
    			if(respMsg.getPayload() != null){
    				resp.decode(respMsg.getPayload());
    			}
    			resp.setMsg(respMsg);
    			req.setMsg(msg);
    		}
    		
    		if(resp != null && resp.isSuccess() && !(resp.getResult() instanceof ServerError)) {
    			if(!stream) {
    				//同步请求成功，直接返回
        			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_OK, req, resp);
        			req.setFinish(true);
        			waitForResponse.remove(req.getRequestId());
        			return resp.getResult();
    			} else {
    				//异步请求
    				//异步请求，收到一个确认包
					MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS, req, resp);
        			req.setFinish(true);
        			waitForResponse.remove(req.getRequestId());
        			return resp.getResult();
    			}
    		}
    		
    		//下面是此次请求失败，进入重试处理过程
    		
    		StringBuffer sb = new StringBuffer();
			if(se!= null){
				sb.append(se.toString());
			}
			sb.append(" host[").append(si.getHost()).append("] port [").append(si.getPort())
			.append("] service[").append(si.getServiceName())
			.append("] method [").append(sm.getMethodName())
			.append("] param [").append(sm.getMethodParamTypes());
    		
    		if(resp == null){
    			//肯定是超时了
    			if(retryCnt > 0){
    				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_TIMEOUT, req, null);
    				sb.append("] do retry: ").append(retryCnt);
    			} else {
    				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL, req, null);
    				sb.append("] time request request and stop retry: ").append(retryCnt);
    				throw new CommonException(sb.toString());
    			}
    			logger.error(sb.toString());
    			
    			if(interval > 0 && retryCnt > 0){
    				try {
    					//超时重试间隔
    					Thread.sleep(si.getRetryInterval());
    				} catch (InterruptedException e) {
    					logger.error("Sleep exceptoin ",e);
    				}
    				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_RETRY, req, resp);
    			}
    			
    		}else if(resp.getResult() instanceof ServerError){
				//服务器已经发生错误，是否需要重试
				 se = (ServerError)resp.getResult();
				 //logger.error("error code: "+se.getErrorCode()+" ,msg: "+se.getMsg());
				 req.setSuccess(resp.isSuccess());
				 MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_EXCEPTION_ERR, req, null);
				 throw new CommonException(sb.toString());
			} else if(!resp.isSuccess()){
				//服务器正常逻辑处理错误，不需要重试，直接失败
				 req.setSuccess(resp.isSuccess());
				 MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_BUSSINESS_ERR, req, resp);
			     throw new CommonException(sb.toString());
			}
    		//代码不应该走到这里，如果走到这里，说明系统还有问题
    		throw new CommonException(sb.toString());
    		
        }while(retryCnt-- > 0);
       
	}

	@Override
	public Short type() {
		return Message.MSG_TYPE_RRESP_JRPC;
	}

	@Override
	public void onMessage(ISession session,Message msg) {
		//receive response
		IResponseHandler handler = waitForResponse.get(msg.getReqId());
		if(handler!= null){
			handler.onResponse(msg);
		} else {
			logger.error("msdId:"+msg.getId()+",reqId:"+msg.getReqId()+",sId:"+msg.getSessionId()+" IGNORE");
		}
		
	}
	
	private static interface IResponseHandler{
		void onResponse(Message msg);
	}
	
	/*
	 private Object decodeResult(RpcResponse resp, IRequest req, Class<?> returnType) {
		if(returnType == Void.class) {
			return null;
		}
		if(returnType == String.class) {
			return new String(resp.getPayload(),Charset.forName("utf-8"));
		}
	   if(returnType.isPrimitive()) {
		    ByteBuffer bb = ByteBuffer.wrap(resp.getPayload());
            if (Boolean.TYPE == returnType)
               return bb.get()==1;
            if (Byte.TYPE == returnType)
                return bb.get();
            if (Character.TYPE == returnType)
                return bb.getChar();
            if (Double.TYPE == returnType)
                return bb.getDouble();
            if (Float.TYPE == returnType)
                return bb.getFloat();
            if (Integer.TYPE == returnType)
                return bb.getInt();
            if (Long.TYPE == returnType)
                return bb.getLong();
            if (Short.TYPE == returnType)
                return bb.getShort();
            throw new CommonException(returnType.getName() + " is unknown primitive type.");
        }else if(returnType.isArray()){
        	Class<?> comType = returnType.getComponentType();
        } else if(IDecodable.class.isAssignableFrom(returnType)) {
        	try {
        		IDecodable obj = (IDecodable)returnType.newInstance();
        		obj.decode(resp.getPayload());
				return obj;
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }else {
        	try {
        		Object obj = returnType.newInstance();
				return obj;
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		return null;
	}
	*/

}
