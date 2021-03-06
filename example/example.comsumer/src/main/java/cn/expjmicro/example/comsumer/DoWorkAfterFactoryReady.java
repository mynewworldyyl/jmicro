package cn.expjmicro.example.comsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;

@Component(active=true)
public class DoWorkAfterFactoryReady implements IPostFactoryListener {

	private final static Logger logger = LoggerFactory.getLogger(DoWorkAfterFactoryReady.class);
	
	@Override
	public void preInit(IObjectFactory of) {
		logger.info("preInit");
	}

	@Override
	public void afterInit(IObjectFactory of) {
		logger.info("afterInit");
		for(int i = 0; i < 1;i++){
			new Thread(new Worker(of,i)).start();
		}
	}

	@Override
	public int runLevel() {
		return 1000;
	}

}
