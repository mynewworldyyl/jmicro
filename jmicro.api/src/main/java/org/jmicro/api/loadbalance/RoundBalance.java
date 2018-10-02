package org.jmicro.api.loadbalance;

import java.util.Set;

import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Selector;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.common.Constants;

@Selector(Constants.DEFAULT_SELECTOR)
public class RoundBalance implements ISelector{

	@Inject(required=true,value=Constants.DEFAULT_REGISTRY)
	private IRegistry registry;
	
	private int next=0;
	
	@SuppressWarnings("null")
	@Override
	public ServiceItem getService(String srvName) {
		Set<ServiceItem> srvItems = registry.getServices(srvName);
		if(srvItems == null && srvItems.isEmpty()) {
			return null;
		}
		ServiceItem[] arr = new ServiceItem[srvItems.size()];
		srvItems.toArray(arr);
		int next = this.next++%arr.length;
		return arr[next];
	}
	
}
