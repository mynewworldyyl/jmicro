package org.jmicro.registry.zk;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.zkclient.ZkclientZookeeperTransporter;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Lazy;
import org.jmicro.api.annotation.Registry;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.Constants;
import org.jmicro.common.JMicroContext;
import org.jmicro.common.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Registry(Constants.DEFAULT_REGISTRY)
@Lazy(false)
public class ZKRegistry implements IRegistry {

	private final static Logger logger = LoggerFactory.getLogger(ZKRegistry.class);
	
	private ZookeeperClient client = null;
	
	private Map<String,Set<ServiceItem>> serviceItems = new ConcurrentHashMap<String,Set<ServiceItem>>();
	
	@Override
	@JMethod("init")
	public void init() {
		Config cfg = JMicroContext.getCfg();
		URL url = cfg.getRegistryUrl();
		this.client = new ZkclientZookeeperTransporter().connect(url);
		
		//this.client = new CuratorZookeeperTransporter().connect(url);
		this.client.addStateListener((state)->{
			if(StateListener.CONNECTED == state) {
				logger.debug("ZKRegistry CONNECTED, add listeners");
			}else if(StateListener.DISCONNECTED == state) {
				logger.debug("ZKRegistry DISCONNECTED");
			}else if(StateListener.RECONNECTED == state) {
				logger.debug("ZKRegistry Reconnected and reregist Services");
				for(Set<ServiceItem> sis : serviceItems.values()) {
					for(ServiceItem si: sis) {
						regist(si);
					}	
				}
			}
		});
		
		this.client.addChildListener(ServiceItem.ROOT, (path,children)->{
			serviceAdd(path,children);
		});
		List<String> childrens = this.client.getChildren(ServiceItem.ROOT);
		logger.debug("Service: "+childrens.toString());
		serviceAdd(ServiceItem.ROOT,childrens);
	}

	private void serviceAdd(String path, List<String> children) {
		
		for(String child : children){
			String data = this.client.data(path+"/"+child);
			String serviceInterName = ServiceItem.serviceName(child);
			logger.debug("service add: " + child);
			if(!serviceItems.containsKey(serviceInterName)){
				serviceItems.put(serviceInterName, new HashSet<ServiceItem>());
			}
			Set<ServiceItem> items = serviceItems.get(serviceInterName);
			items.add(new ServiceItem(child,data));
		}
		
	}

	@Override
	public void regist(ServiceItem item) {
		String key = item.key();
		logger.debug("regist service: "+key);
		this.client.create(key,item.val(), true);
	}

	@Override
	public void unregist(ServiceItem item) {
		String key = item.key();
		logger.debug("unregist service: "+key);
		this.client.delete(item.key());
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName) {
		Set<ServiceItem> sis = this.serviceItems.get(serviceName);
		if(sis == null || sis.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		Set<ServiceItem> set = new HashSet<ServiceItem>();
		for(ServiceItem si : sis) {
			set.add(si);
		}
		return set;
	}

	@Override
	public boolean isExist(String serviceName) {
		for(String key : serviceItems.keySet()) {
			if(key.startsWith(serviceName)) {
				return true;
			}
		}
		return false;
	}
	
}
