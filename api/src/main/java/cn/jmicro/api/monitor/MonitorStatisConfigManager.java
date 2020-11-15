package cn.jmicro.api.monitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.mng.JmicroInstanceManager;
import cn.jmicro.api.mng.JmicroInstanceManager.IInstanceListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

@Component
public class MonitorStatisConfigManager {

	public static final String STATIS_WARNING_ROOT = Config.BASE_DIR + "/statisConfigs";

	private final Logger logger = LoggerFactory.getLogger(MonitorStatisConfigManager.class);
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ProcessInfo pi;
	
	@Inject
	private ServiceManager srvMng;
	
	@Inject
	private JmicroInstanceManager insManager;
	
	private boolean lazyParseConfig = true;
	
	private Map<Integer,StatisConfig> configs = new HashMap<>();
	
	private Map<String, Map<Short,Integer>> sm2Types2ConfigNum = new HashMap<>();
	
	private Map<String, Map<Short,Integer>> ins2Types2ConfigNum = new HashMap<>();
	
	private RaftNodeDataListener<StatisConfig> configListener;
	
	//private Map<String,Integer> srvListenerNum = new HashMap<>();
	private IServiceListener snvListener = (type,item)->{
		if(type == IListener.REMOVE) {
			onServiceRemove(item);
		}
	};
	
	private IInstanceListener insListener = (type, processInfo)->{
		if(type == IListener.REMOVE) {
			if(ins2Types2ConfigNum.containsKey(processInfo.getInstanceName())) {
				synchronized(ins2Types2ConfigNum) {
					ins2Types2ConfigNum.remove(processInfo.getInstanceName());
				}
			}
		}
	};
	
	private IRaftListener<StatisConfig> lis = new IRaftListener<StatisConfig>() {
		public void onEvent(int type,String id, StatisConfig lw) {
			 if(type == IListener.DATA_CHANGE | type == IListener.ADD) {
				dataChanged(lw);
			}
		}
	};
	
	public void ready() {
		//op.addChildrenListener(STATIS_WARNING_ROOT, lis);
		configListener = new RaftNodeDataListener<>(op,STATIS_WARNING_ROOT,StatisConfig.class,false);
		configListener.addListener(lis);
		srvMng.addListener(snvListener);
		insManager.addInstanceListner(insListener);
	}
	
	private void onServiceRemove(ServiceItem item) {
		Set<String> smKeys = new HashSet<>();
		synchronized(sm2Types2ConfigNum) {
			smKeys.addAll(sm2Types2ConfigNum.keySet());
		}
		//服务实例下线，对应的全部服务下的服务方法都要删除，下次上线并被调用时时再重新解析配置
		String key = item.getKey().toKey(true, true, true);
		for(String smKey : smKeys) {
			if(smKey.startsWith(key)) {
				synchronized(sm2Types2ConfigNum) {
					sm2Types2ConfigNum.remove(smKey);
				}
			}
		}
	}

	//依据服务7要素做初步判断是否需要提交数据到监控服务器
	//服务7要素：服务名称，名称空间，版本，实例名，IP，端口，方法名
	public boolean canSubmit(ServiceMethod sm , Short t, String actName) {
		if(sm == null) {
			if(!ins2Types2ConfigNum.containsKey(pi.getInstanceName()) && this.lazyParseConfig) {
				parseStatisConfigByInstaneName(pi.getInstanceName());
			}
			
			return ins2Types2ConfigNum.containsKey(pi.getInstanceName()) &&
					ins2Types2ConfigNum.get(pi.getInstanceName()).containsKey(t);
		} else {
			String smKey = sm.getKey().toKey(true, true, true);
			String withoutAct = smKey + UniqueServiceKey.SEP;
			
			if(!sm2Types2ConfigNum.containsKey(withoutAct) && this.lazyParseConfig) {
				parseStatisConfigByServiceMethod(sm);
			}
			
			if(sm2Types2ConfigNum.containsKey(withoutAct) 
					&& sm2Types2ConfigNum.get(withoutAct).containsKey(t)) {
				return true;
			}
			
			String withAct = smKey + UniqueServiceKey.SEP + actName;
			if(!sm2Types2ConfigNum.containsKey(withAct) && this.lazyParseConfig) {
				parseStatisConfigByServiceMethod(sm);
			}
			
			if(sm2Types2ConfigNum.containsKey(withAct) 
					&& sm2Types2ConfigNum.get(withAct).containsKey(t)) {
				return true;
			}
			
			ProcessInfo smPi = this.insManager.getProcessByName(sm.getKey().getInstanceName());
			
			if(!ins2Types2ConfigNum.containsKey(sm.getKey().getInstanceName()) && this.lazyParseConfig) {
				parseStatisConfigByInstaneName(sm.getKey().getInstanceName());
			}
			
			if(smPi != null && ins2Types2ConfigNum.containsKey(sm.getKey().getInstanceName())) {
				return ins2Types2ConfigNum.get(sm.getKey().getInstanceName()).containsKey(t);
			}
			
			return false;
		}
	}

	private void parseStatisConfigByInstaneName(String instanceName) {
		Set<StatisConfig> cons = copyConfigs();
		for(StatisConfig c : cons) {
			if(c.getByType() == StatisConfig.BY_TYPE_INSTANCE 
					&& instanceName.equals(c.getByins())) {
				parseInstanceConfigData(c,1);
			}
		}
	}

	private void parseStatisConfigByServiceMethod(ServiceMethod sm) {
		Set<StatisConfig> cons = copyConfigs();
		for(StatisConfig c : cons) {
			switch(c.getByType()) {
			case StatisConfig.BY_TYPE_SERVICE_METHOD:
			case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
			case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
				if(sm.getKey().getServiceName().equals(c.getBysn()) 
						&& UniqueServiceKey.matchNamespace(c.getByns(), sm.getKey().getNamespace())
						&& UniqueServiceKey.matchVersion(c.getByver(), sm.getKey().getVersion())) {
					parseServiceConfigData(c,1);
					
				}
			}
		}
	}
	
	private Set<StatisConfig> copyConfigs() {
		Set<StatisConfig> cons = new HashSet<>();
		synchronized(configs) {
			cons.addAll(configs.values());
		}
		return cons;
	}

	private void dataChanged(StatisConfig lw) {
		if(lw == null) {
			return;
		}
		
		initStatisConfig(lw);
		
		StatisConfig olw = null;
		synchronized(configs) {
			olw = configs.get(lw.getId());
		}
		
		if(!lw.isEnable()) {
			if(olw != null && olw.isEnable()) {
				//禁用配置
				parseStatisConfigData(lw,0);
			}
			if(olw != null) {
				synchronized(configs) {
					configs.remove(lw.getId());
				}
			}
			return;
		}
		
		switch(lw.getByType()) {
		case StatisConfig.BY_TYPE_SERVICE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			 String[] srvs = lw.getByKey().split(UniqueServiceKey.SEP);
			 lw.setBysn(srvs[0]);
			 lw.setByns(srvs[1]);
			 lw.setByver(srvs[2]);
			 lw.setByins(srvs[3]);
			 lw.setByme(srvs[6]);
		}
		 
		synchronized (configs) {
			configs.put(lw.getId(), lw);
		}
		//已经存在的配置从禁用到启用
		if(!lazyParseConfig) {
			parseStatisConfigData(lw,1);
		}
		return;
	
	}
	
	
	private void parseStatisConfigData(StatisConfig lw, int enable) {
		switch(lw.getByType()) {
		case StatisConfig.BY_TYPE_SERVICE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			 String[] srvs = lw.getByKey().split(UniqueServiceKey.SEP);
			 lw.setBysn(srvs[0]);
			 lw.setByns(srvs[1]);
			 lw.setByver(srvs[2]);
			 lw.setByins(srvs[3]);
			 lw.setByme(srvs[6]);
			 parseServiceConfigData(lw,enable);
			 break;
		case StatisConfig.BY_TYPE_INSTANCE:
			parseInstanceConfigData(lw,enable);
		}
	}

	private void parseInstanceConfigData(StatisConfig lw, int enable) {
		String insName = lw.getByins();
		parseType2ConfigNum(this.ins2Types2ConfigNum,lw,insName,enable);
	}

	private void parseServiceConfigData(StatisConfig lw,int enable) {
		
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
			
			if(lw.getByType() == StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD) {
				if(!sm.getKey().getInstanceName().equals(lw.getByins())) {
					continue;
				}
			}
			
			String smKey = sm.getKey().toKey(true, true, true);
			
			if(lw.getByType() == StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD) {
				smKey += "##" + lw.getActName(); 
			} else {
				smKey += "##" ; 
			}
			
			parseType2ConfigNum(this.sm2Types2ConfigNum,lw,smKey,enable);
			
			/*if(enable == 0) {
				this.removeServiceListener(sm.getKey().getServiceName(),
						sm.getKey().getNamespace(),sm.getKey().getVersion());
			} else {
				this.addServiceListener(sm.getKey().getServiceName(),
						sm.getKey().getNamespace(),sm.getKey().getVersion());
			}*/
		}
	}

	/*private void addServiceListener(String sn,String ns,String ver) {
		String key = UniqueServiceKey.serviceName(sn, ns, ver);
		if(srvListenerNum.containsKey(key) && srvListenerNum.get(key) > 0) {
			srvListenerNum.put(key, srvListenerNum.get(key)+1);
		}else {
			srvListenerNum.put(key, 1);
			this.srvMng.addServiceListener(key, snvListener);
		}
	}

	private void removeServiceListener(String sn,String ns,String ver) {
		String key = UniqueServiceKey.serviceName(sn, ns, ver);
		if(srvListenerNum.containsKey(key)) {
			int curNum = srvListenerNum.get(key);
			if(curNum <= 1) {
				srvListenerNum.remove(key);
				this.srvMng.removeServiceListener(key, snvListener);
			} else {
				srvListenerNum.put(key, curNum-1);
			}
		}
	}*/
	
	private void parseType2ConfigNum(Map<String, Map<Short,Integer>> key2Type2ConfigNum,StatisConfig lw
			,String key,int enable) {
		Map<Short,Integer> type2ConfigNum = key2Type2ConfigNum.get(key);
		
		if(enable == 1) {
			//启用
			if(type2ConfigNum == null) {
				type2ConfigNum = new HashMap<>();
				synchronized(key2Type2ConfigNum) {
					key2Type2ConfigNum.put(key, type2ConfigNum);
				}
			}
			
			for(Short t : lw.getTypes()) {
				if(type2ConfigNum.containsKey(t)) {
					//对应的类型配置计数减1
					type2ConfigNum.put(t, type2ConfigNum.get(t)+1);
				}else {
					type2ConfigNum.put(t, 1);
				}
			}
		} else if(enable == 0) {
			//禁用
			if(type2ConfigNum != null) {
				for(Short t : lw.getTypes()) {
					if(type2ConfigNum.containsKey(t)) {
						if(type2ConfigNum.get(t) <= 1) {
							//小于或等于0，表示此服务方法不需要再监听此类型事件
							type2ConfigNum.remove(t);
							/*if(type2ConfigNum.isEmpty()) {
								key2Type2ConfigNum.remove(key);
							}*/
						} else {
							//对应的类型配置计数减1
							type2ConfigNum.put(t, type2ConfigNum.get(t)-1);
						}
					}
				}
			}
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
	
	
	private void initStatisConfig(StatisConfig lw) {
		if(!lw.isEnable()) {
			return ;
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
				logger.error("Invalid exp: " + lw.getId() + "---> " + lw.getExpStr());
				return;
			}
			Exp exp = new Exp();
			exp.setSuffix(suffixExp);
			exp.setOriEx(lw.getExpStr());
			lw.setExp(exp);
		}
	}
}
