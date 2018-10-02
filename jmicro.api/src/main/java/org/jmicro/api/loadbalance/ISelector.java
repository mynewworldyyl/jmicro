package org.jmicro.api.loadbalance;

import org.jmicro.api.registry.ServiceItem;

public interface ISelector {

	ServiceItem getService(String srvName);
}
