package org.jmicro.api.pubsub;

import java.lang.reflect.Method;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Subscribe;
import org.jmicro.api.objectfactory.IPostFactoryListener;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceLoader;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(active=true,value="pubSubPostFactoryReadyListener")
public class PubSubPostFactoryReadyListener implements IPostFactoryListener {

	private final static Logger logger = LoggerFactory.getLogger(PubSubManager.class);
	
	@Inject
	private ServiceLoader srvLoader;
	
	@Inject
	private PubSubManager pubSubManager;
	
	@Override
	public void afterInit(IObjectFactory of) {
		/*Collection<Object> srvs = srvLoader.getServices().values();
		if(srvs == null || srvs.isEmpty()) {
			return;
		}
		for(Object s : srvs) {
			ServiceItem si = this.srvLoader.getServiceItems(s.getClass());
			loadSubscriber(s.getClass(),si);
		}*/
		//pubSubManager.init();
	}

	@Override
	public void preInit(IObjectFactory of) {
	}
	
	@Override
	public int runLevel() {
		return 1008;
	}

	private void loadSubscriber(Class<?> clazz, ServiceItem item) {
		Class<?> srvCls = ProxyObject.getTargetCls(clazz);
		Subscribe clsSubs = null;
		if(srvCls.isAnnotationPresent(Subscribe.class)) {
			clsSubs = srvCls.getAnnotation(Subscribe.class);
		}
		
		for(ServiceMethod sm : item.getMethods()) {
			if("wayd".equals(sm.getKey().getMethod()) && sm.getKey().getParamsStr().indexOf("java.lang.String") >= 0) {
				continue;
			}
			Class<?>[] paramClazzes = UniqueServiceMethodKey.paramsClazzes(sm.getKey().getParamsStr());
			try {
				Method m = srvCls.getMethod(sm.getKey().getMethod(), paramClazzes);
				if(!m.isAnnotationPresent(Subscribe.class)) {
					continue;
				}
				String topic = null;
				Subscribe mtSubs = m.getAnnotation(Subscribe.class);
				if(!StringUtils.isEmpty(mtSubs.topic())) {
					topic = mtSubs.topic();
				}else if(clsSubs != null && !StringUtils.isEmpty(mtSubs.topic())) {
					topic = mtSubs.topic();
				}
				
				if(StringUtils.isEmpty(mtSubs.topic())) {
					logger.error("No valid topic value for {}",sm.getKey().toString());
					continue;
				}
				
			/*	this.pubSubManager.subscribe(null, topic, sm.getKey().getServiceName(),
						sm.getKey().getNamespace(), sm.getKey().getVersion(), sm.getKey().getMethod());
			*/	
			} catch (NoSuchMethodException e) {
				
			}catch(SecurityException e){
				logger.error("",e);
			}
			
		}
		
	}


}
