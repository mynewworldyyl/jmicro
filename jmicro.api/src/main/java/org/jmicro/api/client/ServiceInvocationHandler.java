package org.jmicro.api.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.IDecodable;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.server.RpcResponse;
import org.jmicro.common.Constants;

@Component(value=Constants.DEFAULT_INVOCATION_HANDLER,lazy=false)
public class ServiceInvocationHandler implements InvocationHandler{
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Inject(required=true)
	private ISelector selector;
	
	@Inject
	private IIdGenerator idGenerator;
	
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
        
        return this.doRequest(method,args,clazz);
    
	}

	private Object doRequest(Method method, Object[] args, Class<?> srvClazz) {
		//System.out.println(req.getServiceName());
		RpcRequest req = new RpcRequest();
        req.setMethod(method.getName());
        req.setServiceName(method.getDeclaringClass().getName());
        req.setArgs(args);
        req.setRequestId(idGenerator.getLongId(IRequest.class));
	        
		ServiceItem si = selector.getService(ProxyObject.getTargetCls(srvClazz).getName());
		if(si ==null) {
			throw new CommonException("Service [" + srvClazz.getName() + "] not found!");
		}
		
		//req.setImpl(si.getImpl());
		req.setNamespace(si.getNamespace());
		req.setVersion(si.getVersion());
		
		IClientSession session = this.sessionManager.connect(si.getHost(), si.getPort());
		req.setSession(session);
		
		Map<String,Object> result = new HashMap<>();
		this.sessionManager.write(req, (resp,reqq)->{
			//Object rst = decodeResult(resp,req,method.getReturnType());
			result.put("result", resp.getResult());
			synchronized(req) {
				req.notify();;			
			}
		});
		
		
		synchronized(req) {
			try {
				req.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result.get("result");
	}
	
	/*private Object decodeResult(RpcResponse resp, IRequest req, Class<?> returnType) {
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
	}*/

}
