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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;
import com.alibaba.fastjson.JSONException;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.codec.TypeUtils;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.AbstractHandler;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:12
 */
@Component(value=Constants.DEFAULT_HANDLER,lazy=false,active=true,side=Constants.SIDE_PROVIDER)
public class RpcRequestHandler extends AbstractHandler implements IRequestHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);
	
	@Inject
	private RpcClassLoader rpcClassloader;
	
	@Override
	public IPromise<Object> onRequest(IRequest request) {
		Object obj = JMicroContext.get().getObject(Constants.SERVICE_OBJ_KEY, null);
		Promise<Object> p = null;
		try {
			
			Class<?>[] pst = getMethodParamsType(request.getArgs());
			
			Method m = getServiceMethod(obj,pst,request);
			if(m.getName().equals("updateFun")) {
				logger.debug("updateFun");
			}
			boolean f = m.isAccessible();
			if(!f) {
				//通过Lambda动态注册的服务方法,，会报方法调用异常，应该是内部生成的类是非public导致，在此暂时做此处理
				if(obj.getClass().getName().contains("$$Lambda$")) {
					m.setAccessible(true);
				}
			}
			
			/*if(request.getMethod().equals("updateResource")) {
				logger.debug(request.getImpl());
			}*/
			
			Object[] args = request.getArgs();
			if(request.getProtocol() == Message.PROTOCOL_JSON 
				|| request.getProtocol() == Message.PROTOCOL_EXTRA) {
				args = parseJsonArgs(obj,m,request);
			}
			
			Object result = m.invoke(obj, args);
			
			/*if(JMicroContext.get().getAccount() != null)
				logger.info(Thread.currentThread().getName()+",after Act: "+JMicroContext.get().getAccount().getActName());
			*/
			
			if(!f) {
				//正常的非public方法调用不到跑到这里，所以可以直接设置即可
				m.setAccessible(f);
			}
			
			if(result != null && result instanceof IPromise) {
				return (IPromise<Object>)result;
			}
			
			p = new Promise<Object>();
			p.setResult(result);
			p.done();
			
		} catch (Throwable e) {
			logger.error("onRequest:",e);
			LG.log(MC.LOG_ERROR, RpcRequestHandler.class, "Invoke service error ", e);
			
			p = new Promise<>();
			RespJRso<Object> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			Throwable srcex = e.getCause();
			if(srcex instanceof CommonException) {
				CommonException ce = (CommonException)srcex;
				//p.setFail(ce.getKey(), ce.getMessage());
				r.setMsg(ce.getMessage());
			}else {
				//p.setFail(MC.MT_SERVER_ERROR, e.getMessage());
				r.setMsg(e.getMessage());
			}
			
			p.setResult(r);
			p.done();
		}
		return p;
	}
	
	private Object[] parseJsonArgs(Object obj, Method m, IRequest req){

		if(req.getArgs() == null || req.getArgs().length == 0) return new Object[0];
		
		Type[] clses = m.getGenericParameterTypes();
		
		for(Type c : clses) {
			if(TypeUtils.isMap(c) || TypeUtils.isCollection(c)) {
				//ServiceMethodJRso sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
				try {
					Class<?> intCls = this.rpcClassloader.loadClass(req.getServiceName());
					Method imethod = intCls.getMethod(req.getMethod(), m.getParameterTypes());
					clses  = imethod.getGenericParameterTypes();
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				break;
			}
		}

		Object[] jsonArgs = req.getArgs();
		
		Object[] args = new Object[clses.length];
		int i = 0;
		int j = 0;
		for(; i < clses.length; i++){
			Object arg = jsonArgs[j++];
			//logger.info(arg.toString());
			Type t = clses[i];
			try {
				Object a = JsonUtils.getIns().fromJson(JsonUtils.getIns().toJson(arg),t);
				args[i] = a;
			} catch (JSONException e) {
				logger.error("Type: "+t.toString());
				logger.error("Value: "+JsonUtils.getIns().toJson(arg));
				throw e;
			}
		}
	
		return args;
		
	}
	
	public static Method getServiceMethod(Object obj ,Class<?>[] pst, IRequest req){
		
		try {
			Method m = obj.getClass().getMethod(req.getMethod(), pst);
			return m;
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
			//降级到只匹配方法名称，意味着方法不能重载
			Method[] ms = obj.getClass().getMethods();
			for(Method m : ms) {
				if(m.getName().equals(req.getMethod())) {
					return m;
				}
			}
			throw new RpcException(req,e,MC.MT_SERVICE_METHOD_NOT_FOUND);
		}
	}
	
	public static Class<?>[]  getMethodParamsType(Object[] args){
		if(args == null || args.length==0){
			return new Class<?>[0];
		}
		Class<?>[] parameterTypes = new Class[args.length];
		for(int i = 0; i < args.length; i++) {
			if(args[i] != null) {
				Class<?> pt = args[i].getClass();
				if(Set.class.isAssignableFrom(pt)) {
					parameterTypes[i]=Set.class;
				} else if(Map.class.isAssignableFrom(pt)) {
					parameterTypes[i]=Map.class;
				} else if(List.class.isAssignableFrom(pt)) {
					parameterTypes[i]=List.class;
				}  else if(Collection.class.isAssignableFrom(pt)) {
					parameterTypes[i]=Collection.class;
				} else {
					parameterTypes[i] = ReflectUtils.getPrimitiveClazz(pt);
				}
			}else {
				parameterTypes[i] = Object.class;
			}
			
		}
		return parameterTypes;
	}
	
	public static Method getInterfaceMethod(IRequest req){
		try {
			Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(req.getServiceName());
			Class<?>[] pst = getMethodParamsType(req.getArgs());
			Method m = cls.getMethod(req.getMethod(),pst);
			return m;
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
			logger.error("getInterfaceMethod",e);
		}
		return null;
	}

}
