package cn.jmicro.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicro;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.mng.JmicroInstanceManager;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ResourceData;
import cn.jmicro.api.monitor.ResourceMonitorConfig;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.monitor.genclient.IResourceService$JMAsyncClient;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class ResourceMonitorServer{

	private static final Logger logger = LoggerFactory.getLogger(ResourceMonitorServer.class);
	
	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
		JMicro.waitForShutdown();
	}
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Reference(namespace="monitorResourceService", version="*", type="ins",required=false,changeListener="resourceServiceChange")
	private Set<IResourceService$JMAsyncClient> resourceServices = Collections.synchronizedSet(new HashSet<>());
	
	private Set<IResourceService$JMAsyncClient> adds = new HashSet<>();
	
	private Set<IResourceService$JMAsyncClient> dels = new HashSet<>();
	
	//private Map<Integer,Set<String>> config2Ins = new ConcurrentHashMap<>();
	
	private Map<String,Reg> srv2Regs = new ConcurrentHashMap<>();
	
	private RaftNodeDataListener<ResourceMonitorConfig> configListener = null;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private JmicroInstanceManager insMng;
	
	@Inject
	private ResourceMonitorConfigManager configManager;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private PubSubManager pubsubMng;
	
	public void ready() {
		
		configListener = new RaftNodeDataListener<>(op,ResourceMonitorConfig.RES_MONITOR_CONFIG_ROOT,
				ResourceMonitorConfig.class,false);
		
		configListener.addListener((type,node,pi)->{
			notifyConfigEvent(type,pi);
		});
		
		if(!resourceServices.isEmpty()) {
			this.adds.addAll(resourceServices);
		}
		
		TimerTicker.doInBaseTicker(3, ResourceMonitorServer.class.getName(), null, (key,att)->{
			checkResourceServiceAdd();
			checkResourceServiceDelete();
			doWorker();
		});
	}

	@SuppressWarnings("unchecked")
	private void doWorker() {
		if(this.srv2Regs.isEmpty()) {
			return;
		}
		
		Set<String> srvins = new HashSet<>();
		srvins.addAll(this.srv2Regs.keySet());
		
		for(String insName: srvins) {
			Reg r = srv2Regs.get(insName);
			if(r == null) {
				srv2Regs.remove(insName);
				continue;
			};
			
			if(r.configs.isEmpty() || r.running && TimeUtils.getCurTime() - r.lastCheckTime < 30000) continue;
			
			r.running = true;
			try {
				r.resSrv.getResourceJMAsync(new HashMap<>(),r)
				.success((result,reg)->{
					Reg rr = (Reg)reg;
					notifyResourceMonitorResult(rr,(Set<ResourceData>)result);
					rr.lastCheckTime = TimeUtils.getCurTime();
					rr.running = false;
				}).fail((code,msg,reg)->{
					Reg rr = (Reg)reg;
					logger.error("code: " + code + ", msg: " + msg +",srv: "+ r.resSrv.getItem().getKey().toKey(true, true, true));
					rr.running = false;
					rr.lastCheckTime = TimeUtils.getCurTime();
				});
			}catch(Throwable e) {
				logger.error("",e);
				r.running = false;
				r.lastCheckTime = TimeUtils.getCurTime();
			}
		}
	}

	private void notifyResourceMonitorResult(Reg rr, Set<ResourceData> result) {
		
		Set<Integer> cids = new HashSet<>();
		cids.addAll(rr.configs.keySet());
		
		Map<String,ResourceData> rdsMap = new HashMap<>();
		for(ResourceData rd : result) {
			rdsMap.put(rd.getResName(), rd);
		}
		
		for(Integer cid : cids) {
			ResourceMonitorConfig cfg = rr.configs.get(cid);
			if(cfg == null) continue;
			ResourceData rd = rdsMap.get(cfg.getResName());
			if(rd == null) continue;
			
			if(cfg.getExp() != null && !ExpUtils.compute(cfg.getExp(), rd.getMetaData(), Boolean.class)) {
				continue;
			}
			
			switch(cfg.getToType()) {
			case StatisConfig.TO_TYPE_SERVICE_METHOD:
				if(cfg.getToSrv() == null) {
					this.createToRemoteService(cfg);
					if(cfg.getToSrv() == null) {
						logger.error("Notify result fail for service proxy is null!");
						return;
					}
				}
				PromiseUtils.callService(cfg.getToSrv(), cfg.getToMt(), null, result)
				.fail((code,msg,cxt)->{
					logger.error("Notify fail: " + cfg.getToSn() +"##"+cfg.getToNs() +"##" + cfg.getToVer()+"##"+ cfg.getToMt());
				});
				break;
			case StatisConfig.TO_TYPE_CONSOLE:
				logger.info(JsonUtils.getIns().toJson(result));
				break;
			case StatisConfig.TO_TYPE_MESSAGE:
				PSData pd = new PSData();
				pd.setData(result);
				pd.setTopic(cfg.getToParams());
				pd.setPersist(false);
				pd.setId(idGenerator.getIntId(PSData.class));
				this.pubsubMng.publish(pd);
				break;
			case StatisConfig.TO_TYPE_DB:
				MongoCollection<Document> coll = mongoDb.getCollection(cfg.getToParams());
				coll.insertOne(Document.parse(JsonUtils.getIns().toJson(result)));
				break;
			case StatisConfig.TO_TYPE_MONITOR_LOG:
				LG.log(MC.LOG_INFO, cfg.getToParams(), JsonUtils.getIns().toJson(result),null);
				break;
			case StatisConfig.TO_TYPE_EMAIL:
				LG.log(MC.LOG_INFO, cfg.getToParams(), JsonUtils.getIns().toJson(result),null);
				break;
			case StatisConfig.TO_TYPE_FILE:
				/*try {
					sc.getBw().write(JsonUtils.getIns().toJson(sd)+"\n");
				} catch (IOException e) {
					logger.error("",JsonUtils.getIns().toJson(sd));
				}*/
				break;
			}
			
			
		}
	}

	private void checkResourceServiceDelete() {
		if(dels.isEmpty()) {
			return;
		}
		
		synchronized(dels) {
			for(IResourceService$JMAsyncClient srv : dels) {
				ServiceItem si = srv.getItem();
				String insName = si.getKey().getInstanceName();
				if(this.srv2Regs.containsKey(insName)) {
					Reg r = this.srv2Regs.remove(insName);
					/*for(Integer cid : r.configs.keySet()) {
						Set<String> s = this.config2Ins.get(cid);
						if(s != null) {
							s.remove(insName);
						}
					}*/
				}
			}
			dels.clear();
		}
	}


	private void checkResourceServiceAdd() {
		if(adds.isEmpty()) {
			return;
		}
		synchronized(adds) {
			for(IResourceService$JMAsyncClient srv : adds) {
				String insName = srv.getItem().getKey().getInstanceName();
				if(this.srv2Regs.containsKey(insName)) {
					continue;
				}
				
				Reg r = new Reg(srv);
				
				this.configListener.forEachNode((cfg)->{
					this.addService2Reg(cfg, r);
				});
				
				this.srv2Regs.put(insName, r);
			}
			adds.clear();
		}
	}


	public void resourceServiceChange(AbstractClientServiceProxyHolder srv,int type) {
		IResourceService$JMAsyncClient djm = (IResourceService$JMAsyncClient)srv;
		if(type == IListener.ADD) {
			this.adds.add(djm);
		}else if(type == IListener.REMOVE) {
			this.dels.add(djm);
		}
	}
	
	private void notifyConfigEvent(int type, ResourceMonitorConfig cfg) {
		if(type == IListener.ADD) {
			if(cfg.isEnable()) {
				configAdd(cfg);
			}
		}else if(type == IListener.DATA_CHANGE) {
			Set<String> srvKeys = new HashSet<>();
			srvKeys.addAll(this.srv2Regs.keySet());
			for(String insName : srvKeys) {
				Reg r = this.srv2Regs.get(insName);
				if(r != null && r.configs.containsKey(cfg.getId())) {
					r.configs.remove(cfg.getId());
					if(!cfg.isEnable()) {
						continue;
					}
				}
				this.addService2Reg(cfg, r);
			}
		}else if(type == IListener.REMOVE) {
			configRemove(cfg);
		}
	}
	
	private void configAdd(ResourceMonitorConfig cfg) {
		Set<String> srvKeys = new HashSet<>();
		srvKeys.addAll(this.srv2Regs.keySet());
		for(String insName : srvKeys) {
			Reg r = this.srv2Regs.get(insName);
			if(r != null) {
				this.addService2Reg(cfg, r);
			}
		}
	}
	
	private void configRemove(ResourceMonitorConfig cfg) {
		Set<String> srvKeys = new HashSet<>();
		srvKeys.addAll(this.srv2Regs.keySet());
		for(String insName : srvKeys) {
			Reg r = this.srv2Regs.get(insName);
			if(r != null && r.configs.containsKey(cfg.getId())) {
				r.configs.remove(cfg.getId());
			}
		}
	}
	
	private void addService2Reg(ResourceMonitorConfig cfg, Reg r) {
		
		String macherInsName = cfg.getMonitorInsName();
		if(macherInsName != null && "*".equals(macherInsName)) {
			macherInsName = null;
		}else if(macherInsName != null && macherInsName.endsWith("*")) {
			macherInsName = macherInsName.substring(0,macherInsName.length()-1);
		}
		
		ServiceItem si = r.resSrv.getItem();
		String insName = si.getKey().getInstanceName();
		if(StringUtils.isNotEmpty(macherInsName)) {
			if(!insName.startsWith(macherInsName)) {
				return;
			}
		}
		
		String resName = cfg.getResName();
		ProcessInfo pi = this.insMng.getProcessByName(insName);
		
		boolean f = false;
		Set<CfgMetadata> metadata = pi.getMetadatas();
		for(CfgMetadata cm : metadata) {
			if(cm.getResName().equals(resName)) {
				f = true;
			}
		}
		
		if(f) {
			/*if(!config2Ins.containsKey(cfg.getId())) {
				config2Ins.put(cfg.getId(), new HashSet<>());
			}
			config2Ins.get(cfg.getId()).add(insName);*/
			if(cfg.getToSn() == null) {
				reparseConfig(cfg);
			}
			r.configs.put(cfg.getId(), cfg);
		}
	}

	private void reparseConfig(ResourceMonitorConfig cfg) {
		if(StatisConfig.TO_TYPE_SERVICE_METHOD == cfg.getToType()) {
			 String[] ps =  cfg.getToParams().split(UniqueServiceKey.SEP);
			 cfg.setToSn(ps[UniqueServiceKey.INDEX_SN]);
			 cfg.setToNs(ps[UniqueServiceKey.INDEX_NS]);
			 cfg.setToVer(ps[UniqueServiceKey.INDEX_VER]);
			 cfg.setToMt(ps[UniqueServiceKey.INDEX_METHOD]);
			 createToRemoteService(cfg);
		}
	}
	
	public boolean createToRemoteService(ResourceMonitorConfig cfg) {
		if(cfg.getToSrv() != null) {
			return true;
		}
		
		if(reg.isExists(cfg.getToSn(), cfg.getToNs(), cfg.getToVer())) {
			Object srv = null;
			try {
				 srv = of.getRemoteServie(cfg.getToSn(),cfg.getToNs(),cfg.getToVer(),null);
			}catch(CommonException e) {
				logger.error(e.getMessage());
				return false;
			}
			
			if(srv == null) {
				String msg2 = "Fail to create service proxy ["+cfg.getToSn() +"##"+cfg.getToNs()+"##"+ cfg.getToVer()+"] not found for id: " + cfg.getId();
				logger.warn(msg2);
				LG.logWithNonRpcContext(MC.LOG_WARN, this.getClass(), msg2);
				return false;
			}
			cfg.setToSrv(srv);
			return true;
		} else {
			//服务还不在在，可能后续上线，这里只发一个警告
			String msg2 = "Now config service ["+cfg.getToSn() +"##"+cfg.getToNs()+"##"+ cfg.getToVer()+"] not found for id: " + cfg.getId();
			logger.warn(msg2);
			return false;
		}
	}

	private class Reg{
		
		private Reg(IResourceService$JMAsyncClient resSrv) {
			this.resSrv = resSrv;
		}
		
		private IResourceService$JMAsyncClient resSrv;
		private Map<Integer,ResourceMonitorConfig> configs = new HashMap<>();
		
		private long lastCheckTime = 0;
		private boolean running = false;
		
	}
	
}
