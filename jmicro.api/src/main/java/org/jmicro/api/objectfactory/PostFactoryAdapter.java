package org.jmicro.api.objectfactory;

public class PostFactoryAdapter implements IPostFactoryListener {

	@Override
	public void preInit(IObjectFactory of) {
	}

	@Override
	public void afterInit(IObjectFactory of) {
	}

	@Override
	public int runLevel() {
		return Integer.MAX_VALUE;
	}

}
