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
package org.jmicro.api.pubsub;

import java.lang.reflect.Method;
import java.util.Map;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:09:47
 */
public class SubCallbackImpl implements ISubCallback{

	private final static Logger logger = LoggerFactory.getLogger(SubCallbackImpl.class);
	
	private UniqueServiceMethodKey mkey = null;
	
	private Object srvProxy = null;
	
	private Method m = null;
	
	private IRegistry reg;
	
	public SubCallbackImpl(UniqueServiceMethodKey mkey,Object srv, IRegistry reg){
		if(mkey == null) {
			throw new CommonException("SubCallback service method cannot be null");
		}
		
		if(srv == null) {
			throw new CommonException("SubCallback service cannot be null");
		}
		this.mkey = mkey;
		this.srvProxy = srv;
		this.reg = reg;
		setMt();
	}
	
	@Override
	public void onMessage(PSData item) {
		Object obj = null;
		try {
			Class[] ptype = m.getParameterTypes();
			if(ptype == null) {
				 obj = m.invoke(this.srvProxy, new Object[0]);
			}else if(ptype.length == 1 && ptype[0] == PSData.class) {
				 obj = m.invoke(this.srvProxy, item);
			} else {
				 Object[] args = (Object[])item.getData();
				 obj = m.invoke(this.srvProxy, args);
			}
		} catch (Throwable e) {
			throw new CommonException("Fail to send message to [" + mkey.toString()+"]", e);
		}
		
		boolean f = Message.is(item.getFlag(), PSData.FLAG_ASYNC_METHOD);
		if(f) {
			Map<String,Object> cxt = item.getContext();
		    	
			//AsyncInterceptor中设置以下参数
		    String sn = (String)cxt.get(Constants.SERVICE_NAME_KEY);
			String ns = (String)cxt.get(Constants.SERVICE_NAMESPACE_KEY);
			String ver = (String)cxt.get(Constants.SERVICE_VERSION_KEY);
		    
			String mn = (String)cxt.get(Constants.SERVICE_METHOD_KEY);
			
			Long linkId = (Long)cxt.get(JMicroContext.LINKER_ID);
			//Long reqId = (Long)cxt.get(JMicroContext.REQ_ID);
			
			if( sn == null ) {
				String msg = "Async callback service name is NULL:" + mkey.toString()+" [sn="+ns + ",ns="+ns + "ver="+ver +",mn="+ mn+",with args:"+ (obj==null?"":JsonUtils.getIns().toJson(obj)) +"]";
				SF.doBussinessLog(MonitorConstant.LOG_ERROR,SubCallbackImpl.class,null, msg);
				throw new CommonException(msg);
			}
			
			Object srv = reg.getServices(sn, ns, ver);
			
			if(srv != null) {
				try {
					Method m = null;
					if(obj == null) {
						m = srv.getClass().getMethod(mn);
					} else {
						m = srv.getClass().getMethod(mn,obj.getClass());
					}
					
					if(m != null) {
						//JMicroContext.get().setParam(key, val);
						JMicroContext.get().setLong(JMicroContext.LINKER_ID, linkId);
						//JMicroContext.get().setLong(JMicroContext.REQ_ID, reqId);
						m.invoke(srv, obj);
					}
				} catch (Throwable e) {
					String msg = "Fail to callback src service:" + mkey.toString()+" [sn="+ns + ",ns="+ns + "ver="+ver +",mn="+ mn+",with args:"+ (obj==null?"":JsonUtils.getIns().toJson(obj)) +"]";
					SF.doBussinessLog(MonitorConstant.LOG_ERROR,SubCallbackImpl.class,e, msg);
					throw new CommonException(msg,e);
				}
			} else {
				String msg = "Async callback service not found:" + mkey.toString()+" [sn="+ns + ",ns="+ns + "ver="+ver +",mn="+ mn+",with args:"+ (obj==null?"":JsonUtils.getIns().toJson(obj)) +"]";
				SF.doBussinessLog(MonitorConstant.LOG_ERROR,SubCallbackImpl.class,null, msg);
				throw new CommonException(msg);
			}
			
		}
		
	}

	private void setMt() {
		try {
			this.m = this.srvProxy.getClass().getMethod(mkey.getMethod(), PSData.class);
		} catch (NoSuchMethodException | SecurityException e) {
			try {
				Class<?>[] argsCls = UniqueServiceMethodKey.paramsClazzes(mkey.getParamsStr());
				this.m = this.srvProxy.getClass().getMethod(mkey.getMethod(), argsCls);
			} catch (NoSuchMethodException | SecurityException e1) {
				throw new CommonException("Get ["+mkey.toString() +"] fail",e);
			}
		}
	}

	@Override
	public String info() {
		return mkey.toKey(false, false, false);
	}

	@Override
	public String toString() {
		return info();
	}

	@Override
	public int hashCode() {
		return info().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}
	
}
