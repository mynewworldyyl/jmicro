package org.jmicro.api.idgenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.IDStrategy;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.config.Config;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ComponentIdServer /*implements IIdClient,IIdServer*/{

	private final static Logger logger = LoggerFactory.getLogger(ComponentIdServer.class);
	
	private static final int DEFAULT_CATCHE_SIZE = 100;
	
	private static final String ID_CONFIG_KEY_PREFIX = "/ComponentIdServer/idgenerator-";
	
	/**
	 * 非ID服务本身获取ID
	 */
	@Inject(value="idClient",required = false)
	private IdClient idClient;
	
	/**
	 * id服务本身要获取ID，直接通过IIdServer服务本向获取
	 */
	@Inject(value = Constants.DEFAULT_IDGENERATOR, required = false)
	private IIdServer idServer;
	
	/**
	 * idgenerator-打头的Key都放到此配置中,*代表ID类的全名
	 */
	@Cfg(value=ID_CONFIG_KEY_PREFIX+"*", defGlobal=true)
	private Map<String,Integer> cacheCofig = new HashMap<>();
	
	@Inject
	private Config config;
	
	private Map<String,Queue<Long>> longIdsCache = new HashMap<>();
	private Map<String,Queue<Integer>> intIdsCache = new HashMap<>();
	private Map<String,Queue<String>> strIdsCache = new HashMap<>();
	
	private boolean isNotIdServer = false;

	public void init() {
		if(idClient == null && idServer == null) {
			throw new CommonException("IIdClient and IIdServer is NULL");
		}
		isNotIdServer = idServer == null;
	}
	
	public String[] getStringIds(String idKey, int num) {
		if(isNotIdServer) {
			return idClient.getStringIds(idKey, num);
		}else {
			return idServer.getStringIds(idKey, num);
		}
	}

	public Long[] getLongIds(String idKey, int num) {
		if(isNotIdServer) {
			return idClient.getLongIds(idKey, num);
		}else {
			return idServer.getLongIds(idKey, num);
		}
	}

	public Integer[] getIntIds(String idKey, int num) {
		if(isNotIdServer) {
			return idClient.getIntIds(idKey, num);
		}else {
			return idServer.getIntIds(idKey, num);
		}
	}
	
	public Long getLongId(Class<?> idCls) {
		
		Queue<Long> ids = longIdsCache.get(idCls.getName());
		if(ids != null && !ids.isEmpty()) {
			Long id = ids.poll();
			if(id != null) {
				return id;
			}
		}
		
		synchronized(longIdsCache) {
			
			ids = longIdsCache.get(idCls.getName());
			
			if(ids == null) {
				ids = new ConcurrentLinkedQueue<Long>();
				longIdsCache.put(idCls.getName(), ids);
			}
			
			if(ids.isEmpty()) {
				Long[] reqIds = this.getLongIds(idCls.getName(), getCacheSize(idCls));
				ids.addAll(Arrays.asList(reqIds));
			}
			return ids.poll();
		}
		
		
	}

	public String getStringId(Class<?> idCls) {
		Queue<String> ids = strIdsCache.get(idCls.getName());
		if(ids != null && !ids.isEmpty()) {
			String id = ids.poll();
			if(id != null) {
				return id;
			}
		}
		
		synchronized(strIdsCache) {
				
			ids = strIdsCache.get(idCls.getName());
			if(ids == null) {
				ids = new ConcurrentLinkedQueue<String>();
				strIdsCache.put(idCls.getName(), ids);
			}
			
			if(ids.isEmpty()) {
				String[] reqIds = this.getStringIds(idCls.getName(), getCacheSize(idCls));
				ids.addAll(Arrays.asList(reqIds));
			}
			}
		
		return ids.poll();
	}

	public Integer getIntId(Class<?> idCls) {
		Queue<Integer> ids = intIdsCache.get(idCls.getName());
		if(ids != null && !ids.isEmpty()) {
			Integer id = ids.poll();
			if(id != null) {
				return id;
			}
		}
		
		synchronized(intIdsCache) {
			
			ids = intIdsCache.get(idCls.getName());
			
			if(ids == null) {
				ids = new ConcurrentLinkedQueue<Integer>();
				intIdsCache.put(idCls.getName(), ids);
			}
			
			if(ids.isEmpty()) {
				Integer[] reqIds = this.getIntIds(idCls.getName(), getCacheSize(idCls));
				ids.addAll(Arrays.asList(reqIds));
			}
			return ids.poll();
		}
		
		
	}

	private int getCacheSize(Class<?> idCls) {
		String p = ID_CONFIG_KEY_PREFIX + idCls.getName();
		if(cacheCofig.containsKey(p)) {
			return cacheCofig.get(p);
		}
		
		int size = 0;
		if(idCls.isAnnotationPresent(IDStrategy.class)) {
			IDStrategy ids = idCls.getAnnotation(IDStrategy.class);
			size = ids.value();
			if(size <= 0) {
				logger.error("IDStragety config size error:{},size:{},set to default:{}",
						idCls.getName(),size,DEFAULT_CATCHE_SIZE);
			}
		}
		
		if(size <= 0) {
		    size = DEFAULT_CATCHE_SIZE;
		}
		
		cacheCofig.put(p, size);
		config.createConfig(size+"", p, true);
		return size;
	}
	
}
