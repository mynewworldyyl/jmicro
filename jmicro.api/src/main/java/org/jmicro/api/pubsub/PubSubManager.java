package org.jmicro.api.pubsub;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.config.Config;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:50
 */
@Component(value="pubSubManager")
public class PubSubManager {
	
	//生产者成功将消息放入消息队列,但并不意味着消息被消费者成功消费
	public static final int PUB_OK = 0;
	//无消息服务可用,需要启动消息服务
	public static final int PUB_SERVER_NOT_AVAILABALE = -1;
	//消息队列已经满了,客户端可以重发,或等待一会再重发
	public static final int PUB_SERVER_DISCARD = -2;
	//消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
	public static final int PUB_SERVER_BUSSUY = -3;

	private final static Logger logger = LoggerFactory.getLogger(PubSubManager.class);
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IRegistry registry;
	
	@Inject
	private IDataOperator dataOp;
	
	@Inject
	private ServiceManager srvManager;
	
	/**
	 * default pubsub server
	 */
	@Reference(namespace=Constants.DEFAULT_PUBSUB,version="0.0.1",required=false)
	private IInternalSubRpc defaultServer;
	
	/**
	 * is enable pubsub feature
	 */
	@Cfg(value="/PubSubManager/enable",defGlobal=false)
	private boolean enable = true;
	
	/**
	 * is enable pubsub server
	 */
	@Cfg(value="/PubSubManager/enableServer",defGlobal=false, changeListener="initPubSubServer")
	private boolean enableServer = false;
	
	@Cfg(value="/PubSubManager/openDebug",defGlobal=false)
	private boolean openDebug = true;
	
	/**
	 * default pubsub server name
	 */
	//@Cfg(value="/PubSubManager/defaultServerName",changeListener="init")
	//private String defaultServerName = Constants.DEFAULT_PUBSUB;
	
	/**
	 * topic listener
	 */
	//private Set<ITopicListener> topicListeners = new HashSet<>();
	
	/**
	 * The directory of the structure
	 * PubSubDir is the root directory
	 * Topic is pubsub topic and sub node is the listener of service method
	 * 
	 *            |            |--L2
	 *            |----topic1--|--L1
	 *            |            |--L3 
	 *            |
	 *            |            |--L1 
	 *            |            |--L2
	 *            |----topic2--|--L3
	 *            |            |--L4
	 * PubSubDir--|            |--L5   
	 *            |
	 *            |            |--L1
	 *            |            |--L2
	 *            |            |--L3
	 *            |----topic3--|--L4
	 *            |            |--L5
	 *                         |--L6
	 *
	 */
	private Set<ISubsListener> subListeners = Collections.synchronizedSet(new HashSet<>());
	
	/**
	 * subscriber path to context list
	 * key is subscriber path and value the context
	 */
	//private Map<String,Map<String,String>> path2SrvContext = new HashMap<>();
	
	private Map<String,Set<String>> topic2Method = new ConcurrentHashMap<>();
	
	//private Map<String,Boolean> srvs = new HashMap<>();
	
	/*private INodeListener topicNodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == INodeListener.NODE_ADD){
				logger.error("NodeListener service add "+type+",path: "+path);
			} else if(type == INodeListener.NODE_REMOVE) {
				logger.error("service remove:"+type+",path: "+path);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};*/
	
	/**
	 * 监听全部服务的增加操作，判断是否有订阅方法，如果有，则注册到对应的主是下面
	 */
  /*  private IServiceListener serviceParseListener = new IServiceListener() {
		@Override
		public void serviceChanged(int type, ServiceItem item) {
			if(type == IServiceListener.SERVICE_ADD) {
				parseServiceAdded(item);
			}else if(type == IServiceListener.SERVICE_REMOVE) {
				//serviceRemoved(item);
			}else if(type == IServiceListener.SERVICE_DATA_CHANGE) {
				//serviceDataChange(item);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+item.getKey().toKey(true, true, true));
			}
		}
	};*/
	
	private IServiceListener serviceAddedRemoveListener = new IServiceListener() {
		@Override
		public void serviceChanged(int type, ServiceItem item) {
			if(type == IServiceListener.SERVICE_ADD) {
				parseServiceAdded(item);
			}else if(type == IServiceListener.SERVICE_REMOVE) {
				serviceRemoved(item);
			}else if(type == IServiceListener.SERVICE_DATA_CHANGE) {
				serviceDataChange(item);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+item.getKey().toKey(true, true, true));
			}
		}
	};
	
	public void init1() {
		initPubSubServer();
		
		/*if(pubSubServers.isEmpty()) {
			throw new CommonException("No pubsub server found, pubsub is disable!");
		}
		
		if(!StringUtils.isEmpty(defaultServerName)) {
			IInternalSubRpc s = this.pubSubServers.get(defaultServerName);
			if(s == null) {
				logger.error("server [{}] not found",defaultServerName);
			}
			defaultServer = s;
		}*/
		
		/*logger.info("add listener");
		this.dataOp.addChildrenListener(Config.PubSubDir, new IChildrenListener() {
			@Override
			public void childrenChanged(String path, List<String> children) {
				topicsAdd(children);
			}
		});	
		*/
	}
	
	private void initPubSubServer() {
		if(!enableServer) {
			//不启用pubsub Server功能，此运行实例是一个
			logger.info("Pubsub server is disable by config [/PubSubManager/enableServer]");
			return;
		}
		Set<String> children = this.dataOp.getChildren(Config.PubSubDir,true);
		for(String t : children) {
			Set<String>  subs = this.dataOp.getChildren(Config.PubSubDir+"/"+t,true);
			for(String sub : subs) {
				this.dataOp.deleteNode(Config.PubSubDir+"/"+t+"/"+sub);
			}
		}
		srvManager.addListener(serviceAddedRemoveListener);
	}
	
	protected void serviceDataChange(ServiceItem item) {
		
	}

	protected void serviceRemoved(ServiceItem item) {
		
		for(ServiceMethod sm : item.getMethods()) {
			if(StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			
			if(this.topic2Method.containsKey(sm.getTopic())) {
				String mk = sm.getKey().toKey(false, false, false).intern();
				Set<String> ms = this.topic2Method.get(sm.getTopic());
				ms.remove(mk);
				
				if(ms.isEmpty()) {
					this.topic2Method.remove(sm.getTopic());
				}
				
				this.unsubcribe(null,sm);
			}
			
			this.notifySubListener(ISubsListener.SUB_REMOVE, sm.getTopic(), sm.getKey(), null);
		}
		/*
		String key = item.serviceName();
		if(srvs.containsKey(key)) {
			srvs.remove(key);
		}
		registry.removeServiceListener(key, serviceAddedRemoveListener);
		*/
		
	}

	protected void parseServiceAdded(ServiceItem item) {
		if(item == null || item.getMethods() == null) {
			return;
		}
		
		boolean flag = false;
		
		for(ServiceMethod sm : item.getMethods()) {
			if(StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			flag = true;
			
			if(!this.topic2Method.containsKey(sm.getTopic())) {
				this.topic2Method.put(sm.getTopic(), new HashSet<>());
			}
			
			String mk = sm.getKey().toKey(false, false, false).intern();
			Set<String> ms = this.topic2Method.get(sm.getTopic());
			if(!ms.contains(mk)) {
				this.subscribe(null, sm);
				ms.add(mk);
				if(openDebug) {
					logger.debug("Got ont CB: {}",mk);
				}
				this.notifySubListener(ISubsListener.SUB_ADD, sm.getTopic(), sm.getKey(), null);
			}
		}
		
		/*String key = item.serviceName();
		if(flag && !srvs.containsKey(key)) {
			srvs.put(key, true);
			registry.addExistsServiceListener(key, serviceAddedRemoveListener);
		}*/
	}

	/*public void addTopicListener(ITopicListener l) {
		topicListeners.add(l);
	}
	
	public void removeTopicListener(ITopicListener l) {
		topicListeners.remove(l);
	}
	
	public void notifyTopicListener(byte type,String topic,Map<String,String> context) {
		if(topicListeners.isEmpty()) {
			return;
		}
		
		Iterator<ITopicListener> ite = this.topicListeners.iterator();
		ITopicListener l = null;
		while(ite.hasNext()) {
			l = ite.next();
			l.on(type, topic, context);
		}
	}*/
	
	public void addSubsListener(ISubsListener l) {
		if(subListeners == null) {
			subListeners = new HashSet<ISubsListener>();
		}
		subListeners.add(l);
		
		if(!this.topic2Method.isEmpty()) {
			for(Map.Entry<String, Set<String>> e : topic2Method.entrySet()) {
				for(String key : e.getValue()) {
					UniqueServiceMethodKey k = UniqueServiceMethodKey.fromKey(key);
					l.on(ISubsListener.SUB_ADD, e.getKey(), k,null);
				}
			}
		}
	}
	
	public void removeSubsListener(ISubsListener l) {
		Set<ISubsListener> subs = subListeners;
		if(subs != null && subs.contains(l)) {
			subs.remove(l);
		}
	}
	
	public long publish(Map<String,Object> context, String topic, String content,byte flag) {

		IInternalSubRpc s = this.defaultServer;// this.getServer(context);
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		return this.publish(item);
		
	}
	
	public long publish(Map<String,Object> context,String topic, byte[] content,byte flag) {
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		item.setFlag(flag);
		return this.publish(item);
	}

	public long publish(PSData item) {
		IInternalSubRpc s = this.defaultServer;//this.getServer(item.getContext());
		if(s == null) {
			logger.error("No Pubsub server for topic:{}",item.getTopic());
			return PUB_SERVER_NOT_AVAILABALE;
		}
		if(openDebug) {
			logger.debug("Publish topic: {}, data: {}",item.getTopic(),item.getData());
		}
		if(item.getId() <= 0) {
			//为消息生成唯一ID
			//大于0时表示客户端已经预设置值
			item.setId(this.idGenerator.getIntId(PSData.class));
		}
		return s.publishData(item);
	}

	private boolean subscribe(Map<String,String> context,ServiceMethod sm) {
		String p = this.getPath(sm);
		String cxt = context == null ? "{ip:'localhost'}":JsonUtils.getIns().toJson(context);
		if(!dataOp.exist(p)) {
			dataOp.createNode(p, cxt, true);
		}
		return true;
	}

	private boolean unsubcribe(Map<String,String> context,ServiceMethod sm) {
		String p = this.getPath(sm);
		dataOp.deleteNode(p);
		return true;
	}
	
	private String getPath(ServiceMethod sm) {
		String p = Config.PubSubDir+"/" + sm.getTopic().replaceAll("/", "_");
		String key = sm.getKey().toKey(false, false, false);
		key = key.replaceAll("/","_");
		key = key.substring(0, key.length()-1);
	    return p+"/"+key;
	}

	private void notifySubListener(byte type,String topic,UniqueServiceMethodKey k,Map<String,String> context) {
		Set<ISubsListener> subs = subListeners;
		if(subs != null && subs.isEmpty()) {
			return;
		}
		
		/*String topic = this.getTopic(path);
		UniqueServiceMethodKey k = this.getMethodKey(path); */
		
		Iterator<ISubsListener> ite = subs.iterator();
		ISubsListener l = null;
		while(ite.hasNext()) {
			l = ite.next();
			l.on(type, topic,k, context);
		}
		
	}

	public boolean isEnableServer() {
		return this.enableServer;
	}
	
}
