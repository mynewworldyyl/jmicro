package cn.jmicro.api.objectfactory;

import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;

public abstract class AbstractClientServiceProxyHolder implements IServiceListener{

	protected ClientServiceProxyHolder proxyHolder = new ClientServiceProxyHolder();

	public ClientServiceProxyHolder getHolder() {
		return proxyHolder;
	}

	public void setHolder(ClientServiceProxyHolder holder) {
		this.proxyHolder = holder;
	}

	@Override
	public void serviceChanged(int type, UniqueServiceKeyJRso siKey,ServiceItemJRso si) {
		proxyHolder.serviceChanged(type, siKey,si);
	}
	
	public boolean isReady() {
		return proxyHolder.isUsable();
	}
	
	public int clientId() {
		return proxyHolder.getItem() == null?-1:proxyHolder.getItem().getClientId();
	}
	
	public ServiceItemJRso getItem() {
		return proxyHolder.getItem();
	}
	
	public int getInsId() {
		return proxyHolder.getInsId();
	}

	/*public <T> T[] convertArray(Class<T> targetType, Object[] arrayObjects) {
        if (targetType == null) {
            return (T[]) arrayObjects;
        }
        if (arrayObjects == null) {
            return null;
        }
        T[] targetArray = (T[]) Array.newInstance(targetType, arrayObjects.length);
        try {
            System.arraycopy(arrayObjects, 0, targetArray, 0, arrayObjects.length);
        } catch (ArrayStoreException e) {
        	e.printStackTrace();
        }
        return targetArray;
    }*/

	
}
