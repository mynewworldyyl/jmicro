package org.jmicro.api.pubsub;

import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.config.Config;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
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
	
	@Cfg(value="/PubSubManager/openDebug",defGlobal=false)
	private boolean openDebug = true;
	
	@Inject
	private IDataOperator dataOp;
	
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
	 * subscriber path to context list
	 * key is subscriber path and value the context
	 */
	//private Map<String,Map<String,String>> path2SrvContext = new HashMap<>();
	
	
	
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
	
	
	
	public void init1() {
		//initPubSubServer();
		
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
	
	public boolean subsubcre(Object srv,String method, String topic,Map<String,String> context) {
		
		return true;
	}
	
	
	private boolean doSaveSubscribe(Map<String,String> context, ServiceMethod sm) {
		String p = this.getPath(sm);
		String cxt = context == null ? "{ip:'localhost'}":JsonUtils.getIns().toJson(context);
		if(!dataOp.exist(p)) {
			dataOp.createNode(p, cxt, true);
		}
		return true;
	}

	private boolean doSaveUnsubcribe(Map<String,String> context,ServiceMethod sm) {
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
	
}
