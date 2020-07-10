package cn.jmicro.api.objectfactory;

import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;

public abstract class AbstractClientServiceProxyHolder implements IServiceListener{

	protected ClientServiceProxyHolder proxyHolder = new ClientServiceProxyHolder();

	public ClientServiceProxyHolder getHolder() {
		return proxyHolder;
	}

	public void setHolder(ClientServiceProxyHolder holder) {
		this.proxyHolder = holder;
	}

	@Override
	public void serviceChanged(int type, ServiceItem item) {
		proxyHolder.serviceChanged(type, item);
	}
	
	
}
