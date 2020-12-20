package cn.jmicro.api.net;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.objectfactory.ProxyObject;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

@Component(value="interceptorManager")
public class InterceptorManager {

	private static final Class<?> TAG = InterceptorManager.class;
	
	static final Logger logger = LoggerFactory.getLogger(InterceptorManager.class);
	
	private volatile Map<String,IRequestHandler> providerHandlers = new ConcurrentHashMap<>();
	
	private volatile Map<String,IRequestHandler> consumerHandlers = new ConcurrentHashMap<>();
	
    public IResponse handleRequest(RpcRequest req) {
		
    	boolean callSideProvider = JMicroContext.isCallSideService();
    	
    	Map<String,IRequestHandler> hs = this.consumerHandlers;
    	String handlerName = Constants.DEFAULT_CLIENT_HANDLER;
    	if(callSideProvider) {
    		hs = this.providerHandlers;
    		handlerName = Constants.DEFAULT_HANDLER;
    	}
    	
    	IRequestHandler handler = null;
    	
    	String key = reqMethodKey(req);
    	
    	if( hs.containsKey(key) ){
    		handler = hs.get(key);
    	} else {
    		String handlerKey = JMicroContext.get().getString(handlerName,handlerName);
    		handler = JMicro.getObjectFactory().getByName(handlerKey);
    		if( handler == null ){
    			handler = JMicro.getObjectFactory().getByName(handlerName);
    		}
    		if( handler == null ){
    			throw new CommonException("Interceptor ["+handlerKey + " not found]");
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

    	boolean callSideProvider = JMicroContext.isCallSideService();
    	String firstIntName=null,lastIntName=null;
    	if(callSideProvider) {
    		firstIntName = Constants.FIRST_INTERCEPTOR;
    		lastIntName = Constants.LAST_INTERCEPTOR;
    	} else {
    		firstIntName = Constants.FIRST_CLIENT_INTERCEPTOR;
    		lastIntName = Constants.LAST_CLIENT_INTERCEPTOR;
    	}
    	
		IInterceptor[] handlers = null;
		IInterceptor firstHandler = null;
		IInterceptor lastHandler = null;
		
		Collection<IInterceptor> hs = getInterceptors();
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
				if(firstIntName.equals(ha.value()) ||
						firstIntName.equals(ca.value())){
					if(firstHandler != null){
						StringBuffer sb = new StringBuffer();
						sb.append("More than one ").append(firstIntName).append(" found");
						sb.append(firstHandler.getClass().getName()).append(", ").append(ha.getClass().getName());
						throw new CommonException(sb.toString());
					}
					firstHandler = h;
				}else if(lastIntName.equals(ha.value()) || lastIntName.equals(ca.value())){
					if(lastHandler != null){
						StringBuffer sb = new StringBuffer();
						sb.append("More than one [").append(lastIntName).append("] found");
						sb.append(lastHandler.getClass().getName()).append(", ").append(ha.getClass().getName());
						throw new CommonException(sb.toString());
					}
					lastHandler = h;
				} else {
					handlers[index++] = h;
				}
			}
		}
		if(firstHandler == null){
			StringBuffer sb = new StringBuffer("Interceptor not found [")
					.append(firstIntName)
					.append("]");
			throw new CommonException(sb.toString());
		}
		handlers[0] = firstHandler;
		
		if(lastHandler == null){
			StringBuffer sb = new StringBuffer("Interceptor not found [")
					.append(lastIntName)
					.append("]");
			throw new CommonException(sb.toString());
		}
		handlers[handlers.length-1] = lastHandler;
		
		if(handlers.length > 3) {
			Arrays.sort(handlers, 1, handlers.length-2, (o1,o2)->{
				
				if(o1 == o2) {
					return 0;
				}
				
				if(o1 == null && o2 != null) {
					return -1;
				}
				
				if(o1 != null && o2 == null) {
					return 1;
				}
				
				Interceptor ha1 = o1.getClass().getAnnotation(Interceptor.class);
				Interceptor ha2= o2.getClass().getAnnotation(Interceptor.class);
				return ha1.order() > ha2.order() ? 1:(ha1.order() == ha2.order() ? 0:-1);
			});
		}
		
		
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
    
    private Collection<IInterceptor> getInterceptors() {
    	boolean callSideProvider = JMicroContext.isCallSideService();
    	Collection<IInterceptor> coll = JMicro.getObjectFactory().getByParent(IInterceptor.class);
    	for(Iterator<IInterceptor> ite = coll.iterator();ite.hasNext();){
			IInterceptor h = ite.next();
			Class<?> cls = ProxyObject.getTargetCls(h.getClass());
			if(!cls.isAnnotationPresent(Component.class)
					|| !cls.isAnnotationPresent(Interceptor.class)) {
				continue;
			}
			Component ca = cls.getAnnotation(Component.class);
			if(callSideProvider && Constants.SIDE_COMSUMER.equals(ca.side())) {
				ite.remove();
			}else if(!callSideProvider && Constants.SIDE_PROVIDER.equals(ca.side())) {
				ite.remove();
			}
    	}
		return coll;
	}

	private String reqMethodKey(RpcRequest req){
		StringBuffer sb = new StringBuffer(req.getServiceName());
		sb.append(req.getNamespace()).append(req.getVersion())
		.append(req.getMethod());
		
		if(req.getArgs() != null && req.getArgs().length > 0){
			sb.append(UniqueServiceMethodKey.paramsStr(req.getArgs()));
		}
		
		return sb.toString();
	}
}
