package org.jmicro.api.monitor.v2;

import java.util.Map;

import org.jmicro.api.registry.UniqueServiceKey;

public abstract class AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	protected String skey(String namespace,String version) {
		return UniqueServiceKey.serviceName(IMonitorDataSubscriber.class.getName(), namespace,
				version).toString();
	}

	@Override
	public Map<Short,Double>  getData(String srvKey, Short[] type) {
		return null;
	}

	
}
