package org.jmicro.classloader;

import java.util.HashSet;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.classloader.IClassloaderRpc;
import org.jmicro.api.classloader.RpcClassLoader;
import org.jmicro.api.objectfactory.IFactoryListener;
import org.jmicro.api.objectfactory.IObjectFactory;

@Component(level=50000)
public class RpcClassloaderClient implements IFactoryListener{

	@Reference
	private Set<IClassloaderRpc> classLoaderServers = new HashSet<>();
	
	private RpcClassLoader cl = null;
	
	@Override
	public void preInit(IObjectFactory of) {
		ClassLoader parent = RpcClassloaderClient.class.getClassLoader();
		/*if(parent == null) {
			parent = RpcClassloaderClient.class.getClassLoader();
		}*/
		cl = new RpcClassLoader(parent,this.classLoaderServers);
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
