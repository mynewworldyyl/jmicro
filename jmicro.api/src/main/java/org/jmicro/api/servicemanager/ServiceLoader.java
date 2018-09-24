package org.jmicro.api.servicemanager;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.service.Service;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.server.AbstractHandler;
import org.jmicro.api.server.IServer;
import org.jmicro.common.ClassScannerUtils;
import org.jmicro.common.Constants;
import org.jmicro.common.JMicroContext;
import org.jmicro.common.Utils;
import org.jmicro.common.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceLoader {

	private final static Logger logger = LoggerFactory.getLogger(ServiceLoader.class);
	
	private static ServiceLoader loader = new ServiceLoader();
	private ServiceLoader(){}
	public static ServiceLoader getInf(){
		return loader;
	}	
	
	private Map<String,Object> services = new ConcurrentHashMap<String,Object>();
	
	private Map<String,Class<?>> servicesAnno = new ConcurrentHashMap<String,Class<?>>();
	
	private Object getService(String name){
		if(services.containsKey(name)){
			return services.get(name);
		}else {
			synchronized(this) {
				if(!servicesAnno.containsKey(name)){
					Set<Class<?>> clses = ClassScannerUtils.getIns().loadClassesByAnno(Service.class);
					if(clses == null || clses.isEmpty()){
						return null;
					}
					Utils.getIns().setClasses(clses,this.servicesAnno);
				}
			
				if(!servicesAnno.containsKey(name)){
					throw new CommonException("Service not found","service name "+name);
				}
				return this.createService(this.servicesAnno.get(name));
			}	
		}
	}
	
	
	public boolean exportService(){
		if(!services.isEmpty()){
			return true;
		}
		synchronized(this) {
			Config cfg = JMicroContext.get().getCfg();
			Set<Class<?>> clses = ClassScannerUtils.getIns().getClassesWithAnnotation(cfg.getBasePackages(), Service.class);
			if(clses == null || clses.isEmpty()){
				logger.error("No Service found when start jmicro server");
				return false;
			}
			Utils.getIns().setClasses(clses,this.servicesAnno);
			return this.doExport(clses);

		}	
	}
	
	private boolean doExport(Set<Class<?>> clses) {
		Iterator<Class<?>> ite = clses.iterator();
		boolean flag = false;
		while(ite.hasNext()){
			Class<?> c = ite.next();
			Object srv = this.createService(c);
			if(srv == null){
				throw new CommonException("fail to export server "+c.getName());
			}
			addToServer(srv);
			registService(srv);
			String key = this.serviceName(c);
			services.put(key, srv);
			flag = true;
		}
		return flag;
	}
	
	private void addToServer(Object srv) {
		IServer server = this.getServer(srv.getClass());
		server.addHandler(new AbstractHandler(srv));
	}
	
	private void registService(Object srv) {
		String[] urls = this.getServiceUrl(srv);
		if(urls == null || urls.length == 0){
			logger.error("class "+srv.getClass().getName()+" is not service");
			return;
		}
		IRegistry registry = this.getRegistry();
		for(String url : urls){
			registry.regist(url);
		}
	}
	
	private String[] getServiceUrl(Object srv) {
		Class<?> srvCls = srv.getClass();
		if(!srvCls.isAnnotationPresent(Service.class)){
			return null;
		}
		Service srvAnno = srvCls.getAnnotation(Service.class);
		IServer server = this.getServer(srvCls);
		Class<?>[] interfaces = srvAnno.interfaces();
		if(interfaces.length ==0 ){
			throw new CommonException("service ["+srvCls.getName()+"] have to implement at least one interface.");
		}
		StringBuffer sb =new StringBuffer(server.host()).append("?");
		String[] urls = new String[interfaces.length];
		int index = 0;
		for(Class<?> in : interfaces){
			if(!in.isInstance(srv)){
				throw new CommonException("service ["+srvCls.getName()+"] not implement interface ["+in.getName()+"].");
			}
			StringBuffer u = new StringBuffer(sb.toString()).append("interface=").append(in.getName())
					.append("&imp=").append(srvCls.getName());
			urls[index] = u.toString();
		}
		return urls;
	}
	
	private IServer getServer(Class<?> srvCls){
		Service srvAnno = srvCls.getAnnotation(Service.class);
		String serverName = srvAnno.server();
		
		IServer server = ComponentManager.getCommponentManager(IServer.class).getComponent(serverName);
		if(server == null){
			throw new CommonException("server ["+serverName+"] not found for service ["+srvCls.getName()+"]");
		}
		return server;
	}
	
	
	public IRegistry getRegistry(){
		IRegistry registry = ComponentManager.getCommponentManager(IRegistry.class)
				.getComponent(Constants.REGISTRY_KEY);
		if(registry == null){
			registry = ComponentManager.getCommponentManager(IRegistry.class)
					.getComponent(Constants.DEFAULT_REGISTRY_KEY);
		}
		if(registry == null){
			throw new CommonException("Registry not found");
		}
		return registry;
	}
	
	
	public String serviceName(Class<?> c) {
		if(c.isAnnotationPresent(Service.class)){
			Service s =  c.getAnnotationsByType(Service.class)[0];
			if(s.value()!= null && !"".equals(s.value().trim())){
				return s.value();
			}
		}
		return c.getName();
	}

	private Object createService(Class class1) {
		Object srv = ComponentManager.getObjectFactory().createObject(class1);
		return srv;
	}	
	
}
