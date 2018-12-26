package org.jmicro.api.pubsub;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IChildrenListener;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.raft.INodeListener;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
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

	private final static Logger logger = LoggerFactory.getLogger(PubSubManager.class);
	
	/**
	 * pubsub servers listener
	 * key is the service name, value is the service proxy
	 */
	//@Reference
	//private Map<String,IInternalSubRpc> pubSubServers = new ConcurrentHashMap<>();
	
	@Inject
	private IRegistry registry;
	
	@Inject
	private IDataOperator dataOp;
	
	/**
	 * default pubsub server
	 */
	@Reference(namespace="org.jmicro.pubsub.DefaultPubSubServer",version="0.0.1",
			handler="specailInvocationHandler",required=false)
	private IInternalSubRpc defaultServer;
	
	/**
	 * is enable pubsub feature
	 */
	@Cfg(value="/PubSubManager/enable",changeListener="init")
	private boolean enable = true;
	
	/**
	 * default pubsub server name
	 */
	//@Cfg(value="/PubSubManager/defaultServerName",changeListener="init")
	//private String defaultServerName = Constants.DEFAULT_PUBSUB;
	
	/**
	 * topic listener
	 */
	private Set<ITopicListener> topicListeners = new HashSet<>();
	
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
	private Set<ISubsListener> subListeners = new HashSet<>();
	
	/**
	 * subscriber path to context list
	 * key is subscriber path and value the context
	 */
	private Map<String,Map<String,String>> path2SrvContext = new HashMap<>();
	
	private INodeListener topicNodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == INodeListener.NODE_ADD){
				logger.error("NodeListener service add "+type+",path: "+path);
			} else if(type == INodeListener.NODE_REMOVE) {
				logger.error("service remove:"+type+",path: "+path);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};
	
	private INodeListener subsNodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == INodeListener.NODE_ADD){
				logger.error("NodeListener service add "+type+",path: "+path);
			} else if(type == INodeListener.NODE_REMOVE) {
				logger.error("service remove:"+type+",path: "+path);
				srvRemove(path);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};
	
	private IChildrenListener topicChildrenListener =  new IChildrenListener() {
		@Override
		public void childrenChanged(String path, List<String> children) {
			String topic = path.substring(Config.PubSubDir.length()+1);
			for(String srv : children) {
				srvAdd(topic,srv);
			}
		}
	};
	
	public void init() {
		if(!enable) {
			logger.error("Pubsub server is disable by config [/PubSubManager/enable]");
			return;
		}
		
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
		
		logger.info("add listener");
		this.dataOp.addChildrenListener(Config.PubSubDir, new IChildrenListener() {
			@Override
			public void childrenChanged(String path, List<String> children) {
				topicsAdd(children);
			}
		});	
	}
	
	private void topicsAdd(List<String> children) {
		for(String topic: children) {
			topicAdd(topic);
		}
	}
	
	/**
	 * 1。 取主题下的所有监听服务
	 * 2。 监听主题下子结点
	 * @param parent
	 * @param topic
	 */
	private void topicAdd(String topic) {
		String path = Config.PubSubDir + "/" + topic;
		List<String> children = this.dataOp.getChildren(path);
		
		if(children == null || children.isEmpty()) {
			this.dataOp.addChildrenListener(path, topicChildrenListener);
			return;
		}
		
		for(String srv : children) {
			srvAdd(topic,srv);
		}
		
		this.dataOp.addChildrenListener(path, topicChildrenListener);
	}

	private void srvAdd(String topic, String srv) {
		String srvPath = Config.PubSubDir + "/" + topic + "/" + srv;
		if(!path2SrvContext.containsKey(srvPath)) {
			Map<String,String> context = null;
			String ctx = this.dataOp.getData(srvPath);
			if(StringUtils.isEmpty(ctx)) {
				context = new HashMap<String,String>();
				path2SrvContext.put(srvPath,context);
			}else {
				context = JsonUtils.getIns().getStringMap(ctx);
				path2SrvContext.put(srvPath, context);
			}
			this.notifySubListener(ISubsListener.SUB_ADD, srvPath, context);
			this.dataOp.addNodeListener(srvPath, subsNodeListener);
		}
	}
	
	private void srvRemove(String path) {
		this.notifySubListener(ISubsListener.SUB_ADD, path, path2SrvContext.get(path));
		path2SrvContext.remove(path);
		dataOp.removeNodeListener(path, subsNodeListener);
	}

	public void addTopicListener(ITopicListener l) {
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
	}
	
	
	public void addSubsListener(ISubsListener l) {
		if(subListeners == null) {
			subListeners = new HashSet<ISubsListener>();
		}
		subListeners.add(l);
		
		if(!this.path2SrvContext.isEmpty()) {
			for(Map.Entry<String, Map<String,String>> e : path2SrvContext.entrySet()) {
				String topic = this.getTopic(e.getKey());
				UniqueServiceMethodKey k = this.getMethodKey(e.getKey());
				l.on(ISubsListener.SUB_ADD, topic, k, e.getValue());
			}
		}
	}
	
	public void removeSubsListener(String path,ISubsListener l) {
		Set<ISubsListener> subs = subListeners;
		if(subs != null && subs.contains(l)) {
			subs.remove(l);
		}
	}
	
	private void notifySubListener(byte type,String path,Map<String,String> context) {
		Set<ISubsListener> subs = subListeners;
		if(subs != null && subs.isEmpty()) {
			return;
		}
		
		String topic = this.getTopic(path);
		UniqueServiceMethodKey k = this.getMethodKey(path); 
		
		Iterator<ISubsListener> ite = subs.iterator();
		ISubsListener l = null;
		while(ite.hasNext()) {
			l = ite.next();
			l.on(type, topic,k, context);
		}
		
	}
	
	public boolean publish(Map<String,String> context, String topic, String content) {

		IInternalSubRpc s = this.defaultServer;// this.getServer(context);
		
		PSData item = new PSData();
		item.setTopic(topic);
		try {
			item.setData(content.getBytes(Constants.CHARSET));
		} catch (UnsupportedEncodingException e) {
			logger.error("topic:"+topic,e);
		}
		item.setContext(context);
		
		return s.publish(item);
		
	}
	
	public boolean publish(Map<String,String> context,String topic, byte[] content) {
		
		IInternalSubRpc s = this.defaultServer;//this.getServer(context);
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		
		return s.publish(item);
	}

	public boolean publish(PSData item) {
		IInternalSubRpc s = this.defaultServer;//this.getServer(item.getContext());
		return s.publish(item);
	}

	public boolean subscribe(Map<String,String> context,String topic, String srvName,
			String namespace, String version,String method) {
		String p = this.getPath(topic, srvName, namespace, version, method);
		dataOp.createNode(p,JsonUtils.getIns().toJson(context),false);	
		return true;
	}

	public boolean unsubcribe(Map<String,String> context,String topic, String srvName, 
			String namespace, String version,String method) {
		String p = this.getPath(topic, srvName, namespace, version, method);
		dataOp.deleteNode(p);
		return true;
	}
	
	private String getPath(String topic, String srvName, 
			String namespace, String version,String method) {
		Set<ServiceItem>  srvs = registry.getServices(srvName, namespace, version);
		if(srvs == null || srvs.isEmpty()) {
			String msg = "srv ["+srvName+"], namespace ["+ namespace+"], version ["+version+"] not found";
			try {
				logger.warn(msg);
				logger.warn("Wainting for 5 seconds");
				Thread.sleep(6*1000);
			} catch (InterruptedException e) {
			}
			srvs = registry.getServices(srvName, namespace, version);
			if(srvs == null || srvs.isEmpty()) {
				throw new CommonException(msg);
			}
			logger.warn("Get it after 5 seconds "+ msg);
		}
		
		ServiceItem si = srvs.iterator().next();
		ServiceMethod sm = si.getMethod(method, new Class[] {PSData.class});
		if(sm == null) {
			throw new CommonException("srv ["+srvName+"], namespace ["+ namespace+
					"], version ["+version+"], method [" + method + "] not found");
		}
		
		String p = Config.PubSubDir+"/" + topic.replaceAll("/", "_")+"/"+sm.getKey().toKey(false, false, false);
	    return p;
	}
	
	private String getTopic(String path) {
		String str = path.substring(Config.PubSubDir.length()+1);
		str = str.substring(0,str.indexOf("/"));
		str = str.replaceAll("_", "/");
		return str;
	}
	
	private UniqueServiceMethodKey getMethodKey(String path) {
		String str = path.substring(path.lastIndexOf("/")+1);
		return UniqueServiceMethodKey.fromKey(str);
	}

	/*private IInternalSubRpc getServer(Map<String,String> context) {
		
		if(!enable) {
			throw new CommonException("PubSub Server Is Disable!");
		}
		
		IInternalSubRpc s = defaultServer;
		String sn = Constants.DEFAULT_PUBSUB;
		if(context != null) {
			sn = context.get(Constants.PUBSUB_KEY);
			if(!StringUtils.isEmpty(sn)) {
				s = pubSubServers.get(sn);
			}
		}
		
		if(s == null) {
			throw new CommonException("PubSub Server ["+sn+"] Is Disable!");
		}
		
		return s;
	}*/
}
