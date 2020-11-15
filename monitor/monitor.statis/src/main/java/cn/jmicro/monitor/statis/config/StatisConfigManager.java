package cn.jmicro.monitor.statis.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.mng.JmicroInstanceManager;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.monitor.StatisIndex;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class StatisConfigManager {

	public static final String STATIS_WARNING_ROOT = Config.BASE_DIR + "/statisConfigs";

	private final Logger logger = LoggerFactory.getLogger(StatisConfigManager.class);
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvMng;
	
	@Inject
	private JmicroInstanceManager insManager;
	
	private Map<Integer,StatisConfig> configs = new HashMap<>();
	
	private Map<String,Set<Integer>> srvName2Configs = new HashMap<>();
	//方便根据实例名取配置
	//private Map<String,Set<Integer>> ins2Configs = new HashMap<>();
	
	//一个服务方法可以被不同配置匹配
	private Map<String,Set<Integer>> srvMethod2Configs = new HashMap<>();
	
	//一个配置可以匹配一个服务方法，并且可以匹配不同运行实例的同一个服务方法
	private Map<Integer,Set<String>> config2SmKeys = new HashMap<>();
	
	private Map<String,Set<Integer>> instance2Configs = new HashMap<>();
	
	//private Map<String,Set<Integer>> account2Configs = new HashMap<>();
	
	private RaftNodeDataListener<StatisConfig> configListener;
	
	private IServiceListener snvListener = (type,item)->{
		if(type == IListener.REMOVE) {
			onServiceRemove(item);
		}else if(type == IListener.ADD) {
			onServiceAdd(item);
		}
	};
	
	/*private IInstanceListener insListener = (type, pi)->{
		if(type == IListener.REMOVE) {
			if(ins2Configs.containsKey(pi.getInstanceName())) {
				
			}
		}else if(type == IListener.ADD) {
			
		}
	};*/
	
	public void ready() {
		configListener = new RaftNodeDataListener<>(op,STATIS_WARNING_ROOT,StatisConfig.class,false);
		configListener.addListener(lis);
		srvMng.addListener(snvListener);
		//insManager.addInstanceListner(insListener);
	}
	
	private void dataChanged(Integer id, StatisConfig lw) {
		if(lw == null) {
			return;
		}
		
		if(!lw.isEnable()) {
			StatisConfig olw = configs.get(id);
			if(olw != null) {
				//禁用配置
				parseConfigData(olw,0);
				configs.remove(id);
				
				/*
				Set<Integer> cs = ins2Configs.get(olw.getByins());
				if(cs != null && !cs.isEmpty()) {
					cs.remove(olw.getId());
				}
				ins2Configs.remove(olw.getByins());
				*/
				
				removeService2Config(lw);
				notifyStatisConfig(IListener.REMOVE,olw);
			}
			return;
		}
		
		if(configs.containsKey(id)) {
			//启用中的配置不可能改数据，所以认为是重复的通知
			return;
		}
		
		Set<Short> set = null;
		if(!Utils.isEmpty(lw.getNamedType())) {
			String p = Config.NamedTypesDir+"/"+lw.getNamedType();
			set = this.getTypeByKey(p);
		}else {
			set = new HashSet<>();
		}
		
		lw.setTypes(set);
		
		for(StatisIndex si : lw.getStatisIndexs()) {
			set.addAll(Arrays.asList(si.getNums()));
			if(si.getDens() != null && si.getDens().length > 0) {
				set.addAll(Arrays.asList(si.getDens()));
			}
		}
		
		if(!Utils.isEmpty(lw.getExpStr())) {
			List<String> suffixExp = ExpUtils.toSuffix(lw.getExpStr());
			if(!ExpUtils.isValid(suffixExp)) {
				logger.error("Invalid exp: " + id + "---> " + lw.getExpStr());
				return;
			}
			Exp exp = new Exp();
			exp.setSuffix(suffixExp);
			exp.setOriEx(lw.getExpStr());
			lw.setExp(exp);
		}
		
		//已经存在的配置从禁用到启用
		configs.put(lw.getId(), lw);
		
		/*
		Set<Integer> cs = ins2Configs.get(lw.getByins());
		if(cs == null) {
			cs = new HashSet<>();
			ins2Configs.put(lw.getByins(), cs);
		}
		cs.add(lw.getId());
		*/
		
		parseConfigData(lw,1);
		
		addService2Config(lw);
		
		if(lw.isEnable()) {
			updateMonitorAttr(lw,1);
		}else {
			updateMonitorAttr(lw,0);
		}
		
		notifyStatisConfig(IListener.ADD,lw);
		return;
	}
	
	private void addService2Config(StatisConfig lw) {
		switch(lw.getByType()) {
		case StatisConfig.BY_TYPE_SERVICE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			if(!srvName2Configs.containsKey(lw.getBysn())) {
				srvName2Configs.put(lw.getBysn(), new HashSet<>());
			}
			srvName2Configs.get(lw.getBysn()).add(lw.getId());
		}
	}

	private void removeService2Config(StatisConfig lw) {
		switch(lw.getByType()) {
		case StatisConfig.BY_TYPE_SERVICE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			if(srvName2Configs.containsKey(lw.getBysn())) {
				Set<Integer> ids = srvName2Configs.get(lw.getBysn());
				if(ids != null) {
					ids.remove(lw.getId());
					if(ids.isEmpty()) {
						srvName2Configs.remove(lw.getBysn());
					}
				}
			}
		}
	}

	private IRaftListener<StatisConfig> lis = new IRaftListener<StatisConfig>() {
		public void onEvent(int type,String id, StatisConfig lw) {
			Integer cid = Integer.parseInt(id);
			if(type == IListener.DATA_CHANGE || type == IListener.ADD) {
				dataChanged(cid,lw);
			}
		}
	};
	
	private void parseConfigData(StatisConfig lw,int enable) {
		int tt = lw.getByType();
		switch(tt) {
			case StatisConfig.BY_TYPE_SERVICE_METHOD:
			case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
			case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
				 String[] srvs = lw.getByKey().split(UniqueServiceKey.SEP);
				 lw.setBysn(srvs[0]);
				 lw.setByns(srvs[1]);
				 lw.setByver(srvs[2]);
				 lw.setByins(srvs[3]);
				 lw.setByme(srvs[6]);
				 parseServiceData(lw,enable);
				 break;
			case StatisConfig.BY_TYPE_INSTANCE:
				if(enable == 1) {
					if(!instance2Configs.containsKey(lw.getByins())) {
						instance2Configs.put(lw.getByins(), new HashSet<>());
					}
					instance2Configs.get(lw.getByins()).add(lw.getId());
				}else {
					if(instance2Configs.containsKey(lw.getByins())) {
						instance2Configs.get(lw.getByins()).remove(lw.getId());
					}
				}
				break;
			case StatisConfig.BY_TYPE_ACCOUNT:
				
				break;
		}
	}
	
	private void parseServiceData(StatisConfig lw, int enable) {
		
		if(enable == 1) {
			Set<ServiceItem> items = reg.getServices(lw.getBysn(), lw.getByns(), lw.getByver());
			if(items == null || items.isEmpty()) {
				return;
			}
			
			for(ServiceItem si : items) {
				ServiceMethod sm = si.getMethod(lw.getByme());
				if(sm != null) {
					String smKey = smKey(sm,lw);
					addServiceMethod2Config(smKey,lw.getId());
				}
			}
		} else {
			removeServiceMethod2Config(lw.getId());
		}
		
	}

	private Set<Short> getTypeByKey(String path) {
		Set<Short> l = new HashSet<>();
		if(op.exist(path)) {
			String data = op.getData(path);
			if(StringUtils.isNotEmpty(data)) {
				String[] ts = data.split(",");
				for(String t : ts) {
					Short type = Short.parseShort(t);
					l.add(type);
				}
			}		
		}
		return l;
	}
	
	private void onServiceAdd(ServiceItem si) {
		Set<Integer> cfgIds = srvName2Configs.get(si.getKey().getServiceName());
		if(cfgIds == null || cfgIds.isEmpty()) {
			return;
		}
		
		for(Integer cid : cfgIds) {
			StatisConfig sc = this.configs.get(cid);
			
			ServiceMethod sm = si.getMethod(sc.getByme());
			if(sm == null) {
				continue;
			}
			
			if(sc.getByType() == StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD) {
				if(!sm.getKey().getInstanceName().equals(sc.getByins())) {
					continue;
				}
			}
			
			String smKey = smKey(sm,sc);
			
			addServiceMethod2Config(smKey,sc.getId());
			
			srvMng.setMonitorable(sm,1);
		}
	}
	
	private void onServiceRemove(ServiceItem item) {
		
		Set<Integer> cfgIds = srvName2Configs.get(item.getKey().getServiceName());
		if(cfgIds == null || cfgIds.isEmpty()) {
			return;
		}

		Set<Integer> delConfigs = new HashSet<>();
		synchronized(srvName2Configs) {
			delConfigs.addAll(cfgIds);
		}
		
		for(Integer cid : delConfigs) {
			Set<String> keys = config2SmKeys.get(cid);
			if(keys != null) {
				StatisConfig sc = this.configs.get(cid);
				ServiceMethod sm = item.getMethod(sc.getByme());
				String smKey = this.smKey(sm, sc);
				if(keys.contains(smKey)) {
					srvMethod2Configs.get(smKey).remove(cid);
					keys.remove(smKey);
				}
			}
		}
	}
	
	private void addServiceMethod2Config(String smKey,Integer cid) {
		if(!srvMethod2Configs.containsKey(smKey)) {
			srvMethod2Configs.put(smKey, new HashSet<>());
		}
		srvMethod2Configs.get(smKey).add(cid);
		
		if(!config2SmKeys.containsKey(cid)) {
			config2SmKeys.put(cid, new HashSet<>());
		}
		config2SmKeys.get(cid).add(smKey);
	}
	
	private void removeServiceMethod2Config(int cid) {
		Set<String> smKeys = config2SmKeys.get(cid);
		if(smKeys != null && !smKeys.isEmpty()) {
			for(String skey : smKeys) {
				if(srvMethod2Configs.containsKey(skey)) {
					srvMethod2Configs.get(skey).remove(cid);
				}
			}
		}
		config2SmKeys.remove(cid);
	}
	
	private String smKey(ServiceMethod sm,StatisConfig sc) {
		String smKey = sm.getKey().toKey(true, true, true);
		if(sc.getByType() == StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD) {
			smKey += UniqueServiceKey.SEP + sc.getActName();
		}else {
			smKey += UniqueServiceKey.SEP;
		}
		return smKey;
	}

	private void updateMonitorAttr(StatisConfig lw,int enable) {

		int tt = lw.getByType();
		
		switch(tt) {
			case StatisConfig.BY_TYPE_SERVICE_METHOD:
			case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
			case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
				 setServiceMethodMonitorable(lw,enable);
				 break;
			case StatisConfig.BY_TYPE_INSTANCE:
				setProcessInfoMonitorable(lw,enable);
		}
	}

	private void setProcessInfoMonitorable(StatisConfig lw, int enable) {
		ProcessInfo pi = insManager.getProcessByName(lw.getByins());
		boolean e = enable == 1?true:false;
		if(pi != null && (pi.isMonitorable() != e)) {
			pi.setMonitorable(e);
		}
		op.setData(ChoyConstants.INS_ROOT+"/"+pi.getId(), JsonUtils.getIns().toJson(pi));
	}

	private void setServiceMethodMonitorable(StatisConfig lw,int enable) {
		Set<ServiceItem> items = reg.getServices(lw.getBysn(), lw.getByns(), lw.getByver());
		if(items == null || items.isEmpty()) {
			return;
		}
		for(ServiceItem si : items) {
			ServiceMethod sm = si.getMethod(lw.getByme());
			if(sm == null) {
				continue;
			}
			
			if(lw.getByType() == StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD) {
				if(!sm.getKey().getInstanceName().equals(lw.getByins())) {
					continue;
				}
			}
			
			if(enable != sm.getMonitorEnable()) {
				srvMng.setMonitorable(sm,enable);
			}
		}
	}
	
	void addStatisConfigListener(IStatisConfigListener l) {
		if(scListener == null) {
			scListener = new HashSet<>();
		}
		
		if(!scListener.contains(l)) {
			scListener.add(l);
		}
	}
	
	void removeStatisConfigListener(IStatisConfigListener l) {
		if(scListener == null) {
			return;
		}
		
		if(scListener.contains(l)) {
			scListener.remove(l);
		}
	}
	
	private Set<IStatisConfigListener> scListener = null;
	
	private void notifyStatisConfig(int event,StatisConfig sc) {
		if(scListener == null) {
			return;
		}
		
		for(IStatisConfigListener l : this.scListener) {
			l.onEvent(event, sc);
		}
	}

	interface IStatisConfigListener extends IListener {
		void onEvent(int event,StatisConfig sc);
	}
	
	
	@SuppressWarnings("unchecked")
	Set<Integer> getSrvConfigs(String smKey) {
		if(srvMethod2Configs.containsKey(smKey)) {
			Collections.unmodifiableSet(srvMethod2Configs.get(smKey));
		}
		return Collections.EMPTY_SET;
	}
	
	@SuppressWarnings("unchecked")
	Set<Integer> getInstanceConfigs(String insName) {
		if(this.instance2Configs.containsKey(insName)) {
			Collections.unmodifiableSet(instance2Configs.get(insName));
		}
		return Collections.EMPTY_SET;
	}
	
	StatisConfig getConfigs(Integer cid) {
		return this.configs.get(cid);
	}
	
	@SuppressWarnings("unused")
	private boolean computeByExpression(StatisConfig cfg ,ServiceMethod sm) {
		
		Map<String,Object> cxt = new HashMap<>();
		
		Exp exp = cfg.getExp();
		if(sm != null) {
			cxt.put(Constants.SERVICE_NAME_KEY, sm.getKey().getServiceName());
			cxt.put(Constants.SERVICE_NAMESPACE_KEY, sm.getKey().getNamespace());
			cxt.put(Constants.SERVICE_VERSION_KEY, sm.getKey().getVersion());
			cxt.put(Constants.SERVICE_METHOD_KEY, sm.getKey().getMethod());
			cxt.put(JMicroContext.REMOTE_HOST, sm.getKey().getHost());
			cxt.put(JMicroContext.REMOTE_PORT, sm.getKey().getPort());
			cxt.put(Constants.INSTANCE_NAME, sm.getKey().getInstanceName());
		}
		
		cxt.put(JMicroContext.LOCAL_HOST, Config.getExportSocketHost());
		//cxt.put(JMicroContext.LOCAL_PORT, Config.getInstanceName());
		cxt.put(Constants.LOCAL_INSTANCE_NAME, Config.getInstanceName());
		
		return ExpUtils.compute(exp, cxt, Boolean.class);
		
	}
}
