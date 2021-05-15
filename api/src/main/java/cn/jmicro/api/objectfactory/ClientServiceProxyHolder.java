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
package cn.jmicro.api.objectfactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.client.InvocationHandler;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.codegenerator.AsyncClientUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:00:01
 */
public class ClientServiceProxyHolder implements IServiceListener{

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private ServiceItem item = null;
	
	private String ns;
	
	private String v;
	
	private String sn;
	
	private IObjectFactory of;
	
	private Map<String,AsyncConfig> acs = null;
	
	private boolean direct = false;
	
	private int insId = -1;
	
	private String blPkgName;
	
	private InvocationHandler targetHandler = null;
	
	public IObjectFactory getOf() {
		return of;
	}

	public void setOf(IObjectFactory of) {
		this.of = of;
	}
	
	public int getInsId() {
		return insId;
	}
	
	@Override
	public void serviceChanged(int type, ServiceItem item) {

		if(!this.serviceKey().equals(item.serviceKey())){
			throw new CommonException("Service listener give error service oriItem:"+ 
					this.getItem()==null ? serviceKey():this.getItem().getKey().toKey(true, true, true)+" newItem:"+item.getKey().toKey(true, true, true));
		}
		
		if(!checkPackagePermission(item,this.blPkgName)) {
			logger.warn("No permission to use service [" + item.getKey().getServiceName()+"] from " + this.blPkgName);
			return;
		}
		
		if(IServiceListener.ADD == type){
			logger.info("Service Item Add: cls:{}, key:{}",this.getClass().getName(),this.serviceKey());
			this.setItem(item);
		}else if(IServiceListener.REMOVE == type) {
			logger.info("Service Item Remove cls:{}, key:{}",this.getClass().getName(),this.serviceKey());
			this.setItem(null);
		}else if(IServiceListener.DATA_CHANGE == type) {
			logger.info("Service Item Change: cls:{}, key:{}",this.getClass().getName(),this.serviceKey());
			this.setItem(item);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T invoke(String originMethod,final Object rpcContext, Object... args) {

		Map<String,Object> curCxt = backupAndResetContext();
		
		JMicroContext cxt = JMicroContext.get();
		
		ServiceItem dsi = cxt.getParam(Constants.DIRECT_SERVICE_ITEM, null);
		
		ServiceItem si = this.item;
		
		if(dsi != null) {
			si = dsi;
		}
		
		boolean isAsync = originMethod.endsWith(AsyncClientProxy.ASYNC_METHOD_SUBFIX);
		
		String methodName = originMethod;
		
		if(isAsync) {
			methodName = AsyncClientUtils.genSyncMethodName(originMethod);
		}
		
		if(si == null) {
			if (!isUsable()) {
				String msg = "Service Item is NULL when call method [" + methodName + "] with params ["
						+ UniqueServiceMethodKey.paramsStr(args) + "] proxy [" + this.getClass().getName() + "]";
				logger.error(msg);
				restoreContext(curCxt);
				throw new CommonException(msg);
			} else {
				si = this.item;
			}
		}
		
		ServiceMethod sm = si.getMethod(methodName, args);
		if (sm == null) {
			restoreContext(curCxt);
			throw new CommonException("cls[" + si.getImpl() + "] method [" + methodName + "] method not found");
		}
		
		if(!isAsync) {
			String returnTypeName = sm.getKey().getReturnParam();
			if(!Utils.isEmpty(returnTypeName)) {
				isAsync = sm.getKey().getReturnParam().startsWith("Lcn/jmicro/api/async/IPromise");
			}
		}
		
		JMicroContext.configComsumer(sm, si);
		
		if(Constants.LICENSE_TYPE_FREE != sm.getFeeType()) {
			ActInfo ai = cxt.getAccount();
			if(ai == null) {
				String msg = "License need login: " + si.getKey().toKey(false, false, false);
				LG.log(MC.LOG_INFO, this.getClass(), msg);
				restoreContext(curCxt);
				throw new CommonException(msg);
			}
			
			if(Constants.LICENSE_TYPE_CLIENT == sm.getFeeType() 
					&& si.getClientId() != ai.getId()) {
				boolean f = false;
				if(sm.getAuthClients() != null && sm.getAuthClients().length > 0) {
					for(int t : sm.getAuthClients()) {
						if(t == ai.getId()) {
							f = true;
							break;
						}
					}
				}
				
				if(!f) {
					String msg = "Not authronize account ["+ai.getActName()+"] for " + si.getKey().toKey(false, false, false);
					LG.log(MC.LOG_WARN, this.getClass(), msg);
					restoreContext(curCxt);
					throw new CommonException(msg);
				}
			} else if(Constants.LICENSE_TYPE_PRIVATE == sm.getFeeType() && si.getClientId() != ai.getId()) {
				String msg = "Private service ["+ai.getActName()+"] for " + si.getKey().toKey(false, false, false);
				LG.log(MC.LOG_WARN, this.getClass(), msg);
				restoreContext(curCxt);
				throw new CommonException(msg);
			}
		}
		
		boolean sdirect = false;
		
		try {
			
			InvocationHandler h = targetHandler;
			if (h == null) {
				synchronized (this) {
					h = targetHandler;
					if (h == null) {
						String handler = si.getHandler();
						if (StringUtils.isEmpty(handler)) {
							handler = Constants.DEFAULT_INVOCATION_HANDLER;
						}
						h = of.getByName(handler);
						if (h == null) {
							String msg = "Handler not found when call method [" + methodName + "] with params ["
									+ UniqueServiceMethodKey.paramsStr(args) + "] proxy [" + this.getClass().getName()
									+ "]";
							logger.error(msg);
							restoreContext(curCxt);
							throw new CommonException(msg);
						}
						targetHandler = h;
					}
				}
			}

			cxt.setString(cn.jmicro.api.JMicroContext.CLIENT_NAMESPACE, this.getNamespace());
			cxt.setString(cn.jmicro.api.JMicroContext.CLIENT_SERVICE, this.getNamespace());
			cxt.setString(cn.jmicro.api.JMicroContext.CLIENT_VERSION, this.getNamespace());
			cxt.setString(cn.jmicro.api.JMicroContext.CLIENT_METHOD, this.getNamespace());

			cxt.setParam(Constants.CLIENT_REF_METHOD, methodName);
			cxt.setObject(Constants.PROXY, this);

			//context.setObject(JMicroContext.MONITOR, JMicro.getObjectFactory().get(MonitorManager.class));
			cxt.setParam(Constants.SERVICE_METHOD_KEY, sm);
			cxt.setParam(Constants.SERVICE_ITEM_KEY, si);
			cxt.setParam(JMicroContext.LOCAL_HOST, Config.getExportSocketHost());
			
			if(JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM, null) == null) {
				if(isDirect()){
					sdirect = true;
					JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, this.item);
				}
			}

			IPromise<Object> p = h.invoke(this, methodName, args);
			
			restoreContext(curCxt);
			
			if(isAsync) {
				p.then((rst,fail,cxt0)->{
					if(rpcContext != null) {
						p.setContext(rpcContext);
					}
					restoreContext(curCxt);
				});
				return (T)p;
			} else {
				//同步返回结果
				if(p.isSuccess()) {
					//p.getResult()直到服务端数据返回或超时失败才后才会返回
					return getVal(p.getResult(),sm.getKey().getReturnParamClass());
				} else {
					throw new CommonException(p.getFailCode(),p.getFailMsg());
				}
			}
		} finally {
			if (sdirect) {
				cxt.removeParam(Constants.DIRECT_SERVICE_ITEM);
			}
		}
	}
	
	private void restoreContext(Map<String, Object> curCxt) {
		JMicroContext.clear();
		JMicroContext.get().putAllParams(curCxt);
	}

	private <T> T getVal(Object retVal,Class<?> rt) {
		T to = null;
		if(retVal != null) {
			to = (T) retVal;
		} else {
			if (rt == null || rt == Void.class || rt == Void.TYPE) {
				return null;
			} else if (rt == Byte.TYPE) {
				to = (T) new Byte((byte) 0);
			} else if (rt == Short.TYPE) {
				to = (T) new Short((byte) 0);
			} else if (rt == Integer.TYPE) {
				to = (T) new Integer((byte) 0);
			} else if (rt == Long.TYPE) {
				to = (T) new Long((byte) 0);
			} else if (rt == Float.TYPE) {
				to = (T) new Float((byte) 0);
			} else if (rt == Double.TYPE) {
				to = (T) new Double((byte) 0);
			} else if (rt == Boolean.TYPE) {
				to = (T) Boolean.FALSE;
			}
		}
		return to;
	}
	
	public boolean isUsable() {
		if(this.item != null) {
			return true;
		}

		synchronized(this) {
			if(this.item == null) {
				ServiceItem si = getItemFromRegistry();
				if(si != null) {
					this.setItem(si);
				}
			}
		}
		return this.item != null;
	
	}
	
	protected ServiceItem getItemFromRegistry() {
		IRegistry r = of.get(IRegistry.class);
		Set<ServiceItem> sis = r.getServices(this.getServiceName(), this.getNamespace(), this.getVersion());
		if(sis != null && !sis.isEmpty()) {
			return sis.iterator().next();
		}
		return null;
	}

	public String getNamespace() {
		return this.ns;
	}
	
	public String getVersion() {
		return this.v;
	}
	
	public String getServiceName() {
		return this.sn;
	}
	
	public Map<String,Object>  backupAndResetContext(){
		//System.out.println("backupAndSetContext");
		
		JMicroContext cxt = JMicroContext.get();
		
		boolean breakFlag = cxt.getBoolean(Constants.BREAKER_TEST_CONTEXT, false);
		
		//Reference ref = cxt.getParam(Constants.REF_ANNO, null);
		
		ServiceItem dsi = cxt.getParam(Constants.DIRECT_SERVICE_ITEM, null);
		
		AsyncConfig async = cxt.getParam(Constants.ASYNC_CONFIG, null);
		
		ActInfo ai = cxt.getAccount();
		
		String loginKey = cxt.getParam(JMicroContext.LOGIN_KEY, null);
		
		Long linkId = cxt.getParam(JMicroContext.LINKER_ID, null);
		
		Long txid = cxt.getParam(TxConstants.TX_ID, null);
		
		Integer txInsId = cxt.getParam(TxConstants.TX_SERVER_ID, null);
		
		Long preRequestId = cxt.getParam(JMicroContext.REQ_ID, null);
		
		boolean isProvider = JMicroContext.isContainCallSide() ? JMicroContext.isCallSideService() : false;
		
		//backup the rpc context from here
		//cxt.backupAndClear();
		
		Map<String,Object> curCxt = new HashMap<>();
		cxt.getAllParams(curCxt);
		if(dsi != null) {
			curCxt.remove(Constants.DIRECT_SERVICE_ITEM);
			curCxt.remove(Constants.ASYNC_CONFIG);
		}
		
		JMicroContext.clear();
		
		cxt = JMicroContext.get();
		
		//Start a new RPC context from here
		//false表示不是provider端
		JMicroContext.setCallSide(false);
		if(breakFlag) {
			cxt.setBoolean(Constants.BREAKER_TEST_CONTEXT, true);
		}
		
		/*if(ref != null) {
			cxt.setParam(Constants.REF_ANNO, ref);
		}*/
		
		if(dsi != null) {
			cxt.setParam(Constants.DIRECT_SERVICE_ITEM, dsi);
		}
		
		if(async != null) {
			cxt.setParam(Constants.ASYNC_CONFIG, async);
		}
		
		if(loginKey != null) {
			cxt.setParam(JMicroContext.LOGIN_KEY, loginKey);
		}
		
		if(isProvider && linkId != null && linkId > 0) {
			cxt.setParam(JMicroContext.LINKER_ID, linkId);
		}
		
		if(txid != null && txid > 0) {
			cxt.setParam(TxConstants.TX_ID, txid);
			cxt.setParam(TxConstants.TX_SERVER_ID, txInsId);
		}
		
		if(isProvider && preRequestId != null && preRequestId > 0) {
			//pre request ID is the parent ID of this request
			cxt.setParam(JMicroContext.REQ_PARENT_ID, preRequestId);
		}
		
		if(ai != null) {
			cxt.setAccount(ai);
		}
		
		return curCxt;
	}
	
	//public abstract  boolean enable();
	//public abstract void enable(boolean enable);
	
	public  void setItem(ServiceItem si){
		if(si != null) {
			if(StringUtils.isEmpty(this.ns)) {
				this.ns = si.getKey().getNamespace();
			}
			
			if(StringUtils.isEmpty(this.v)) {
				this.ns = si.getKey().getVersion();
			}
			
			if(StringUtils.isEmpty(this.sn)) {
				this.ns = si.getKey().getServiceName();
			}
			this.insId =si.getInsId();
		}
	
		this.item = si;
	}
	
	public  ServiceItem getItem(){
		return this.item;
	}
	
	public String serviceKey(){
		return UniqueServiceKey.serviceName(this.getServiceName(), this.getNamespace(), this.getVersion());
	}
	
	@Override
	public int hashCode() {
		return this.serviceKey().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ClientServiceProxyHolder)){
			return false;
		}
		ClientServiceProxyHolder o = (ClientServiceProxyHolder)obj;
		return this.serviceKey().equals(o.serviceKey());
	}
	
	public void setAsyncConfig(AsyncConfig[] acs) {
		if(acs != null && acs.length > 0) {
			this.acs = new HashMap<>();
			for(AsyncConfig a : acs) {
				this.acs.put(a.getForMethod(), a);
			}
		}
	}
	
	public static boolean checkPackagePermission(ServiceItem si,String pn) {
		boolean f = true;
		if(si.getLimit2Packages().size() > 0) {
			f = false;
			if(pn != null) {
				for(String p : si.getLimit2Packages()) {
					if(pn.startsWith(p)) {
						f = true;
						break;
					}
				}
			}
		}
		return f;
	}
	
	
	
	public AsyncConfig getAcs(String mkey) {
		if(acs == null) {
			return null;
		}
		return this.acs.get(mkey);
	}

	public boolean isDirect() {
		return direct;
	}

	public void setDirect(boolean direct) {
		this.direct = direct;
	}
	
	public void setNamespace(String ns) {
		this.ns = ns;
	}
	
	public void setVersion(String v) {
		this.v = v;
	}
	
	public void setServiceName(String sn) {
		this.sn = sn;
	}

	public String getBlPkgName() {
		return blPkgName;
	}

	public void setBlPkgName(String blPkgName) {
		this.blPkgName = blPkgName;
	}
	
}
