package cn.jmicro.gateway.msg;

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
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.gateway.IGatewayMessageCallbackJMSrv;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.ISessionListener;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.ITickerAction;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/**
 * 消息网关，专门处理客户端转发消息
 * @author Yulei Ye
 * @date 2022年4月10日 上午8:28:52
 */
@Component
public class MsgGatewayManager {

	private final static Logger logger = LoggerFactory.getLogger(MsgGatewayManager.class);
	
	public static final String MESSAGE_SERVICE_REG_ID = "__messageServiceRegId";
	public static final String TIMER_KEY = "__MessageRegistionStatusCheck";
	
	//public static final String APPEND_ID2_TOPIC_PATH = "appendId2TopicPath";
	
	public static final String TAG = MessageServiceImpl.class.getName();
	
	private Map<String,Set<Registion>> topic2Sessions = new HashMap<>();
	
	private Map<Integer,String> id2Topic = new HashMap<>();
	
	private Map<Integer,Registion> actId2Sessions = new HashMap<>();
	
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
	
	@Inject
	private AccountManager accountManager;
	
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
	
	public long forward(Message msg, Integer tactId) {
		//备份消息类型，返回给发消息发送者
		byte t = msg.getType();
		
		//改为异步消息返回给目标用户
		msg.setType(Constants.MSG_TYPE_ASYNC_RESP);
		
		msg.setMsgId(idServer.getLongId(Message.class));
		msg.putExtra(Message.EXTRA_KEY_SMSG_ID, msg.getMsgId());
		
		Registion r = this.actId2Sessions.get(tactId);
		if(r == null) {
			logger.warn("Act not online: " + tactId);
			return -1;
		}
		
		ByteBuffer bb = (ByteBuffer)msg.getPayload();
		logger.info(new String(bb.array()));
		
		r.sess.write(msg);//直接转发消息
		
		//还原消息类型
		msg.setType(t);
		return msg.getMsgId();
	}
	
	public int subscribe(ISession session, String topic, Message msg) {
		if(StringUtils.isEmpty(topic)) {
			logger.error("Topic cannot be NULL");
			return -1;
		}
		
		if(!pm.isPubsubEnable(0)) {
			return -2;
		}
		
		Set<Registion> sess = this.topic2Sessions.get(topic);
		if(sess == null) {
			synchronized(topic2Sessions) {
				sess = this.topic2Sessions.get(topic);
				if(sess == null) {
					this.topic2Sessions.put(topic, sess = new HashSet<Registion>());
				}
			}
		}
		
		Set<UniqueServiceKeyJRso> items = reg.getServices(IGatewayMessageCallbackJMSrv.class.getName());
		if(items == null) {
			logger.error(IGatewayMessageCallbackJMSrv.class.getName() + " service item not found!");
			return -1;
		}		
		
		boolean flag = false;
		
		for(UniqueServiceKeyJRso si : items) {
			ServiceItemJRso sit = this.srvManager.getServiceByKey(si.fullStringKey());
			if(sit == null) continue;
			ServiceMethodJRso sm = sit.getMethod("onPSMessage", new Class[] {new PSDataJRso[0].getClass()});
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
						reg.update(sit);
					}
				} else {
					sm.setTopic(topic);
					reg.update(sit);
				}
				
			} else {
				logger.error("onMessage method not found!");
				return -1;
			}
		}
		
		if(flag) {
			
			if(msg.getExtraMap() == null || msg.getExtraMap().isEmpty()) {
				logger.error("Login key not found with empty extra map");
				return -1;
			}
			
			Object lk = msg.getExtraMap().get(Message.EXTRA_KEY_LOGIN_KEY);
			if(lk == null) {
				logger.error("Login key not found key: " + Message.EXTRA_KEY_LOGIN_KEY+", topic: " + topic);
				return -1;
			}
			
			ActInfoJRso ai = this.accountManager.getAccount(lk.toString());
			if(ai == null) {
				logger.error("Act not found by: " + lk);
				 return -1;
			}
			
			Registion r = new Registion();
			//r.ctx = ctx;
			r.id = this.idServer.getIntId(MessageServiceImpl.class);
			r.sess = session;
			r.topic = topic;
			r.actId = ai.getId();
			r.clientId = ai.getClientId();
			r.lastActiveTime = TimeUtils.getCurTime();
			
			sess.add(r);
			Set<Integer> ids = session.getParam(MESSAGE_SERVICE_REG_ID);
			if(ids == null) {
				ids = new HashSet<Integer>();
				session.putParam(MESSAGE_SERVICE_REG_ID, ids);
			}
			ids.add(r.id);
			this.id2Topic.put(r.id, r.topic);
			actId2Sessions.put(ai.getId(), r);
			session.addSessionListener(seeesionListener);
			return r.id;
		}
		
		return -1;
	}

	public boolean unsubscribe(Integer id) {
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
			this.actId2Sessions.remove(rr.actId);
		}
		
		logger.debug("unregist topic:{} id:{} ",rr.topic,rr.id);
		
		if(sess.isEmpty()) {
			Set<UniqueServiceKeyJRso> items = reg.getServices(IGatewayMessageCallbackJMSrv.class.getName());
			if(items == null) {
				logger.error(IGatewayMessageCallbackJMSrv.class.getName() + " service item not found!");
				return true;
			}
			
			for(UniqueServiceKeyJRso si : items) {
				ServiceItemJRso sit = this.srvManager.getServiceByKey(si.fullStringKey());
				ServiceMethodJRso sm = sit.getMethod("onPSMessage", new Class[] {new PSDataJRso[0].getClass()});
				if(sm != null) {
					logger.debug("remmove topic:{} from:{} ",rr.topic,sm.getKey().fullStringKey());
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
						reg.update(sit);
					}
				} else {
					logger.error("onMessage method not found!");
					return false;
				}
			}
		}
		return true;
	}
	

	public void publishOneMessage(PSDataJRso i) {
		
		try {
			
			Message msg = new Message();
			msg.setType(Constants.MSG_TYPE_ASYNC_RESP);
			
			//强制使用JSON下发数据
			msg.setPayload(ICodecFactory.encode(codecFactory, i, Message.PROTOCOL_JSON));
			
			//强制使用JSON下发数据
			msg.setDownProtocol(Message.PROTOCOL_JSON);
			
			forwardMsgByTopic(msg,i.getTopic(),i.getSrcClientId(),i.getContext());
			
		} catch (Throwable e) {
			logger.error("",e);
		}	
	
	}
	
	private void forwardMsgByTopic(Message msg,String topic, Integer srcClientId, Map<String,Object> cxt) {
		Set<Registion> rsList = topic2Sessions.get(topic);
		if(rsList == null || rsList.isEmpty()) {
			logger.warn("No subcriber for topic: " + topic);
			return;
		}
		
		Set<Registion> rs = new HashSet<>();
		rs.addAll(rsList);
		
		//System.out.println("QPS type: "+MonitorConstant.STATIS_QPS+"="+i.getData());
		
		Map<String,Object> context = null;
		
		if(cxt != null) {
			context = new HashMap<>();
			context.putAll(cxt);
		}
		
		for(Registion r : rs) {
			/*if(srcClientId > 0 && r.clientId != srcClientId) {
				logger.warn("Source clientId:" + srcClientId+", target clientId:" + r.clientId+", topic: "+ topic);
				continue;
			}*/
			
			if(context != null && r.ctx != null && !r.ctx.isEmpty()) {
				cxt.clear();
				cxt.putAll(context);
				cxt.putAll(r.ctx);
			}
			
			try {
				r.sess.write(msg);
				r.lastActiveTime = TimeUtils.getCurTime();
			} catch (Throwable e) {
				logger.error("onMessage write error will unsubscribe the topic: "+r.topic,e);
				this.unsubscribe(r.id);
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
	
	public void jready() {
		TimerTicker timer = TimerTicker.getDefault(30*1000L);
		timer.addListener(TIMER_KEY, null, tickerAct);

	}

	private class Registion{
		public int id;
		public int clientId;
		public int actId;
		
		public ISession sess;
		
		public String topic;
		
		public Map<String,Object> ctx;
		
		public long lastActiveTime = TimeUtils.getCurTime();
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + id;
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Registion other = (Registion) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (id != other.id)
				return false;
			return true;
		}
		
		private MsgGatewayManager getOuterType() {
			return MsgGatewayManager.this;
		}
		
		
	}

	public Long forward(Message msg, String topic) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		if(ai == null) {
			logger.error("消息转发失败，账号未登录 topic:" + topic);
			return 0L;
		}
		
		//改为异步消息返回给目标用户
		msg.setType(Constants.MSG_TYPE_ASYNC_RESP);
		
		msg.putExtra(Message.EXTRA_KEY_SMSG_ID, msg.getMsgId());
		msg.setMsgId(idServer.getLongId(Message.class));
		
		forwardMsgByTopic(msg,topic,ai.getClientId(),null);
		return msg.getMsgId();
	}

}
