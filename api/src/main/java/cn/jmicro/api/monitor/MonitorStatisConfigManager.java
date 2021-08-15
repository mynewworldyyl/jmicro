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
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.mng.ProcessInstanceManager.IInstanceListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

@Component
public class MonitorStatisConfigManager {

	private final Logger logger = LoggerFactory.getLogger(MonitorStatisConfigManager.class);
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ProcessInfoJRso pi;
	
	@Inject
	private ServiceManager srvMng;
	
	@Inject
	private ProcessInstanceManager insManager;
	
	private boolean lazyParseConfig = true;
	
	private Map<Integer,StatisConfigJRso> configs = new HashMap<>();
	
	private Map<String, Map<Short,Integer>> sm2Types2ConfigNum = new HashMap<>();
	
	private Map<String, Map<Short,Integer>> ins2Types2ConfigNum = new HashMap<>();
	
	private RaftNodeDataListener<StatisConfigJRso> configListener;
	
	//private Map<String,Integer> srvListenerNum = new HashMap<>();
	private IServiceListener snvListener = (type,siKey,item)->{
		if(type == IListener.REMOVE) {
			onServiceRemove(siKey,item);
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
	
	private IRaftListener<StatisConfigJRso> lis = new IRaftListener<StatisConfigJRso>() {
		public void onEvent(int type,String id, StatisConfigJRso lw) {
			if(type == IListener.DATA_CHANGE) {
				statisConfigChanged(lw);
			}else if(type == IListener.REMOVE){
				statisConfigRemove(lw);
			}else if(type == IListener.ADD) {
				statisConfigAdd(lw);
			}
		}
	};
	
	public void jready() {
		//op.addChildrenListener(STATIS_WARNING_ROOT, lis);
		if(!op.exist(StatisConfigJRso.STATIS_CONFIG_ROOT)) {
			op.createNodeOrSetData(StatisConfigJRso.STATIS_CONFIG_ROOT, "", IDataOperator.PERSISTENT);
		}
		configListener = new RaftNodeDataListener<>(op,StatisConfigJRso.STATIS_CONFIG_ROOT,StatisConfigJRso.class,false);
		configListener.addListener(lis);
		srvMng.addListener(snvListener);
		insManager.addInstanceListner(insListener);
	}
	
	private void onServiceRemove(UniqueServiceKeyJRso siKey,ServiceItemJRso si) {
		Set<String> smKeys = new HashSet<>();
		synchronized(sm2Types2ConfigNum) {
			smKeys.addAll(sm2Types2ConfigNum.keySet());
		}
		//服务实例下线，对应的全部服务下的服务方法都要删除，下次上线并被调用时时再重新解析配置
		String key = siKey.fullStringKey();
		for(String smKey : smKeys) {
			if(smKey.startsWith(key)) {
				synchronized(sm2Types2ConfigNum) {
					sm2Types2ConfigNum.remove(smKey);
				}
			}
		}
	}
	
	public StatisConfigJRso getConfig(Integer cid) {
		return this.configs.get(cid);
	}

	//依据服务7要素做初步判断是否需要提交数据到监控服务器
	//服务7要素：服务名称，名称空间，版本，实例名，IP，端口，方法名
	public boolean canSubmit(ServiceMethodJRso sm , Short t, int clientId) {
		if(sm == null) {
			if(!ins2Types2ConfigNum.containsKey(pi.getInstanceName()) && this.lazyParseConfig) {
				parseStatisConfigByInstaneName(pi.getInstanceName());
			}
			return ins2Types2ConfigNum.containsKey(pi.getInstanceName()) &&
					ins2Types2ConfigNum.get(pi.getInstanceName()).containsKey(t);
		} else {
			String smKey = sm.getKey().fullStringKey();
			String withoutAct = smKey + UniqueServiceKeyJRso.SEP;
			
			if(!sm2Types2ConfigNum.containsKey(withoutAct) && this.lazyParseConfig) {
				parseStatisConfigByServiceMethod(sm,withoutAct);
			}
			
			if(sm2Types2ConfigNum.containsKey(withoutAct) 
					&& sm2Types2ConfigNum.get(withoutAct).containsKey(t)) {
				return true;
			}
			
			String withAct = smKey + UniqueServiceKeyJRso.SEP + clientId;
			if(!sm2Types2ConfigNum.containsKey(withAct) && this.lazyParseConfig) {
				parseStatisConfigByServiceMethod(sm,withAct);
			}
			
			if(sm2Types2ConfigNum.containsKey(withAct) 
					&& sm2Types2ConfigNum.get(withAct).containsKey(t)) {
				return true;
			}
			
			ProcessInfoJRso smPi = this.insManager.getProcessByName(sm.getKey().getInstanceName());
			
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
		Set<StatisConfigJRso> cons = copyConfigs();
		for(StatisConfigJRso c : cons) {
			if(c.getByType() == StatisConfigJRso.BY_TYPE_INSTANCE 
					&& instanceName.startsWith(c.getByKey())) {
				parseInstanceConfigData(c,1,instanceName);
			}
		}
	}

	private void parseStatisConfigByServiceMethod(ServiceMethodJRso sm,String key) {
		Set<StatisConfigJRso> cons = copyConfigs();
		for(StatisConfigJRso c : cons) {
			switch(c.getByType()) {
			case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
			case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
			case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
				if(sm.getKey().getServiceName().equals(c.getBysn()) 
						&& UniqueServiceKeyJRso.matchNamespace(c.getByns(), sm.getKey().getNamespace())
						&& UniqueServiceKeyJRso.matchVersion(c.getByver(), sm.getKey().getVersion())) {
					parseServiceConfigData(c,1,key);
					
				}
			}
		}
	}
	
	private Set<StatisConfigJRso> copyConfigs() {
		Set<StatisConfigJRso> cons = new HashSet<>();
		synchronized(configs) {
			cons.addAll(configs.values());
		}
		return cons;
	}

	private void statisConfigRemove(StatisConfigJRso lw) {
		if(lw == null || !lw.isEnable()) {
			return;
		}
		
		StatisConfigJRso olw = null;
		synchronized(configs) {
			olw = configs.get(lw.getId());
		}

		if(olw != null) {
			//禁用配置
			parseStatisConfigData(olw,0);
		}
		if(olw != null) {
			synchronized(configs) {
				configs.remove(lw.getId());
			}
		}
		return;
	
	}

	private void statisConfigAdd(StatisConfigJRso lw) {

		if(lw == null || !lw.isEnable()) {
			return;
		}
		
		initStatisConfig(lw);
		
		switch(lw.getByType()) {
		case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			 String[] srvs = lw.getByKey().split(UniqueServiceKeyJRso.SEP);
			 lw.setBysn(srvs[UniqueServiceKeyJRso.INDEX_SN]);
			 lw.setByns(srvs[UniqueServiceKeyJRso.INDEX_NS]);
			 lw.setByver(srvs[UniqueServiceKeyJRso.INDEX_VER]);
			 lw.setByins(srvs[UniqueServiceKeyJRso.INDEX_INS]);
			 lw.setByme(srvs[UniqueServiceKeyJRso.INDEX_METHOD]);
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

	private void statisConfigChanged(StatisConfigJRso lw) {
		if(lw == null) {
			return;
		}
		
		StatisConfigJRso olw = null;
		synchronized(configs) {
			olw = configs.get(lw.getId());
		}
		
		if(!lw.isEnable()) {
			if(olw != null && olw.isEnable()) {
				//禁用配置
				//initStatisConfig(lw);
				parseStatisConfigData(olw,0);
			}
			if(olw != null) {
				synchronized(configs) {
					configs.remove(lw.getId());
				}
			}
			return;
		}
		
		initStatisConfig(lw);
		
		switch(lw.getByType()) {
		case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			 String[] srvs = lw.getByKey().split(UniqueServiceKeyJRso.SEP);
			 lw.setBysn(srvs[UniqueServiceKeyJRso.INDEX_SN]);
			 lw.setByns(srvs[UniqueServiceKeyJRso.INDEX_NS]);
			 lw.setByver(srvs[UniqueServiceKeyJRso.INDEX_VER]);
			 lw.setByins(srvs[UniqueServiceKeyJRso.INDEX_INS]);
			 lw.setByme(srvs[UniqueServiceKeyJRso.INDEX_METHOD]);
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
	
	
	private void parseStatisConfigData(StatisConfigJRso lw, int enable) {
		switch(lw.getByType()) {
		case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			 String[] srvs = lw.getByKey().split(UniqueServiceKeyJRso.SEP);
			 lw.setBysn(srvs[UniqueServiceKeyJRso.INDEX_SN]);
			 lw.setByns(srvs[UniqueServiceKeyJRso.INDEX_NS]);
			 lw.setByver(srvs[UniqueServiceKeyJRso.INDEX_VER]);
			 lw.setByins(srvs[UniqueServiceKeyJRso.INDEX_INS]);
			 lw.setByme(srvs[UniqueServiceKeyJRso.INDEX_METHOD]);
			 parseServiceConfigData(lw,enable,lw.getByKey());
			 break;
		case StatisConfigJRso.BY_TYPE_INSTANCE:
			parseInstanceConfigData(lw,enable,lw.getByKey());
		}
	}

	private void parseInstanceConfigData(StatisConfigJRso lw, int enable,String insName) {
		//String insName = lw.getByKey();
		parseType2ConfigNum(this.ins2Types2ConfigNum,lw,insName,enable);
	}

	private void parseServiceConfigData(StatisConfigJRso lw,int enable,String key) {
		
		Set<UniqueServiceKeyJRso> items = reg.getServices(lw.getBysn(), lw.getByns(), lw.getByver());
		if(items == null || items.isEmpty()) {
			return;
		}
		
		for(UniqueServiceKeyJRso si : items) {
			UniqueServiceMethodKeyJRso sm = this.srvMng.getServiceMethodKey(si.fullStringKey(), lw.getByme());
			if(sm == null) {
				continue;
			}
			
			if(lw.getByType() == StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD) {
				if(!sm.getInstanceName().equals(lw.getByins())) {
					continue;
				}
			}
			
			if(lw.getByType() == StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD) {
				if(!sm.getInstanceName().equals(lw.getByins())) {
					continue;
				}
			}
			
			/*String smKey = sm.getKey().toKey(true, true, true);
			
			if(lw.getByType() == StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD) {
				smKey += UniqueServiceKey.SEP + lw.getActName(); 
			} else {
				smKey += UniqueServiceKey.SEP ; 
			}*/
			
			parseType2ConfigNum(this.sm2Types2ConfigNum,lw,key,enable);
			
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
	
	private void parseType2ConfigNum(Map<String, Map<Short,Integer>> key2Type2ConfigNum,StatisConfigJRso lw
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
	
	
	private void initStatisConfig(StatisConfigJRso lw) {
		if(!lw.isEnable()) {
			return ;
		}
		
		Set<Short> set = null;
		if(!Utils.isEmpty(lw.getNamedType())) {
			String p = Config.getRaftBasePath(Config.NamedTypesDir)+"/"+lw.getNamedType();
			set = this.getTypeByKey(p);
		} else {
			set = new HashSet<>();
		}
		
		lw.setTypes(set);
		for(StatisIndexJRso si : lw.getStatisIndexs()) {
			set.addAll(Arrays.asList(si.getNums()));
			if(si.getDens() != null && si.getDens().length > 0) {
				set.addAll(Arrays.asList(si.getDens()));
			}
		}
		
		if(!Utils.isEmpty(lw.getExpStr())) {
			List<String> suffixExp = ExpUtils.toSuffix(lw.getExpStr());
			if(!ExpUtils.isValid(suffixExp)) {
				logger.error("Invalid exp0: " + lw.getId() + "---> " + lw.getExpStr());
				return;
			}
			Exp exp = new Exp();
			exp.setSuffix(suffixExp);
			exp.setOriEx(lw.getExpStr());
			lw.setExp0(exp);
		}
		
		if(!Utils.isEmpty(lw.getExpStr1())) {
			List<String> suffixExp = ExpUtils.toSuffix(lw.getExpStr1());
			if(!ExpUtils.isValid(suffixExp)) {
				logger.error("Invalid exp1: " + lw.getId() + "---> " + lw.getExpStr1());
				return;
			}
			Exp exp = new Exp();
			exp.setSuffix(suffixExp);
			exp.setOriEx(lw.getExpStr());
			lw.setExp1(exp);
		}
		
	}
}
