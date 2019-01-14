package org.jmicro.classloader;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.classloader.RpcClassLoader;
import org.jmicro.api.objectfactory.IFactoryListener;
import org.jmicro.api.objectfactory.IObjectFactory;

@Component(level=50000)
public class RpcClassloaderClient implements IFactoryListener{

	private RpcClassLoader cl = null;
	
	@Override
	public void preInit(IObjectFactory of) {
		ClassLoader parent = RpcClassloaderClient.class.getClassLoader();
		/*if(parent == null) {
			parent = RpcClassloaderClient.class.getClassLoader();
		}*/
		cl = new RpcClassLoader(parent);
		of.regist(RpcClassLoader.class, cl);
		
	}

	@Override
	public void afterInit(IObjectFactory of) {
	}

	@Override
	public int runLevel() {
		return 10000;
	}

	public void init() {
		
	}

	public RpcClassLoader getCl() {
		return cl;
	}
	
}
