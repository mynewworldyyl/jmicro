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

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.exception.FusingException;
import org.jmicro.api.fusing.FuseManager;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SubmitItemHolderManager;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.server.ServerError;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:47
 */
@Component(value=Constants.DEFAULT_INVOCATION_HANDLER,lazy=false)
public class ServiceInvocationHandler implements InvocationHandler{
	
	private final static Logger logger = LoggerFactory.getLogger(ServiceInvocationHandler.class);
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Inject(required=true)
	private ISelector selector;
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject(required=false)
	private SubmitItemHolderManager monitor;
	
	@Cfg(value="/monitorClientEnable",required=false)
	private boolean monitorClientEnable = true;
	
	@Inject
	private FuseManager fuseManager;
	
	
	private Object target = new Object();
	
	public ServiceInvocationHandler(){
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
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
        	AbstractServiceProxy po = (AbstractServiceProxy)proxy;
			return this.doRequest(method,args,clazz,po);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			if(e instanceof FusingException){
				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_SERVICE_FUSING,null, null, e);
				return fuseManager.onFusing(method, args, ((FusingException)e).getSis());
			}else {
				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_RESP_ERR,null, null, clazz,method,args,e);
				throw e;	
			}
		}
    
	}

	private Object doRequest(Method method, Object[] args, Class<?> srvClazz,AbstractServiceProxy proxy) {
		//System.out.println(req.getServiceName());
		ServiceItem poItem = proxy.getItem();
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
    		isFistLoop=false;
    		final Map<String,Object> result = new HashMap<>();
    		this.sessionManager.write(req, (resp,reqq,err)->{
    			//Object rst = decodeResult(resp,req,method.getReturnType());
    			result.put("result", resp.getResult());
    			result.put("resp", resp);
    			synchronized(req) {
    				req.notify();
    			}
    		},retryCnt);//如果同一个连接失败，可以在底层使用同一个连接直接重试，避免“抖动”
    		
    		
    		synchronized(req) {
    			try {
    				if(timeout > 0){
    					req.wait(timeout);
    				}else {
    					req.wait();
    				}
    			} catch (InterruptedException e) {
    				logger.error("",e);
    			}
    		}
    		
    		if(req.isFinish()){
    			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_HAVE_FINISH, req, null);
    			throw new CommonException("got repeat result cls["+si.getServiceName()+",method["+method.getName());
    		}

    		Object obj = result.get("result");
    		IResponse resp = (IResponse)result.get("resp");
    		result.clear();
    		if(obj instanceof ServerError || !req.isSuccess()){
    			se = (ServerError)obj;
    			StringBuffer sb = new StringBuffer();
    			if(se!= null){
    				sb.append(se.toString());
    			}
    			sb.append(" host[").append(si.getHost()).append("] port [").append(si.getPort())
    			.append("] service[").append(si.getServiceName());
    			if(retryCnt > 0){
    				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_TIMEOUT, req, resp);
    				sb.append("] do retry: ").append(retryCnt);
    			}else {
    				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_FAIL, req, resp);
    				sb.append("] fail request and stop retry: ").append(retryCnt);
    			}
    			logger.error(sb.toString());
    			
    			if(interval > 0 && retryCnt > 0){
    				try {
						Thread.sleep(si.getRetryInterval());
					} catch (InterruptedException e) {
						logger.error("Sleep exceptoin ",e);
					}
    				MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_REQ_RETRY, req, resp);
    			}
    			//throw new CommonException(se.toString());
    		} else {
    			MonitorConstant.doSubmit(monitor,MonitorConstant.CLIENT_RESP_OK, req, resp);
    			req.setFinish(true);
    			return obj;
    		}
        }while(retryCnt-- > 0);
       
        if(se != null){
        	 throw new CommonException(se.toString());
        }
        throw new CommonException("");
       
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
