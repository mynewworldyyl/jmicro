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
package cn.jmicro.api;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.http.HttpRequest;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.JMLogItemJRso;
import cn.jmicro.api.monitor.JMStatisItemJRso;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.Linker;
import cn.jmicro.api.monitor.LogMonitorClient;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:55:28
 */
public class JMicroContext  {

	static final Logger logger = LoggerFactory.getLogger(JMicroContext.class);
	
	//标识此RPC是否HTTP请求
	public static final String HTTP_REQ = "Http-Req";
	//HTTP请求头参数
	public static final String HTTP_PARAM_KEY = "__http_params";
	
	//value should be true or false, for provider or consumer side
	public static final String CALL_SIDE_PROVIDER = "_callSideProvider";
	public static final String SESSION_DATA_PREFIX = "__SESS_D:";
	
	public static final String LOCAL_HOST = "_host";
	public static final String LOCAL_PORT = "_port";
	
	public static final String User_Agent = "User-Agent";
	public static final String REMOTE_HOST = "_remoteHost";
	public static final String REMOTE_PORT = "_remotePort";
	public static final String REMOTE_INS_ID = "_remoteInsId";
	
	public static final String ORIGIN_MSG = "_originMsg";
	
	public static final String LOGIN_ACT = "_loginAccount";
	public static final String LOGIN_ACT_SYS = "_loginAccountSys";
	
	public static final String LOGIN_KEY = Message.EXTRA_KEY_LOGIN_KEY + ""; //Constants.TOKEN_KEY;
	public static final String LOGIN_KEY_SYS = Message.EXTRA_KEY_LOGIN_SYS + ""; //"loginKeySys";
	
	public static final String SM_LOG_LEVEL = "__smLogLevel";
	
	public static final String CACHE_LOGIN_KEY = "__ActLoginKey_";
	
	public static final String LINKER_ID = "_linkerId";
	public static final String REQ_PARENT_ID = "_reqParentId";
	//控制RPC方法在每个服务中输出日志，区加于往监控服务器上传日志
	public static final String IS_DEBUG = "_isDebug";
	public static final String IS_MONITORENABLE = "_monitorEnable";
	public static final String REQ_ID = "_reqId";
	public static final String MSG_ID = "_msgId";
	public static final String REQ_INS = "_reqIns";
	
	//public static final String MONITOR = "monitor";
	
	public static final String CLIENT_SERVICE = "clientService";
	public static final String CLIENT_NAMESPACE = "clientNamespace";
	public static final String CLIENT_VERSION = "clientVersion";
	public static final String CLIENT_METHOD = "clientMehtod";
	public static final String CLIENT_ARGSTR= "argStr";
	
	public static final String MRPC_LOG_ITEM = "_mrpc_log_item";
	//public static final String CLIENT_UP_TIME = "_client_up_time";
	public static final String DEBUG_LOG_BASE_TIME = "_server_got_time";
	public static final String DEBUG_LOG = "_debug_loggner";
	
	public static final String MRPC_STATIS_ITEM = "_mrpc_statis_item";
	
	public static final String SESSION_KEY="_sessionKey";
	
	public static final String SESSION_DATA = "_sessionData";
	
	public static final String LOGIN_TOKEN = "_LOGIN_TOKEN";
	public static final String SLOGIN_TOKEN = "_SLOGIN_TOKEN";
	
	private static final ThreadLocal<JMicroContext> cxt = new ThreadLocal<JMicroContext>();
	
	//private final Stack<Map<String,Object>> ctxes = new Stack<>();
	
	private static LogMonitorClient lo;
	
	private static StatisMonitorClient mo;
	
	private static ICache ch;
	
	private static boolean isReady = false;
	
	//当前上下文
	protected final Map<String,Object> curCxt = new HashMap<String,Object>();
	
	private JMicroContext() {}
	
	public static void jready0(LogMonitorClient loo,StatisMonitorClient moo,ICache cache) {
		if(isReady) {
			return;
		}
		isReady = true;
		lo = loo;
		mo = moo;
		ch = cache;
	}
	
	public JMLogItemJRso getMRpcLogItem() {
		//使用者需要调用isMonitor()或isDebug()判断是否可用状态
		return this.getParam(MRPC_LOG_ITEM, null);
	}
	
	public JMStatisItemJRso getMRpcStatisItem() {
		return this.getParam(MRPC_STATIS_ITEM, null);
	}
	
	public void submitMRpcItem() {
		if(!isReady) {
			logger.warn("Monitor server client not ready yet!");
			return;
		}
		
		debugLog();
		
		JMLogItemJRso item = getMRpcLogItem();
		if(item != null ) {
			JMicroContext.get().removeParam(MRPC_LOG_ITEM);
			
			/*if(item.getItems().size() == 0 && this.getBoolean(Constants.NEW_LINKID,false)) {
				OneLog lo = item.addOneItem(deftLogLevel, JMicroContext.class.getName(),"nl",TimeUtils.getCurTime());
				//lo.setType(MC.INVALID_VAL);
			}*/
			
			if(StringUtils.isEmpty(item.getActName())) {
				ActInfoJRso ai = this.getAccount();
				if(ai != null) {
					item.setActClientId(ai.getClientId());
					item.setSysClientId(Config.getClientId());
					ai.setActName(ai.getActName());
				}
			}
			LG.setCommon(item);
			item.setCostTime(System.currentTimeMillis() - item.getCreateTime());
			lo.readySubmit(item);
		}
	
		
		if(this.isMonitorable()) {
			JMStatisItemJRso sItem = getMRpcStatisItem();
			if(sItem != null && sItem.getTypeStatis() != null 
					&& !sItem.getTypeStatis().isEmpty()) {
				JMicroContext.get().removeParam(MRPC_STATIS_ITEM);
				if(StringUtils.isEmpty(item.getActName())) {
					ActInfoJRso ai = this.getAccount();
					if(ai != null) {
						item.setActClientId(ai.getClientId());
						item.setSysClientId(Config.getClientId());
						ai.setActName(ai.getActName());
					}
				}
				MT.setCommon(sItem);
				sItem.setCostTime(TimeUtils.getCurTime() - sItem.getCreateTime());
				mo.readySubmit(sItem);
			}
		}
	}
	
	public static void remove(){
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		JMicroContext c = cxt.get();
		if(c != null) {
			cxt.remove();
		}
	}
	
	public static boolean existRpcContext() {
		return cxt.get() != null && (cxt.get().exists(JMicroContext.REQ_ID) 
				|| cxt.get().exists(JMicroContext.LINKER_ID));
	}
	
	public static JMicroContext get(){
		JMicroContext c = cxt.get();
		if(c != null) {
			return c;
		}
		synchronized(JMicroContext.class) {
			 c = cxt.get();
			if(c == null) {
				c = new JMicroContext();
				cxt.set(c);
			}
		}
		return c;
	}
	
	public static void clear(){
		cxt.remove();
	}
	
	public static void setCallSide(Boolean flag){
		get().setBoolean(JMicroContext.CALL_SIDE_PROVIDER, flag);
	}
	
	public static boolean isContainCallSide(){
		return get().exists(JMicroContext.CALL_SIDE_PROVIDER);
	}
	
	public static boolean isCallSideService(){
		if(isContainCallSide()) {
			return get().getBoolean(JMicroContext.CALL_SIDE_PROVIDER, false);
		}
		throw new CommonException("Non RPC Context!");
	}
	
	public static boolean isCallSideClient(){
		return !isCallSideService();
	}
	
	public static void configProvider(ISession s,Message msg,ServiceMethodJRso sm) {
		
		JMicroContext cx = get();
		
		cx.curCxt.clear();
		
		setCallSide(true);
		
		cx.setParam(JMicroContext.SESSION_KEY, s);
		
		cx.setParam(JMicroContext.REMOTE_HOST, s.remoteHost());
		cx.setParam(JMicroContext.REMOTE_PORT, s.remotePort());
		cx.setParam(JMicroContext.REMOTE_INS_ID, msg.getInsId());
		cx.setParam(JMicroContext.ORIGIN_MSG, msg);
		
		cx.setParam(JMicroContext.LOCAL_HOST, s.localHost());
		cx.setParam(JMicroContext.LOCAL_PORT, s.localPort()+"");
		
		String lk = msg.getExtra(Message.EXTRA_KEY_LOGIN_KEY);
		if(!Utils.isEmpty(lk)) {
			cx.setParam(JMicroContext.LOGIN_TOKEN, lk);
		}
		
		String slk = msg.getExtra(Message.EXTRA_KEY_LOGIN_SYS);
		if(!Utils.isEmpty(slk)) {
			cx.setParam(JMicroContext.SLOGIN_TOKEN, slk);
		}
		
		Integer cid = msg.getExtra(Message.EXTRA_KEY_CLIENT_ID);
		if(cid != null) {
			cx.setParam(Message.EXTRA_KEY_CLIENT_ID+"", cid);
		} else {
			ActInfoJRso sai = JMicroContext.get().getSysAccount();
			if(sai != null) {
				cx.setParam(Message.EXTRA_KEY_CLIENT_ID+"", sai.getClientId());
			}
		}
		
		byte logLevel =  msg.getLogLevel();
		if(logLevel != MC.LOG_NO) {
			cx.setParam(JMicroContext.LINKER_ID, msg.getLinkId());
			cx.setParam(Constants.NEW_LINKID, false);
		}
		
		cx.setByte(JMicroContext.SM_LOG_LEVEL,logLevel);
		
		boolean iMonitorable = false;
		boolean isDebug = false;
		
		if(sm != null) {
			iMonitorable = sm.getMonitorEnable() == 1;
			isDebug = sm.getDebugMode() == 1;
			logLevel = sm.getLogLevel();
		} else {
			iMonitorable = msg.isMonitorable();
			logLevel = msg.getLogLevel();
			isDebug = msg.isDebugMode();
		}
		
		//context.isLoggable = msg.isLoggable();
		//debug mode 下才有效
		cx.setParam(IS_DEBUG, isDebug);
		if(isDebug) {
			//long clientTime = msg.getTime();
			StringBuilder sb = new StringBuilder();
			//long curTime = TimeUtils.getCurTime();
			//context.setParam(CLIENT_UP_TIME, msg.getTime());
			
			cx.setParam(DEBUG_LOG, sb);
		}
		
		cx.setParam(IS_MONITORENABLE, iMonitorable);
		if(iMonitorable) {
			initMrpcStatisItem();
		}
		
		if(logLevel != MC.LOG_NO) {
			initMrpcLogItem(true);
		}
	}
	
	public static void configHttpProvider(HttpRequest req, ServiceMethodJRso sm) {
		
		JMicroContext cx = get();
		
		cx.curCxt.clear();
		
		setCallSide(true);
		
		ISession s = req.getSession();
		
		cx.setBoolean(HTTP_REQ, true);
		
		//将HTTP头部信息全部放RPC上下文中，然后在RPC调用时上送服务端
		Map<String,String> httpParams = new HashMap<>();
		cx.setParam(HTTP_PARAM_KEY, httpParams);
		
		Map<String,String>  hs = req.getHeaderParams();
		if(hs != null && !hs.isEmpty()) {
			for(Map.Entry<String, String> e : hs.entrySet()) {
				httpParams.put(e.getKey(), e.getValue());
			}
		}
		
		cx.setParam(JMicroContext.SESSION_KEY, s);
		
		cx.setParam(JMicroContext.REMOTE_HOST, s.remoteHost());
		cx.setParam(JMicroContext.REMOTE_PORT, s.remotePort());
		//cx.setParam(JMicroContext.REMOTE_INS_ID, msg.getInsId());
		cx.setParam(JMicroContext.ORIGIN_MSG, req);
		
		cx.setParam(JMicroContext.LOCAL_HOST, s.localHost());
		cx.setParam(JMicroContext.LOCAL_PORT, s.localPort()+"");
		
		String lk = req.getHeaderParam(Message.EXTRA_KEY_LOGIN_KEY+"");
		String slk = req.getHeaderParam(Message.EXTRA_KEY_LOGIN_KEY+"");
		
		if(!Utils.isEmpty(lk)) {
			cx.setParam(JMicroContext.LOGIN_TOKEN, lk);
		}
		
		if(!Utils.isEmpty(slk)) {
			cx.setParam(JMicroContext.SLOGIN_TOKEN, slk);
		}
		
		String cid = req.getHeaderParam(Message.EXTRA_KEY_CLIENT_ID+"");
		if(cid != null) {
			cx.setParam(Message.EXTRA_KEY_CLIENT_ID+"", Integer.parseInt(cid));
		} else {
			ActInfoJRso sai = JMicroContext.get().getSysAccount();
			if(sai != null) {
				cx.setParam(Message.EXTRA_KEY_CLIENT_ID+"", sai.getClientId());
			}
		}
		
		Byte logLevel = MC.LOG_NO;
		
		cx.setParam(Constants.NEW_LINKID, false);
		cx.setByte(JMicroContext.SM_LOG_LEVEL, MC.LOG_NO);
		
		boolean iMonitorable = false;
		boolean isDebug = false;
		
		iMonitorable = sm.getMonitorEnable() == 1;
		isDebug = sm.getDebugMode() == 1;
		logLevel = sm.getLogLevel();
		
		//context.isLoggable = msg.isLoggable();
		//debug mode 下才有效
		cx.setParam(IS_DEBUG, isDebug);
		if(isDebug) {
			//long clientTime = msg.getTime();
			StringBuilder sb = new StringBuilder();
			//long curTime = TimeUtils.getCurTime();
			//context.setParam(CLIENT_UP_TIME, msg.getTime());
			
			cx.setParam(DEBUG_LOG, sb);
		}
		
		cx.setParam(IS_MONITORENABLE, iMonitorable);
		if(iMonitorable) {
			initMrpcStatisItem();
		}
		
		if(logLevel != MC.LOG_NO) {
			initMrpcLogItem(true);
		}
	}
	
	
	
	private static void initMrpcStatisItem() {
		JMicroContext context = cxt.get();
		JMStatisItemJRso item = context.getMRpcStatisItem();
		if(item == null) {
			synchronized(JMStatisItemJRso.class) {
				item = context.getMRpcStatisItem();
				if(item == null) {
					item = new JMStatisItemJRso();
					ActInfoJRso ai = context.getAccount();
					if(ai != null) {
						item.setClientId(ai.getClientId());
						//item.setActName(ai.getActName());
					}
					item.setRpc(true);
					
					//the pre RPC Request ID as the parent ID of this request
					context.setParam(MRPC_STATIS_ITEM, item);
				}
			}
		}
	}
	
	private static void initMrpcLogItem(boolean sideProdiver) {
		
		JMicroContext context = cxt.get();
		JMLogItemJRso item = context.getMRpcLogItem();
		if(item == null) {
			synchronized(JMLogItemJRso.class) {
				item = context.getMRpcLogItem();
				if(item == null) {
					item = new JMLogItemJRso();
					ActInfoJRso ai = context.getAccount();
					if(ai != null) {
						item.setActClientId(ai.getClientId());
						item.setSysClientId(Config.getClientId());
						item.setActName(ai.getActName());
					}
					//the pre RPC Request ID as the parent ID of this request
					context.setParam(MRPC_LOG_ITEM, item);
				}
			}
		}
		
		item.setProvider(sideProdiver);
		item.setReqParentId(context.getLong(REQ_PARENT_ID, -1L));
	}
	
	public static boolean isMsg(int flag, int mask) {
		Message msg = getMsg();
		if(msg == null) return false;
		return (msg.getExtrFlag() & mask) != 0;
	}
	
	public static boolean isMsg(short flag, short mask) {
		Message msg = getMsg();
		if(msg == null) return false;
		return (msg.getFlag() & mask) != 0;
	}
	
	public static boolean isFromApiGageway() {
		Message msg = getMsg();
		if(msg == null) return false;
		return msg.isFromApiGateway();
	}
	
	public static Message getMsg() {
		return cxt.get().getParam(ORIGIN_MSG, null);
	}
	
	public static boolean enableOrDisable(int siCfg,int smCfg) {
		return smCfg == 1 ? true: (smCfg == 0 ? false:(siCfg == 1 ? true:false));
	}
	
	public static void configComsumer(ServiceMethodJRso sm,ServiceItemJRso si) {
		JMicroContext context = cxt.get();
		//context.curCxt.clear();
		setCallSide(false);
		
		ActInfoJRso ai = context.getAccount();
		if(ai != null) {
			context.setParam(JMicroContext.LOGIN_TOKEN, ai.getLoginKey());
		}
		
		ActInfoJRso sai = context.getSysAccount();
		if(sai != null) {
			context.setParam(JMicroContext.SLOGIN_TOKEN, sai.getLoginKey());
		}
		
		//debug mode 下才有效
		boolean isDebug = enableOrDisable(si.getDebugMode(),sm.getDebugMode());
		context.setParam(IS_DEBUG, isDebug);
		if(isDebug) {
			context.setParam(DEBUG_LOG_BASE_TIME, TimeUtils.getCurTime());
			context.setParam(DEBUG_LOG, new StringBuilder("Comsumer "));
		}
		
		context.setParam(JMicroContext.REMOTE_INS_ID, si.getInsId());
		byte level = sm.getLogLevel()==MC.LOG_DEPEND?si.getLogLevel():sm.getLogLevel();
		context.setByte(JMicroContext.SM_LOG_LEVEL, level);

		boolean iMonitorable = enableOrDisable(si.getMonitorEnable(),sm.getMonitorEnable());
		context.setParam(IS_MONITORENABLE, iMonitorable);
		if(iMonitorable) {
			initMrpcStatisItem();
		}
		
		if(level != MC.LOG_NO) {
			initMrpcLogItem(false);
			JMLogItemJRso mi = context.getMRpcLogItem();
			if(mi != null) {
				mi.setImplCls(si.getImpl());
				mi.setSmKey(sm.getKey());
				mi.setSysClientId(Config.getClientId());
				//ActInfoJRso ai = context.getAccount();
				if(ai != null) {
					mi.setActName(ai.getActName());
					mi.setActClientId(ai.getClientId());
					mi.setSysClientId(Config.getClientId());
				}
			}
		}
		
	}
	
	public static boolean existLinkId(){
		return get().exists(LINKER_ID);
	}
	
	public static Long createLid(){
		
		Long id = lid();
		if(id > 0) {
			return id;
		}
		
		JMicroContext c = get();
		ComponentIdServer idGenerator = EnterMain.getObjectFactory().get(ComponentIdServer.class,false);
		if(idGenerator != null) {
			id = idGenerator.getLongId(Linker.class);
			c.setLong(LINKER_ID, id);
		}
		return id;
	}
	
	public static long lid(){
		return get().getLong(LINKER_ID, -1L);
	}
	
	/**
	 * 同一个线程多个RPC之间上下文切换
	 */
	/*public void backupAndClear() {
		Map<String,Object> ps = new HashMap<>();
		ps.putAll(get().curCxt);
		ctxes.push(ps);
		get().curCxt.clear();
	}
	
	public void restore() {
		get().curCxt.clear();
		if(ctxes.isEmpty()) {
			throw new CommonException("JMicro Context stack is empty");
		}
		Map<String,Object> ps = ctxes.pop();
		get().curCxt.putAll(ps);
	}*/
	
	public ActInfoJRso getAccount() {
		 return JMicroContext.get().getParam(JMicroContext.LOGIN_ACT, null);
	}
	
	public void setAccount(ActInfoJRso act) {
		 if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法设置当前账号");
		 }
		 JMicroContext.get().setParam(JMicroContext.LOGIN_ACT, act);
	}
	
	public ActInfoJRso getSysAccount() {
		 return JMicroContext.get().getParam(JMicroContext.LOGIN_ACT_SYS, null);
	}
	
	public void setSysAccount(ActInfoJRso act) {
		 if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法设置当前系统登陆账号");
		 }
		 JMicroContext.get().setParam(JMicroContext.LOGIN_ACT_SYS, act);
	}
	
	public boolean hasPermission(int reqLevel) {
		 ActInfoJRso ai = getAccount();
		 if(ai != null) {
			return  true/*ai.getClientId() <= reqLevel*/;
		 }
		 return false;
	}
	
	public boolean hasPermission(int reqLevel, int defaultLevel) {
		 ActInfoJRso ai =  getAccount();
		 if(ai != null) {
			return ai.getClientId()<= reqLevel;
		 } else {
			 return defaultLevel <= reqLevel;
		 }
	}
	
	public void getAllParams(Map<String,Object> params) {
		params.putAll(this.curCxt);
	}
	
	public boolean isDebug(){
		return this.getBoolean(IS_DEBUG, false);
	}
	
	public boolean isMonitorable(){
		return this.getBoolean(IS_MONITORENABLE, false);
	}
	
	//debug mode 下才有效
	public StringBuilder getDebugLog() {
		return this.getParam(DEBUG_LOG, null);
	}
	
	//debug mode 下才有效
	public void appendCurUseTime(String label,boolean force) {
		if(isDebug()) {
			ServiceMethodJRso sm = this.getParam(Constants.SERVICE_METHOD_KEY, null);
			if(sm != null) {
				long curTime = TimeUtils.getCurTime();
				long cost = curTime - this.getLong(DEBUG_LOG_BASE_TIME, curTime);
				if(force || cost > sm.getTimeout()-100) {
					//超时的请求才记录下来
					StringBuilder sb = this.getDebugLog();
					if(sb != null)
						sb.append(",").append(label).append(": ").append(cost);
				}
			}
		}
	}
	
	private void debugLog() {
		if(!this.isDebug()) {
			return;
		}

		ServiceMethodJRso sm = (ServiceMethodJRso)JMicroContext.get().getObject(Constants.SERVICE_METHOD_KEY, null);
		
		int timeout = 3000;
		/*if(sm != null) {
			timeout = sm.getTimeout();
		}*/
		
		StringBuilder log = this.getDebugLog();
		this.removeParam(DEBUG_LOG);
		long curTime = TimeUtils.getCurTime();
		long cost = curTime - this.getLong(DEBUG_LOG_BASE_TIME, curTime);
		
		if(timeout > 0) {
			if(cost > timeout) {
				//超时的请求才记录下来
				log.append(", cost expect :").append(timeout).append(" : ").append(cost);
				logger.warn(log.toString());
				if(LG.isLoggable(MC.LOG_DEBUG)) {
					LG.log(MC.LOG_DEBUG, "NL", log.toString());
				}
			}
		} else if(cost > timeout-100) {
			//超时的请求才记录下来
			log.append(", maybe timeout expect :").append((timeout-100)).append(" : ").append(cost);
			logger.warn(log.toString());
			if(LG.isLoggable(MC.LOG_INFO)) {
				LG.log(MC.LOG_INFO, "NL", log.toString());
			}
		}
	}
	

	public void putAllParams(Map<String,Object> params){
		if(params == null || params.isEmpty()) {
			return;
		}
		
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		
		for(Map.Entry<String, Object> p : params.entrySet()){
			this.curCxt.put(p.getKey(), p.getValue());
		}
	}
	
	public boolean exists(String key){
		return this.curCxt.containsKey(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getParam(String key,T defautl){
		T v = (T)this.curCxt.get(key);
		if(v == null){
			Boolean b = (Boolean)this.curCxt.get(HTTP_REQ);
			if(b != null && b) {
				//http参数必须是字符串String类型
				Map<String,String> hps = (Map<String,String>)this.curCxt.get(HTTP_PARAM_KEY);
				if(hps != null) {
					v = (T)hps.get(key);
					if(v != null) return v;
				}
			}
			return defautl;
		}
		
		return v;
	}
	
	/**
	 * 客户端用户登录账号
	 * @param key
	 * @param defautl
	 * @return
	 */
	public <T> boolean setSessionParam(String key,T val){
		return setSessionParam0(this.getAccount(),key,val);
		
	}
	
	/**
	 * 系统登录账号
	 * @param key
	 * @param defautl
	 * @return
	 */
	public <T> boolean setSSessionParam(String key,T val){
		return setSessionParam0(this.getSysAccount(),key,val);
	}
	
	private <T> boolean setSessionParam0(ActInfoJRso ai, String fname, T val) {
		if(ai == null) {
			return false;
		}
		
		String k = sessionDataKey(ai.getActName());
		boolean r = ch.hput(k, fname, val);
		if(r) {
			ch.expire(k, AccountManager.expired);
		}
		return r;
	}
	
	public boolean removeSessionParam(String fname) {
		return removeSessionParam0(this.getAccount(),fname);
	}
	
	public boolean removeSSessionParam(String fname) {
		return removeSessionParam0(this.getSysAccount(),fname);
	}
	
	private boolean removeSessionParam0(ActInfoJRso ai, String fname) {
		if(ai == null) {
			return false;
		}
		String k = sessionDataKey(ai.getActName());
		return ch.hdel(k, fname);
	}
	
	private String sessionDataKey(String token) {
		return SESSION_DATA_PREFIX + token;
	}

	/**
	 * 客户端用户登录账号
	 * @param key
	 * @param defautl
	 * @return
	 */
	public <T> T getSessionParam(String fname,T defautl,Class<T> type){
		return getSessionParam0(this.getAccount(),fname,defautl,type);
	}
	
	/**
	 * 系统登录账号
	 * @param key
	 * @param defautl
	 * @return
	 */
	public <T> T getSSessionParam(String fname,T defautl,Class<T> type){
		return getSessionParam0(this.getSysAccount(),fname,defautl,type);
	}
	
	private <T> T getSessionParam0(ActInfoJRso ai,String fname,T defautl,Class<T> type){
		if(ai == null) return null;
		T v = ch.hget(sessionDataKey(ai.getActName()), fname,type);
		if(v == null) return defautl;
		return v;
	}
	
	public void removeParam(String key){
		checkPermission(key);
	    this.curCxt.remove(key);
	}
	
	public <T> void setParam(String key,T val){
		checkPermission(key);
		this.curCxt.put(key,val);
	}
	
	public void setInt(String key,int defautl){
		checkPermission(key);
	    this.setParam(key,defautl);
	}
	
	public void setByte(String key,byte defautl){
		checkPermission(key);
	    this.setParam(key,defautl);
	}
	
	public void setString(String key,String val){
		 checkPermission(key);
		 this.setParam(key,val);
	}
	
	public void setBoolean(String key,boolean val){
		 checkPermission(key);
		 this.setParam(key,val);
	}
	
	private static void checkPermission(String key) {
		if(SYSTEM_KEYS.containsKey(key) && !Utils.formSystemPackagePermission(4)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
	}
	
	public void setFloat(String key,Float val){
		checkPermission(key);
		 this.setParam(key,val);
	}
	
	public void setDouble(String key,Double val){
		checkPermission(key);
		 this.setParam(key,val);
	}
	
	public void setLong(String key,Long val){
		checkPermission(key);
		 this.setParam(key,val);
	}
	
	public void setObject(String key,Object val){
		checkPermission(key);
		 this.setParam(key,val);
	}
	
	public Integer getInt(String key,Integer defautl){
		return this.getParam(key,defautl);
	}
	
	public Byte getByte(String key,Byte defautl){
		return this.getParam(key,defautl);
	}
	
	public Long getLong(String key,Long defautl){
		return this.getParam(key,defautl);
	}
	
	public String getString(String key,String defautl){
		return this.getParam(key,defautl);
	}
	
	public Boolean getBoolean(String key,boolean defautl){
		return this.getParam(key,defautl);
	}
	
	public Float getFloat(String key,Float defautl){
		return this.getParam(key,defautl);
	}
	
	public Double getDouble(String key,Double defautl){
		return this.getParam(key,defautl);
	}
	
	public Object getObject(String key,Object defautl){
		return this.getParam(key,defautl);
	}
	
	public static final Map<String,String> SYSTEM_KEYS = new HashMap<>();
	static {
		
		Field[] fs = MC.class.getDeclaredFields();
		for(Field f: fs){
			try {
				if(f.getType() == String.class) {
					SYSTEM_KEYS.put(f.get(null).toString(), "");
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
