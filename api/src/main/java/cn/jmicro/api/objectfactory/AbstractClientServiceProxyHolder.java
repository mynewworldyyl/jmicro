package cn.jmicro.api.objectfactory;

import java.lang.reflect.Array;

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
	

	public <T> T[] convertArray(Class<T> targetType, Object[] arrayObjects) {
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
    }

	
}
