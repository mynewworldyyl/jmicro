
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
package cn.jmicro.gateway;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.gateway.IGatewayMessageCallback;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.ISessionListener;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.ITickerAction;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 *     用于外部客户端订阅pubsub数据
 * 
 * @author Yulei Ye
 * @date 2020年3月26日
 */
@Component
@Service(version="0.0.1",showFront=false,external=false,infs=IGatewayMessageCallback.class,
side=Constants.SIDE_PROVIDER)
public class MessageServiceImpl implements IGatewayMessageCallback,IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);
	
	public static final String MESSAGE_SERVICE_REG_ID = "__messageServiceRegId";
	public static final String TIMER_KEY = "__MessageRegistionStatusCheck";
	
	//public static final String APPEND_ID2_TOPIC_PATH = "appendId2TopicPath";
	
	public static final String TAG = MessageServiceImpl.class.getName();
	
	private Map<String,Set<Registion>> topic2Sessions = new HashMap<>();
	
	private Map<Integer,String> id2Topic = new HashMap<>();
	
	@Cfg(value="/IGatewayMessageCallback/registSessionTimeout",defGlobal=true)
	private long registSessionTimeout = 1000*60*10;
	
	@Inject
	private IRegistry reg = null;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private PubSubManager pm;
	
	@Inject
	private ServiceManager srvManager;
	
	private ISessionListener seeesionListener = (int type, ISession s)->{
		if(type == ISession.EVENT_TYPE_CLOSE) {
			Set<Integer> ids = s.getParam(MESSAGE_SERVICE_REG_ID);
			if(ids != null && !ids.isEmpty()) {
				Set<Integer> idss = new HashSet<>();
				idss.addAll(ids);
				for(Integer id : idss) {
					unsubscribe(id);
				}
			}
		}
	};
	
	private int subscribe(ISession session, String topic,Map<String, Object> ctx) {
		if(StringUtils.isEmpty(topic)) {
			logger.error("Topic cannot be NULL");
			return -1;
		}
		
		if(!pm.isPubsubEnable(0)) {
			return -2;
		}
		
		Set<Registion> sess = this.topic2Sessions.get(topic);
		if(sess == null) {
			sess = new HashSet<Registion>();
			synchronized(topic2Sessions) {
				this.topic2Sessions.put(topic, sess);
			}
		}
		
		Set<ServiceItem> items = reg.getServices(IGatewayMessageCallback.class.getName());
		if(items == null) {
			logger.error(IGatewayMessageCallback.class.getName() + " service item not found!");
			return -1;
		}		
		
		boolean flag = false;
		
		for(ServiceItem si : items) {
			ServiceMethod sm = si.getMethod("onMessage", new Class[] {new PSData[0].getClass()});
			if(sm != null) {
				
				flag = true;
				if(StringUtils.isNotEmpty(sm.getTopic())) {
					String[] ts = sm.getTopic().split(Constants.TOPIC_SEPERATOR);
					boolean f = false;
					for(String t : ts) {
						if(topic.equals(t)) {
							f = true;
							break;
						}
					}
					if(!f) {
						sm.setTopic(sm.getTopic()+Constants.TOPIC_SEPERATOR+topic);
						reg.update(si);
					}
				} else {
					sm.setTopic(topic);
					reg.update(si);
				}
				
			} else {
				logger.error("onMessage method not found!");
			}
		}
		
		if(flag) {
			Registion r = new Registion();
			r.ctx = ctx;
			r.id = this.idServer.getIntId(MessageServiceImpl.class);
			r.sess = session;
			r.topic = topic;
			r.clientId = JMicroContext.get().getAccount().getId();
			r.lastActiveTime = TimeUtils.getCurTime();
			sess.add(r);
			Set<Integer> ids = session.getParam(MESSAGE_SERVICE_REG_ID);
			if(ids == null) {
				ids = new HashSet<Integer>();
				session.putParam(MESSAGE_SERVICE_REG_ID, ids);
			}
			ids.add(r.id);
			this.id2Topic.put(r.id, r.topic);
			session.addSessionListener(seeesionListener);
			return r.id;
		}
		
		return -1;
	}

	private boolean unsubscribe(Integer id) {
		String topic = this.id2Topic.get(id);
		if(StringUtils.isEmpty(topic)) {
			return true;
		}
		
		Set<Registion> sess = this.topic2Sessions.get(topic);
		if(sess == null) {
			return true;
		}
		
		Registion rr = null;
		for(Registion r : sess) {
			if(r.id == id) {
				rr = r;
				break;
			}
		}
		
		/*if(rr != null) {
			ActInfo ai = JMicroContext.get().getAccount();
			if(ai == null || ai.getClientId() != rr.clientId) {
				return false;
			}
		}*/
		
		this.id2Topic.remove(id);
		
		if(rr != null) {
			Set<Integer> ids = rr.sess.getParam(MESSAGE_SERVICE_REG_ID);
			if(ids != null) {
				ids.remove(id);
			}
			sess.remove(rr);
		}
		
		logger.debug("unregist topic:{} id:{} ",rr.topic,rr.id);
		
		if(sess.isEmpty()) {
			Set<ServiceItem> items = reg.getServices(IGatewayMessageCallback.class.getName());
			if(items == null) {
				logger.error(IGatewayMessageCallback.class.getName() + " service item not found!");
				return true;
			}
			
			for(ServiceItem si : items) {
				ServiceMethod sm = si.getMethod("onMessage", new Class[] {new PSData[0].getClass()});
				if(sm != null) {
					logger.debug("remmove topic:{} from:{} ",rr.topic,sm.getKey().toKey(false, false, false));
					if(StringUtils.isNotEmpty(sm.getTopic())) {
						String[] ts = sm.getTopic().split(Constants.TOPIC_SEPERATOR);
						StringBuffer sb = new StringBuffer();
						for(String t : ts) {
							if(topic.equals(t)) {
								continue;
							}
							sb.append(t).append(Constants.TOPIC_SEPERATOR);
						}
						if(sb.length() > 0) {
							sb.delete(sb.length()-1, sb.length());
						}
						sm.setTopic(sb.toString());
						reg.update(si);
					}
				} else {
					logger.error("onMessage method not found!");
					return false;
				}
			}
		}
		return true;
	}


	@Override
	@SMethod(asyncable=true,timeout=5000,retryCnt=0,needResponse=true)
	public void onPSMessage(PSData[] items) {
		if(items == null || items.length == 0) {
			return;
		}
		
		Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_ASYNC_RESP);
		
		//强制使用JSON下发数据
		msg.setDownProtocol(Message.PROTOCOL_JSON);
		
		for(PSData i : items) {
			
			try {
				Set<Registion> rsList = topic2Sessions.get(i.getTopic());
				if(rsList == null || rsList.isEmpty()) {
					continue;
				}
				
				Set<Registion> rs = new HashSet<>();
				rs.addAll(rsList);
				
				//System.out.println("QPS type: "+MonitorConstant.STATIS_QPS+"="+i.getData());
				
				Map<String,Object> context = null;
				
				if(i.getContext() != null) {
					context = new HashMap<>();
					context.putAll(i.getContext());
				}
				
				for(Registion r : rs) {
					if(i.getSrcClientId() > 0 && r.clientId != i.getSrcClientId()) {
						continue;
					}
					
					if(context != null && r.ctx != null && !r.ctx.isEmpty()) {
						i.getContext().clear();
						i.getContext().putAll(context);
						i.getContext().putAll(r.ctx);
					}
					
					//强制使用JSON下发数据
					msg.setPayload(ICodecFactory.encode(codecFactory, i, Message.PROTOCOL_JSON));
					
					try {
						r.sess.write(msg);
						r.lastActiveTime = TimeUtils.getCurTime();
					} catch (Throwable e) {
						logger.error("onMessage write error will unsubscribe the topic: "+r.topic,e);
						this.unsubscribe(r.id);
					}
				}
				
			} catch (Throwable e) {
				logger.error("",e);
			}	
		}
		
	}
	
	private ITickerAction<Object> tickerAct = new ITickerAction<Object>() {
		public void act(String key,Object attachement) {
			Set<Integer> ids = new HashSet<>();
			for(Set<Registion> rs : topic2Sessions.values()) {
				if(rs == null || rs.isEmpty()) {
					continue;
				}

				for(Iterator<Registion> ite = rs.iterator(); ite.hasNext();) {
					Registion r = ite.next();
					if(r.sess.isClose()) {
						ids.add(r.id);
					}
				}
			}
			
			for(Integer id : ids) {
				unsubscribe(id);
			}
		}
	};
	
	@JMethod("ready")
	public void ready() {
		TimerTicker timer = TimerTicker.getDefault(30*1000L);
		timer.addListener(TIMER_KEY, null, tickerAct);
		//-2120102654
		//-1331833745
		srvManager.registSmCode("cn.jmicro.gateway.MessageServiceImpl","mng", "0.0.1", "subscribe",
				new Class[] {ISession.class,String.class,Map.class});
		srvManager.registSmCode("cn.jmicro.gateway.MessageServiceImpl","mng", "0.0.1", "unsubscribe",
				new Class[] {Integer.class});
	}

	private class Registion{
		public int id;
		public int clientId;
		public ISession sess;
		public String topic;
		public Map<String,Object> ctx;
		public long lastActiveTime = TimeUtils.getCurTime();
	}

	@Override
	public Byte type() {
		return Constants.MSG_TYPE_PUBSUB;
	}

	@Override
	public boolean onMessage(ISession session, Message msg) {
		ByteBuffer bb = (ByteBuffer)msg.getPayload();
		String json = new String(bb.array(),0,bb.remaining());
		Map<String,Object> params = JsonUtils.getIns().getStringKeyMap(json);
		
		Object op = params.get("op");
		if( op == null) {
			responseError(session,msg,ServerError.SE_INVALID_OP_CODE,"Op code is null");
			return true;
		}
		
		params.remove("op");
		
		int opCode = Integer.parseInt(op.toString());
		if(opCode == 1) {
			String topic = (String)params.get("topic");
			if(Utils.isEmpty(topic)) {
				responseError(session,msg,ServerError.SE_INVALID_TOPIC,"Topic is null");
				return true;
			}
			params.remove("topic");
			int subId = this.subscribe(session, topic, params);
			msg.setPayload(subId);
		} else if(opCode == 2) {
			String subId = (String)params.get("subId");
			if(Utils.isEmpty(subId)) {
				responseError(session,msg,ServerError.SE_INVALID_SUB_ID,"Invalid subscribe id");
				return true;
			}
			int sid = Integer.parseInt(subId);
			boolean suc = this.unsubscribe(sid);
			msg.setPayload(suc);
		}
		msg.setError(false);
		session.write(msg);
		return true;
	}

	private void responseError(ISession s,Message msg,int seInvalidTopic, String msgStr) {
		msg.setError(true);
		ServerError se = new ServerError(seInvalidTopic,msgStr);
		try {
			byte[] d = JsonUtils.getIns().toJson(se).getBytes(Constants.CHARSET);
			msg.setPayload(ByteBuffer.wrap(d));
			s.write(msg);
		} catch (UnsupportedEncodingException e) {
			logger.error(se.toString(),e);
		}
	}
	
}
