package cn.jmicro.api.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class MonitorStatisConfigManager {

	public static final String STATIS_WARNING_ROOT = Config.BASE_DIR + "/statisConfigs";

	private final Logger logger = LoggerFactory.getLogger(MonitorStatisConfigManager.class);
	
	@Inject
	private IDataOperator op;
	
	private ReentrantLock configLock = new ReentrantLock();
	
	private Map<Integer,StatisConfig> configs = new HashMap<>();
	
	private Map<Integer,Set<String>> configId2SmKeys = new HashMap<>();
	
	private Map<String,Set<Integer>> srv2Configs = new HashMap<>();
	
	private Map<String,Set<Integer>> srvName2Configs = new HashMap<>();
	
	private Map<String,Set<Integer>> instance2Configs = new HashMap<>();
	
	private Map<String,Set<Integer>> account2Configs = new HashMap<>();
	
	//private Map<String,Set<Integer>> expTag2Configs = new HashMap<>();
	
	private Map<String,Set<Short>> sm2Types = new HashMap<>();
	
	private Map<String,Set<Short>> instance2Types = new HashMap<>();
	
	private Map<String,Set<Short>> account2Types = new HashMap<>();
	
	public void ready() {
		op.addChildrenListener(STATIS_WARNING_ROOT, lis);
	}
	
	//依据服务7要素做初步判断是否需要提交数据到监控服务器
	//服务7要素：服务名称，名称空间，版本，实例名，IP，端口，方法名
	public boolean canSubmit(ServiceMethod sm , Short t,String actName) {
		
		String smKey = sm.getKey().toKey(false, false, false);
		if(!sm2Types.containsKey(smKey)) {
			computeWithRpcContext(sm,actName);
		}
		
		if(sm2Types.containsKey(smKey) && sm2Types.get(smKey).contains(t)) {
			return true;
		}
		
		return canSubmitNonRpc(sm.getKey().getInstanceName(),actName,t);
	}
	
	//非RPC环境下，基于实例名，启动实例的账号名，IP做判断是否需要提交数据
	public boolean canSubmitNonRpc(String intName ,String actName, Short t) {
		
		if(!instance2Types.containsKey(intName)) {
			computeByInstanceName(intName);
		}
		
		if(instance2Types.containsKey(intName) 
				&& instance2Types.get(intName).contains(t)) {
			return true;
		}
		
		if(!Utils.isEmpty(actName)) {
			if(!account2Types.containsKey(actName)) {
				computeByActName(actName);
			}
			
			if(account2Types.containsKey(actName) 
					&& account2Types.get(actName).contains(t)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void computeWithRpcContext(ServiceMethod sm,String actName) {
		boolean isLock = false;
		try {
			
			isLock = configLock.tryLock(10, TimeUnit.SECONDS);
			
			if(isLock) {
				String smKey = sm.getKey().toKey(false, false, false);
				
				Set<Short> types = new HashSet<>();
				sm2Types.put(smKey, types);
				
				String key = smKey;
				if(this.srv2Configs.containsKey(key)) {
					Set<Integer> scs = this.srv2Configs.get(key);
					if(scs != null && scs.size() > 0) {
						for(Integer cid : scs) {
							StatisConfig sc = this.configs.get(cid);
							if(sc != null && computeWithFullService(sc,sm,actName)) {
								if(sc.getExp() != null) {
									if(computeByExpression(sc,sm)) {
										addTypeForMethod(smKey,sc,types);
									}
								} else {
									addTypeForMethod(smKey,sc,types);
								}
							}
						}
					}
				}
				
				key = sm.getKey().getServiceName();
				if(this.srvName2Configs.containsKey(key)) {
					Set<Integer> scs = this.srvName2Configs.get(key);
					if(scs != null && scs.size() > 0) {
						for(Integer cid : scs) {
							StatisConfig sc = this.configs.get(cid);
							if(sc != null && computeWithServiceName(sc,sm,actName)) {
								if(sc.getExp() != null) {
									if(computeByExpression(sc,sm)) {
										addTypeForMethod(smKey,sc,types);
									}
								} else {
									addTypeForMethod(smKey,sc,types);
								}
							}
						}
					}
				}
				
				/*if(expTag2Configs.containsKey(sm.getKey().getServiceName())) {
					Set<Integer> scs = this.expTag2Configs.get(key);
					if(scs != null && scs.size() > 0) {
						for(Integer cid : scs) {
							StatisConfig sc = this.configs.get(cid);
							if(sc.getExpForType() != StatisConfig.EXP_TYPE_SERVICE) {
								continue;
							}
							
							if(computeByExpression(sc,sm)) {
								addTypeForMethod(smKey,sc,types);
							}
						}
					}
				}*/
				
			}
			
		} catch (InterruptedException e) {
			logger.error("",e);
		} finally {
			if(isLock) {
				configLock.unlock();
			}
		}
	}
	
	private void addTypeForMethod(String smKey,StatisConfig sc,Set<Short> types) {
		if(sc.getTypes()!= null) {
			types.addAll(sc.getTypes());
		}
		
		if(!configId2SmKeys.containsKey(sc.getId())) {
			configId2SmKeys.put(sc.getId(), new HashSet<>());
		}
		configId2SmKeys.get(sc.getId()).add(smKey);
	}

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

	private void computeByActName(String actName) {
		String key = actName;
		Set<Short> types = new HashSet<>();
		this.account2Types.put(actName, types);
		
		if(actName != null) {
			if(this.account2Configs.containsKey(key)) {
				Set<Integer> scs = this.account2Configs.get(key);
				if(scs != null && scs.size() > 0) {
					for(Integer cid : scs) {
						StatisConfig sc = this.configs.get(cid);
						if(Utils.isEmpty(sc.getActName()) && sc.getActName().equals(actName)) {
							if(sc.getExp() != null) {
								if(computeByExpression(sc,null)) {
									types.addAll(sc.getTypes());
								}
							}else {
								types.addAll(sc.getTypes());
							}
						}
					}
				}
			}
			
			/*if(expTag2Configs.containsKey(actName)) {
				Set<Integer> scs = this.expTag2Configs.get(actName);
				if(scs != null && scs.size() > 0) {
					for(Integer cid : scs) {
						StatisConfig sc = this.configs.get(cid);
						if(sc.getExpForType() != StatisConfig.EXP_TYPE_ACCOUNT) {
							continue;
						}
						if(computeByExpression(sc,null)) {
							types.addAll(sc.getTypes());
						}
					}
				}
			}*/
			
		}
	}

	private void computeByInstanceName(String  instanceName) {
		Set<Short> types = new HashSet<>();
		String key = instanceName;
		this.instance2Types.put(instanceName, types);
		
		if(this.instance2Configs.containsKey(key)) {
			Set<Integer> scs = this.instance2Configs.get(key);
			if(scs != null && scs.size() > 0) {
				for(Integer cid : scs) {
					StatisConfig sc = this.configs.get(cid);
					String byIns = sc.getByins();
					if(byIns.endsWith("*")) {
						String insPrefix = byIns.substring(0,byIns.length()-1);
						if(instanceName.startsWith(insPrefix)) {
							if(sc.getExp() != null) {
								if(computeByExpression(sc,null)) {
									types.addAll(sc.getTypes());
								}
							} else {
								types.addAll(sc.getTypes());
							}
						}
					} else {
						if(instanceName.equals(byIns)) {
							if(sc.getExp() != null) {
								if(computeByExpression(sc,null)) {
									types.addAll(sc.getTypes());
								}
							} else {
								types.addAll(sc.getTypes());
							}
						}
					}
				}
			}
		}
		
		/*if(expTag2Configs.containsKey(instanceName)) {
			Set<Integer> scs = this.expTag2Configs.get(instanceName);
			if(scs != null && scs.size() > 0) {
				for(Integer cid : scs) {
					StatisConfig sc = this.configs.get(cid);
					if(sc.getExpForType() != StatisConfig.EXP_TYPE_INSTANCE) {
						continue;
					}
					if(computeByExpression(sc,null)) {
						types.addAll(sc.getTypes());
					}
				}
			}
		}*/
		
	}

	private boolean computeWithServiceName(StatisConfig cfg, ServiceMethod sm,String actName) {
		if(UniqueServiceKey.matchNamespace(cfg.getByns(), sm.getKey().getNamespace())
				&& UniqueServiceKey.matchVersion(cfg.getByver(), sm.getKey().getVersion())) {
			return computeWithFullService(cfg,sm,actName);
		}
		return false;
	}

	private boolean computeWithFullService(StatisConfig cfg, ServiceMethod sm,String actName) {
		
		switch(cfg.getByType()) {
		case StatisConfig.BY_TYPE_SERVICE_METHOD:
			 if(!sm.getKey().getMethod().equals(cfg.getByme())) {
				 return false;
			 }
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE:
			 if(!sm.getKey().getInstanceName().equals(cfg.getByins())) {
				 return false;
			 }
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
			 if(!(sm.getKey().getInstanceName().equals(cfg.getByins()) 
					 && sm.getKey().getMethod().equals(cfg.getByme()))) {
				 return false;
			 }
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
			if(actName== null || !(actName.equals(cfg.getActName()) 
					 && sm.getKey().getInstanceName().equals(cfg.getByins()))) {
				return false;
			 }
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT:
			 if(actName== null || !actName.equals(cfg.getActName())) {
				 return false;
			 }
		//case StatisConfig.BY_TYPE_SERVICE:
		}
		return true;
	}

	private IDataListener warnDataChangeListener = new IDataListener() {
		public void dataChanged(String path, String data) {
			Integer id = Integer.parseInt(path.substring(path.lastIndexOf("/")+1));
			
			StatisConfig lw = parseStatisConfig(data,id);
			if(lw == null) {
				return;
			}
			
			StatisConfig olw = configs.get(id);
			
			if(!lw.isEnable()) {
				if(olw != null && olw.isEnable()) {
					//禁用配置
					removeConfig(olw);
					notifyStatisConfig(IListener.REMOVE,olw);
				}
				return;
			}
			
			if(olw == null) {
				//已经存在的配置从禁用到启用
				configs.put(lw.getId(), lw);
				addConfig(lw);
				notifyStatisConfig(IListener.ADD,olw);
				return;
			}
			
			olw.setByKey(lw.getByKey());
			olw.setByType(lw.getByType());
			olw.setActName(lw.getActName());
			olw.setStatisIndexs(lw.getStatisIndexs());
			olw.setTag(lw.getTag());
			olw.setTimeCnt(lw.getTimeCnt());
			olw.setTimeUnit(lw.getTimeUnit());
			olw.setToParams(lw.getToParams());
			olw.setToType(lw.getToType());
			olw.setActName(lw.getActName());
			olw.setForClientId(lw.getForClientId());
			
			olw.setByins(lw.getByins());
			olw.setBysn(lw.getBysn());
			olw.setByns(lw.getByns());
			olw.setByver(lw.getByver());
			olw.setByme(lw.getByme());
			
			olw.setToSn(lw.getToSn());
			olw.setToVer(lw.getToVer());
			olw.setToNs(lw.getToNs());
			olw.setToMt(lw.getToMt());
			
			//notifyStatisConfig(IListener.DATA_CHANGE,olw);
		}
	};
	
	private IChildrenListener lis = new IChildrenListener() {
		public void childrenChanged(int type, String parent, String id, String data) {
			Integer cid = Integer.parseInt(id);
			if(type == IListener.ADD) {
				StatisConfig lw = parseStatisConfig(data,cid);
				if(lw == null) {
					return;
				}
				
				op.addDataListener(parent+"/"+id, warnDataChangeListener);
				if(!lw.isEnable()) {
					return;
				}
				
				configs.put(lw.getId(), lw);
				
				addConfig(lw);
				
				notifyStatisConfig(IListener.ADD,lw);
				
			}else if(type == IListener.REMOVE) {
				if(!configs.containsKey(cid)) {
					//删除没有启用的配置，无需处理
					return;
				}
				op.removeDataListener(parent+"/"+id, warnDataChangeListener);
				
				StatisConfig sc = configs.get(cid);
				/*if(sc.getBw() != null) {
					try {
						sc.getBw().close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}*/
				
				removeConfig(sc);
				
				notifyStatisConfig(IListener.REMOVE,sc);
			}
		}
	};
	
	private StatisConfig parseStatisConfig(String data, Integer id) {
		StatisConfig lw = JsonUtils.getIns().fromJson(data, StatisConfig.class);
		if(lw == null) {
			logger.error("Invalid StatisConfig: " + data);
			return null;
		}
		
		if(!lw.isEnable()) {
			return lw;
		}
		
		if(checkConfig(lw) != null) {
			return null;
		}
		
		String p = Config.NamedTypesDir+"/"+lw.getNamedType();
		Set<Short> set = this.getTypeByKey(p);
		lw.setTypes(set);
		
		if(!Utils.isEmpty(lw.getExpStr())) {
			List<String> suffixExp = ExpUtils.toSuffix(lw.getExpStr());
			if(!ExpUtils.isValid(suffixExp)) {
				logger.error("Invalid exp: " + id + "---> " + lw.getExpStr());
				return null;
			}
			Exp exp = new Exp();
			exp.setSuffix(suffixExp);
			exp.setOriEx(lw.getExpStr());
			lw.setExp(exp);
		}
		
		return lw;
	}
	
	protected void removeConfig(StatisConfig lw) {
		this.configs.remove(lw.getId());
		
		switch(lw.getByType()) {
		case StatisConfig.BY_TYPE_SERVICE:
		case StatisConfig.BY_TYPE_SERVICE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT: 
			String key = null;
			if(configId2SmKeys.containsKey(lw.getId())) {
				for(String smKey : configId2SmKeys.get(lw.getId())) {
					if(srv2Configs.containsKey(smKey)) {
						srv2Configs.get(smKey).remove(lw.getId());
					}
					sm2Types.remove(smKey);
				}
			}
			if(this.srvName2Configs.containsKey(lw.getBysn())) {
				this.srvName2Configs.get(lw.getBysn()).remove(lw.getId());
			}
			break;
		case StatisConfig.BY_TYPE_INSTANCE:
			key = lw.getByins();
			if(instance2Configs.containsKey(key)) {
				instance2Configs.get(key).remove(lw.getId());
			}
			this.instance2Types.remove(key);
			break;
		case StatisConfig.BY_TYPE_ACCOUNT:
			key = lw.getActName();
			if(account2Configs.containsKey(key)) {
				account2Configs.get(key).remove(lw.getId());
			}
			this.account2Types.remove(key);
			break;
		/*case StatisConfig.BY_TYPE_EXP:
			key = lw.getByKey();
			if(expTag2Configs.containsKey(key)) {
				expTag2Configs.get(key).remove(lw.getId());
			}
			if(lw.getExpForType() == StatisConfig.EXP_TYPE_SERVICE) {
				if(configId2SmKeys.containsKey(lw.getId())) {
					for(String smKey : configId2SmKeys.get(lw.getId())) {
						sm2Types.remove(smKey);
					}
				}
			}else if(lw.getExpForType() == StatisConfig.EXP_TYPE_ACCOUNT) {
				account2Types.remove(key);
			}else if(lw.getExpForType() == StatisConfig.EXP_TYPE_INSTANCE) {
				instance2Types.remove(key);
			}		
			break;
			*/	
		}
		
	}

	protected void addConfig(StatisConfig lw) {
		switch(lw.getByType()) {
		case StatisConfig.BY_TYPE_SERVICE:
		case StatisConfig.BY_TYPE_SERVICE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE:
		case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
		case StatisConfig.BY_TYPE_SERVICE_ACCOUNT: 
			String key = null;
			if(lw.getByns().endsWith("*") || lw.getByver().endsWith("*")) {
				key = lw.getBysn();
				if(!srvName2Configs.containsKey(key)) {
					srvName2Configs.put(key, new HashSet<>());
				}
				srvName2Configs.get(key).add(lw.getId());
			} else {
				key = UniqueServiceKey.serviceName(lw.getBysn(),lw.getByns(),lw.getByver());
				if(!srv2Configs.containsKey(key)) {
					srv2Configs.put(key, new HashSet<>());
				}
				srv2Configs.get(key).add(lw.getId());	
			}
		case StatisConfig.BY_TYPE_INSTANCE:
			key = lw.getByins();
			if(!instance2Configs.containsKey(key)) {
				instance2Configs.put(key, new HashSet<>());
			}
			instance2Configs.get(key).add(lw.getId());
			break;
		case StatisConfig.BY_TYPE_ACCOUNT:
			key = lw.getActName();
			if(!account2Configs.containsKey(key)) {
				account2Configs.put(key, new HashSet<>());
			}
			account2Configs.get(key).add(lw.getId());
			break;
		/*case StatisConfig.BY_TYPE_EXP:
			key = lw.getByKey();
			if(!expTag2Configs.containsKey(key)) {
				expTag2Configs.put(key, new HashSet<>());
			}
			expTag2Configs.get(key).add(lw.getId());*/
		}
		
	}

	public String checkConfig(StatisConfig lw) {
		String msg = checkByType(lw);
		if(msg != null) {
			return msg;
		}
		
		msg = checkToType(lw);
		if(msg != null) {
			return msg;
		}
		
		if(lw.getStatisIndexs() == null || lw.getStatisIndexs().length == 0) {
			 msg = "Statis index cannot be null for config id: " + lw.getId();
			logger.error(msg);
			LG.logWithNonRpcContext(MC.LOG_ERROR, MonitorStatisConfigManager.class, msg);
			return msg;
		}
		
		if(Utils.isEmpty(lw.getNamedType())) {
			 msg = "NamedType value cannot be null for config id: " + lw.getId();
			logger.error(msg);
			LG.logWithNonRpcContext(MC.LOG_ERROR, MonitorStatisConfigManager.class, msg);
			return msg;
		}
		
		String p = Config.NamedTypesDir+"/"+lw.getNamedType();
		if(!op.exist(p)) {
			 msg = "NamedType ["+lw.getNamedType()+"] not exist for config id: " + lw.getId();
			logger.error(msg);
			LG.logWithNonRpcContext(MC.LOG_ERROR, MonitorStatisConfigManager.class, msg);
			return msg;
		}
		
		return msg;
	}

	private String checkToType(StatisConfig lw) {
		String msg = null;
		try {
			if(Utils.isEmpty(lw.getToType())) {
				msg = "By key params invalid: " + lw.getByKey()+ " for id: " + lw.getId();
				return msg;
			}
			
			if(StatisConfig.TO_TYPE_SERVICE_METHOD.equals(lw.getToType())) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "To key params cannot be null for service [" + StatisConfig.TO_TYPE_SERVICE_METHOD+ "] for id: " + lw.getId();
					return msg;
				}
				
				String[] ps = lw.getToParams().split(UniqueServiceKey.SEP);
				if(ps == null || ps.length < 7) {
					msg = "To param ["+lw.getToParams()+"] invalid [" + StatisConfig.TO_TYPE_SERVICE_METHOD+ "] for id: " + lw.getId();
					return msg;
				}
				
				boolean suc = checkByService(lw.getId(),ps);
				if(!suc) {
					return msg;
				}
				
				if(Utils.isEmpty(ps[6])) {
					msg = "To service method cannot be NULL for id: " + lw.getId();
					return msg;
				}
				
				lw.setToSn(ps[0]);
				lw.setToNs(ps[1]);
				lw.setToVer(ps[2]);
				lw.setToMt(ps[6]);
				
			}else if(StatisConfig.TO_TYPE_DB.equals(lw.getToType())) {
				lw.setToParams(StatisConfig.DEFAULT_DB);
			}else if(StatisConfig.TO_TYPE_FILE.equals(lw.getToType())) {
				if(Utils.isEmpty(lw.getToParams())) {
					msg = "To file cannot be NULL for id: " + lw.getId();
					return msg;
				}

				/*File logFile = new File(this.logDir + lw.getId()+"_"+lw.getToParams());
				if(!logFile.exists()) {
					try {
						logFile.createNewFile();
					} catch (IOException e) {
						logger.error("Create log file fail",e);
						LG.logWithNonRpcContext(MC.LOG_ERROR, MonitorStatisConfigManager.class, msg,e);
						return false;
					}
				}
				
				try {
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
					lw.setBw(bw);
				} catch (FileNotFoundException e) {
					logger.error("Create writer fail",e);
					LG.logWithNonRpcContext(MC.LOG_ERROR, MonitorStatisConfigManager.class, msg,e);
					return false;
				}*/
			}
			
		}finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, MonitorStatisConfigManager.class, msg);
			}
		}
		
		return msg;
	}

	private String checkByType(StatisConfig lw) {
		boolean suc = false;
		String tt = lw.getByType();
		String msg = null;
		
		try {
			if(Utils.isEmpty(tt)) {
				msg = "By type value cannot be null for: " + lw.getId();
			} else {
				switch(tt) {
					case StatisConfig.BY_TYPE_SERVICE:
					case StatisConfig.BY_TYPE_SERVICE_METHOD:
					case StatisConfig.BY_TYPE_SERVICE_INSTANCE:
					case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
					case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
					case StatisConfig.BY_TYPE_SERVICE_ACCOUNT: 
					{
						String[] srvs = lw.getByKey().split(UniqueServiceKey.SEP);
						if(srvs.length < 3) {
							msg = "By key params invalid: " + lw.getByKey()+ " for id: " + lw.getId();
							return msg;
						}
						
						suc = checkByService(lw.getId(),srvs);
						
						if(suc) {
							lw.setBysn(srvs[0]);
							lw.setByns(srvs[1]);
							lw.setByver(srvs[2]);
							lw.setByins(srvs[3]);
							lw.setByme(srvs[6]);
							
							switch(tt) {
							case StatisConfig.BY_TYPE_SERVICE_METHOD:
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfig.BY_TYPE_SERVICE_INSTANCE:
								if(srvs.length < 4 || Utils.isEmpty(srvs[3])) {
									msg = "By instance name cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfig.BY_TYPE_SERVICE_INSTANCE_METHOD:
								if(srvs.length < 4 || Utils.isEmpty(srvs[3])) {
									msg = "By instance name cannot be NULL for id: " + lw.getId();
									return msg;
								}
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfig.BY_TYPE_SERVICE_ACCOUNT_METHOD:
								if(srvs.length < 6 || Utils.isEmpty(srvs[6])) {
									msg = "By service method cannot be NULL for id: " + lw.getId();
									return msg;
								}
								
								if(Utils.isEmpty(lw.getActName())) {
									msg = "By account name cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							case StatisConfig.BY_TYPE_SERVICE_ACCOUNT:
								if(Utils.isEmpty(lw.getActName())) {
									msg = "By account name cannot be NULL for id: " + lw.getId();
									return msg;
								}
								break;
							}
						}
						break;
					}
					case StatisConfig.BY_TYPE_INSTANCE:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "By instance name cannot be NULL for id: " + lw.getId();
							return msg;
						}
						break;
					case StatisConfig.BY_TYPE_ACCOUNT:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "By account name cannot be NULL for id: " + lw.getId();
							return msg;
						}
						break;
					/*case StatisConfig.BY_TYPE_EXP:
						if(Utils.isEmpty(lw.getByKey())) {
							msg = "Expression cannot be NULL for id: " + lw.getId();
							return msg;
						}
						
						if(!ExpUtils.isValid(lw.getByKey())) {
							msg = "Expression is invalid for id: " + lw.getId();
							return msg;
						}
						break;*/
				}
			}
		}finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, MonitorStatisConfigManager.class, msg);
			}
		}
		
		return msg;
	}

	private boolean checkByService(int cfgId, String[] srvs) {
		String msg = null;
		try {
			if(Utils.isEmpty(srvs[0])) {
				msg = "By service name cannot be NULL for id: " + cfgId;
				return false;
			}
			
			if(Utils.isEmpty(srvs[1])) {
				msg = "By namespace cannot be NULL for id: " + cfgId;
				return false;
			}
			
			if(Utils.isEmpty(srvs[2])) {
				msg = "By version cannot be NULL for id: " + cfgId;
				return false;
			}
			
			return true;
		}finally {
			if(msg != null) {
				logger.error(msg);
				LG.logWithNonRpcContext(MC.LOG_WARN, MonitorStatisConfigManager.class, msg);
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
	
	private Set<IStatisConfigListener> scListener = null;
	
}
