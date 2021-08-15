package cn.jmicro.monitor.statis.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.mng.ProcessInstanceManager.IInstanceListener;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.StatisConfigJRso;
import cn.jmicro.api.monitor.StatisIndexJRso;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class StatisConfigManager {

	private static final String ID_MATCHER = "[0-9a-zA-Z_\\.\\-]+";
	
	private static final String METHOD_MATCHER = "[a-zA-Z_][a-zA-Z0-9_]*";
	
	private final Logger logger = LoggerFactory.getLogger(StatisConfigManager.class);
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private ServiceManager srvMng;
	
	@Inject
	private ProcessInstanceManager insManager;
	
	private Map<Integer,StatisConfigJRso> configs = new HashMap<>();
	
	//private Map<String,Set<Integer>> srvName2Configs = new HashMap<>();
	//方便根据实例名取配置
	//private Map<String,Set<Integer>> ins2Configs = new HashMap<>();
	
	//一个服务方法可以被不同配置匹配
	//private Map<String,Set<Integer>> srvMethod2Configs = new HashMap<>();
	
	//一个配置可以匹配一个服务方法，并且可以匹配不同运行实例的同一个服务方法
	//private Map<Integer,Set<String>> config2SmKeys = new HashMap<>();
	
	//private Map<String,Set<Integer>> instance2Configs = new HashMap<>();
	
	//private Map<String,Set<Integer>> account2Configs = new HashMap<>();
	
	private RaftNodeDataListener<StatisConfigJRso> configListener;
	
	private String logDir;
	
	private IServiceListener snvListener = (type,siKey,item)->{
		if(type == IListener.REMOVE) {
			//onServiceRemove(item);
		}else if(type == IListener.ADD) {
			onServiceAdd(siKey,item);
		}
	};
	
	private IInstanceListener insListener = (type, pi)->{
		if(type == IListener.ADD) {
			Set<StatisConfigJRso> cfgs = getInstanceConfigs(pi.getInstanceName());
			if(cfgs.isEmpty()) {
				return;
			}
			setProcessInfoMonitorable(cfgs.iterator().next(),1);
		}
	};
	
	public void jready() {
		logDir = System.getProperty("user.dir")+"/logs/most/";
		File d = new File(logDir);
		if(!d.exists()) {
			d.mkdirs();
		}
		
		configListener = new RaftNodeDataListener<>(op,StatisConfigJRso.STATIS_CONFIG_ROOT,StatisConfigJRso.class,false);
		configListener.addListener(lis);
		
		srvMng.addListener(snvListener);
		insManager.addInstanceListner(insListener);
	}

	private IRaftListener<StatisConfigJRso> lis = new IRaftListener<StatisConfigJRso>() {
		public void onEvent(int type,String id, StatisConfigJRso lw) {
			Integer cid = Integer.parseInt(id);
			if(type == IListener.DATA_CHANGE) {
				statisConfigChanged(cid,lw);
			}else if(type == IListener.REMOVE){
				statisConfigRemove(lw);
			}else if(type == IListener.ADD) {
				statisConfigAdd(lw);
			}
		}
	};
	
	private void statisConfigChanged(Integer id, StatisConfigJRso lw) {
		if(lw == null) {
			return;
		}
		
		if(!lw.isEnable()) {
			StatisConfigJRso olw = configs.get(id);
			if(olw != null) {
				statisConfigRemove(olw);
			}
			return;
		}
		
		if(!configs.containsKey(id)) {
			//启用中的配置不可能改数据，所以认为是重复的通知
			statisConfigAdd(lw);
		} else {
			StatisConfigJRso olw = configs.get(id);
			statisConfigRemove(olw);
			statisConfigAdd(lw);
		}
	}
	
	private void statisConfigRemove(StatisConfigJRso lw) {
		if(lw == null || !lw.isEnable()) {
			return;
		}
		
		StatisConfigJRso olw = configs.get(lw.getId());

		if(olw != null) {
			synchronized(configs) {
				configs.remove(olw.getId());
			}
			
			parseConfigData(olw,0);
			//removeService2Config(olw);
			notifyStatisConfig(IListener.REMOVE,olw);
		}
		
		if(StatisConfigJRso.TO_TYPE_FILE == lw.getToType()) {
			if(lw.getBw() != null) {
				try {
					lw.getBw().close();
				} catch (IOException e) {
					LG.logWithNonRpcContext(MC.LOG_ERROR, StatisManager.class.getName(), 
							"Close buffer error for: " + lw.getId(), e,MC.MT_DEFAULT,true);
				}
			}
		}
		
		return;
	
	}

	private void statisConfigAdd(StatisConfigJRso lw) {

		if(lw == null || !lw.isEnable()) {
			return;
		}
		
		if(configs.containsKey(lw.getId())) {
			//启用中的配置不可能改数据，所以认为是重复的通知
			return;
		}
		
		Set<Short> set = null;
		if(!Utils.isEmpty(lw.getNamedType())) {
			String p = Config.getRaftBasePath(Config.NamedTypesDir)+"/"+lw.getNamedType();
			set = this.getTypeByKey(p);
		}else {
			set = new HashSet<>();
		}
		
		lw.setTypes(set);
		
		for(StatisIndexJRso si : lw.getStatisIndexs()) {
			set.addAll(Arrays.asList(si.getNums()));
			if(si.getDens() != null && si.getDens().length > 0) {
				set.addAll(Arrays.asList(si.getDens()));
			}
		}
		
		parseConfigData(lw,1);
		
		//addService2Config(lw);
		
		if(lw.isEnable()) {
			updateMonitorAttr(lw,1);
		}else {
			updateMonitorAttr(lw,0);
		}
		
		statisConfigAdd0(lw);
		
		notifyStatisConfig(IListener.ADD,lw);
		
		//已经存在的配置从禁用到启用
		synchronized(configs) {
			configs.put(lw.getId(), lw);
		}
				
	}
	
	private void parseConfigData(StatisConfigJRso lw,int enable) {
		int tt = lw.getByType();
		switch(tt) {
			case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
			case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
			case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
				 String[] srvs = lw.getByKey().split(UniqueServiceKeyJRso.SEP);
				 lw.setBysn(srvs[UniqueServiceKeyJRso.INDEX_SN]);
				 lw.setByns(srvs[UniqueServiceKeyJRso.INDEX_NS]);
				 lw.setByver(srvs[UniqueServiceKeyJRso.INDEX_VER]);
				 lw.setByins(srvs[UniqueServiceKeyJRso.INDEX_INS]);
				 lw.setByme(srvs[UniqueServiceKeyJRso.INDEX_METHOD]);
				 break;
			case StatisConfigJRso.BY_TYPE_INSTANCE:
				/*if(enable == 1) {
					if(!instance2Configs.containsKey(lw.getByins())) {
						instance2Configs.put(lw.getByins(), new HashSet<>());
					}
					instance2Configs.get(lw.getByins()).add(lw.getId());
				}else {
					if(instance2Configs.containsKey(lw.getByins())) {
						instance2Configs.get(lw.getByins()).remove(lw.getId());
					}
				}*/
				break;
			case StatisConfigJRso.BY_TYPE_ACCOUNT:
				
				break;
		}
		
		if(StatisConfigJRso.TO_TYPE_SERVICE_METHOD == lw.getToType()) {
			 String[] ps = lw.getToParams().split(UniqueServiceKeyJRso.SEP);
			 lw.setToSn(ps[UniqueServiceKeyJRso.INDEX_SN]);
			 lw.setToNs(ps[UniqueServiceKeyJRso.INDEX_NS]);
			 lw.setToVer(ps[UniqueServiceKeyJRso.INDEX_VER]);
			 lw.setToMt(ps[UniqueServiceKeyJRso.INDEX_METHOD]);
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
	
	private Set<StatisConfigJRso> getByConfigByServiceName(String serviceName) {
		Set<StatisConfigJRso> cfgs = new HashSet<>();
		synchronized(configs) {
			for(StatisConfigJRso sc : this.configs.values()) {
				if(sc.getToSn() != null && sc.getToSn().equals(serviceName)) {
					cfgs.add(sc);
				}
			}
		}
		return cfgs;
	}
	
	private void onServiceAdd(UniqueServiceKeyJRso siKey,ServiceItemJRso item) {
		Set<StatisConfigJRso> cfgs = this.getByConfigByServiceName(siKey.getServiceName());
		
		if(cfgs.isEmpty()) {
			return;
		}
		
		for(StatisConfigJRso sc : cfgs) {
			ServiceItemJRso si = this.srvMng.getServiceByKey(siKey.fullStringKey());
			ServiceMethodJRso sm = si.getMethod(sc.getByme());
			if(sm == null) {
				continue;
			}
			
			if(sc.getByType() == StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD) {
				if(!sm.getKey().getInstanceName().equals(sc.getByins())) {
					continue;
				}
			}
			
			srvMng.setMonitorable(sm,1);
		}
	}
	
	private void updateMonitorAttr(StatisConfigJRso lw,int enable) {
		int tt = lw.getByType();
		switch(tt) {
			case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
			case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
			case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
				 setServiceMethodMonitorable(lw,enable);
				 break;
			case StatisConfigJRso.BY_TYPE_INSTANCE:
				setProcessInfoMonitorable(lw,enable);
		}
	}

	private void setProcessInfoMonitorable(StatisConfigJRso lw, int enable) {
		Set<ProcessInfoJRso> pis = insManager.getProcessByNamePreifx(lw.getByKey());
		if(pis != null && !pis.isEmpty()) {
			final boolean en = enable == 1?true:false;
			pis.forEach((pi)->{
				if(pi != null && (pi.isMonitorable() != en)) {
					pi.setMonitorable(en);
				}
				op.setData(ChoyConstants.INS_ROOT+"/"+pi.getId(), JsonUtils.getIns().toJson(pi));
			});
		}
	}

	private void setServiceMethodMonitorable(StatisConfigJRso lw,int enable) {
		Set<UniqueServiceKeyJRso> items = reg.getServices(lw.getBysn(), lw.getByns(), lw.getByver());
		if(items == null || items.isEmpty()) {
			return;
		}
		for(UniqueServiceKeyJRso siKey : items) {
			ServiceItemJRso si = this.srvMng.getServiceByKey(siKey.fullStringKey());
			ServiceMethodJRso sm = si.getMethod(lw.getByme());
			if(sm == null) {
				continue;
			}
			
			if(lw.getByType() == StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD) {
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
			synchronized(this.configs) {
				scListener.add(l);
				for(StatisConfigJRso sc : this.configs.values()) {
					l.onEvent(IListener.ADD,sc);
				}
			}
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
	
	private void notifyStatisConfig(int event,StatisConfigJRso sc) {
		if(scListener == null) {
			return;
		}
		
		for(IStatisConfigListener l : this.scListener) {
			l.onEvent(event, sc);
		}
	}

	interface IStatisConfigListener extends IListener {
		void onEvent(int event,StatisConfigJRso sc);
	}
	
	Set<StatisConfigJRso> getConfigByType(int...types) {
		
		if(types == null || types.length == 0) {
			throw new CommonException("Config types cannot be Null!");
		}
		
		Set<StatisConfigJRso> cfgs = new HashSet<>();
		synchronized(configs) {
			for(StatisConfigJRso sc : this.configs.values()) {
				for(int t : types) {
					if(sc.getByType() == t) {
						cfgs.add(sc);
						break;
					}
				}
			}
		}
		return cfgs;
	}
	
	StatisConfigJRso getConfigs(Integer cid) {
		return this.configs.get(cid);
	}
	
	private void statisConfigAdd0(StatisConfigJRso lw) {

		if(StatisConfigJRso.TO_TYPE_DB == lw.getToType()) {
			if(Utils.isEmpty(lw.getToParams())) {
				lw.setToParams(StatisConfigJRso.DEFAULT_DB);
			}
		} else if(StatisConfigJRso.TO_TYPE_FILE == lw.getToType()) {

			File logFile = new File(this.logDir + lw.getId() + "_" + lw.getToParams());
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					String msg ="Create log file fail";
					logger.error(msg, e);
					LG.logWithNonRpcContext(MC.LOG_ERROR, StatisManager.class.getName(), msg, e,MC.MT_DEFAULT,true);
				}
			}

			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
				lw.setBw(bw);
			} catch (FileNotFoundException e) {
				String msg ="Create writer fail";
				logger.error(msg, e);
				LG.logWithNonRpcContext(MC.LOG_ERROR, StatisManager.class.getName(), msg, e,MC.MT_DEFAULT,true);
			}
		}
		
		String regex = "";
		switch(lw.getByType()) {
		case StatisConfigJRso.BY_TYPE_SERVICE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfigJRso.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			  regex = "^"+lw.getBysn()+"##";
			 
			  //服务名
			 if(Utils.isEmpty(lw.getByns()) || "*".equals(lw.getByns())) {
				 regex += ID_MATCHER+"##";
			 }else {
				 regex += lw.getByns()+"##";
			 }
			
			 //版本
			 if(Utils.isEmpty(lw.getByver()) || "*".equals(lw.getByver())) {
				regex += ID_MATCHER+"##";
			 } else {
				regex += lw.getByver()+"##";
			 }
			 
			 //实例名
			 if(Utils.isEmpty(lw.getByins()) || "*".equals(lw.getByins())) {
				regex += ID_MATCHER+"##";
			 } else {
				regex += lw.getByins()+"##";
			 }
			 
			 regex += "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}##\\d{1,5}##";//ip和端口
			 
			 //方法名
			 if(Utils.isEmpty(lw.getByme()) || "*".equals(lw.getByme().trim())) {
				regex += METHOD_MATCHER+"##";
			 } else {
				regex += lw.getByme()+"##";
			 }
			 
			 //方法参数
			 //regex += "[;\\]\\.\\/,a-zA-Z0-9]*##";
			 
			 //账号
			/* if(Utils.isEmpty(lw.getActName()) || "*".equals(lw.getActName())) {
				regex += "[a-zA-Z0-9\\_\\-]*##";
			 } else {
				regex += lw.getActName()+"##";
			 }*/
			 
			 if(lw.getClientId()==Constants.NO_CLIENT_ID && lw.getCreatedBy() == Config.getClientId()) {
				 regex += "[-]{0,1}\\d*";
			 } else {
				 regex += lw.getClientId();
			 }
			 
			 regex += "$";
			 
			Pattern pattern = Pattern.compile(regex);
			lw.setPattern(pattern);
			break;
		case StatisConfigJRso.BY_TYPE_INSTANCE:
			regex = lw.getByKey();
			
			regex += "[a-zA-Z\\_\\-]*\\d*##";
			
			if(lw.getClientId()==Constants.NO_CLIENT_ID && lw.getCreatedBy() == Config.getClientId()) {
				 regex += "[-]{0,1}\\d+";
			 }else {
				 regex += "" + lw.getClientId();
			 }
			
			pattern = Pattern.compile(regex);
			lw.setPattern(pattern);
			break;
		}
		
		Short[] ts = new Short[lw.getTypes().size()];
		lw.getTypes().toArray(ts);
		
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
		lw.setCounterTimeout(lw.getCounterTimeout()*1000);
	}

	public Set<StatisConfigJRso> getInstanceConfigs(String instanceName) {
		Set<StatisConfigJRso> cfgs = new HashSet<>();
		synchronized(configs) {
			for(StatisConfigJRso sc : this.configs.values()) {
				if(sc.getByType() == StatisConfigJRso.BY_TYPE_INSTANCE && 
						instanceName.startsWith(sc.getByKey())) {
					cfgs.add(sc);
				}
			}
		}
		return cfgs;
	}

}
