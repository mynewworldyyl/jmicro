package jorg.micro.registry.zk;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.zkclient.ZkclientZookeeperTransporter;
import org.jmicro.api.annotation.Method;
import org.jmicro.api.annotation.registry.Registry;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.Constants;
import org.jmicro.common.JMicroContext;
import org.jmicro.common.config.Config;

@Registry(Constants.DEFAULT_REGISTRY_KEY)
public class ZKRegistry implements IRegistry {

	private ZookeeperClient client = null;
	
	private Map<String,Set<ServiceItem>> serviceItems = new ConcurrentHashMap<String,Set<ServiceItem>>();
	
	@Override
	@Method("init")
	public void init() {
		Config cfg = JMicroContext.get().getCfg();
		URL url = cfg.getRegistryUrl();
		this.client = new ZkclientZookeeperTransporter().connect(url);
		this.client.addChildListener(ServiceItem.ROOT, (path,children)->{
			parseServiceItem(path,children);
		});
	}

	private void parseServiceItem(String path, List<String> children) {
		
		for(String child : children){
			String data = this.client.data(path+"/"+child);
			Set<ServiceItem> items = null;
			if(!serviceItems.containsKey(child)){
				serviceItems.put(child, new HashSet<ServiceItem>());
			}
			items = serviceItems.get(child);
			items.add(new ServiceItem(child,data));
		}
		
	}

	@Override
	public void regist(ServiceItem item) {
		this.client.create(item.key(),item.val(), true);
	}

	@Override
	public void unregist(ServiceItem item) {
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
	
}
