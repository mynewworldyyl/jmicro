package cn.jmicro.resource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.PromiseUtils;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.email.genclient.IEmailSender$JMAsyncClient;
import cn.jmicro.api.exp.Exp;
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
import cn.jmicro.api.service.IServiceAsyncResponse;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class ResourceMonitorServer{

	private static final Logger logger = LoggerFactory.getLogger(ResourceMonitorServer.class);
	
	private String logDir;
	
	public static void main(String[] args) {
		JMicro.getObjectFactoryAndStart(args);
		JMicro.waitForShutdown();
	}
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Reference
	private IEmailSender$JMAsyncClient mailSender;
	
	@Reference(namespace="monitorResourceService", version="*", type="ins",required=false,changeListener="resourceServiceChange")
	private Set<IResourceService$JMAsyncClient> resourceServices = Collections.synchronizedSet(new HashSet<>());
	
	private Set<IResourceService$JMAsyncClient> adds = new HashSet<>();
	
	private Set<IResourceService$JMAsyncClient> dels = new HashSet<>();
	
	//private Map<String,Set<CfgMetadata>> resourceMetadatas = new ConcurrentHashMap<>();
	
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
	private IObjectFactory of;
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private PubSubManager pubsubMng;
	
	public void ready() {
		
		logDir = System.getProperty("user.dir")+"/logs/resLog/";
		File d = new File(logDir);
		if(!d.exists()) {
			d.mkdirs();
		}
		
		configListener = new RaftNodeDataListener<>(op,ResourceMonitorConfig.RES_MONITOR_CONFIG_ROOT,
				ResourceMonitorConfig.class,false);
		
		configListener.addListener((type,node,pi)->{
			notifyConfigEvent(type,Integer.parseInt(node),pi);
		});
		
		if(!resourceServices.isEmpty()) {
			this.adds.addAll(resourceServices);
		}
		
		/*insMng.addInstanceListner((type,pi)->{
			if(type == IListener.REMOVE) {
				return;
			}
			parseProcessMetadata(pi);
		});*/
		
		TimerTicker.doInBaseTicker(3, ResourceMonitorServer.class.getName(), null, (key,att)->{
			checkResourceServiceAdd();
			checkResourceServiceDelete();
			doWorker();
		});
	}

	/*
	private void parseProcessMetadata(ProcessInfo pi) {
		Set<CfgMetadata> cms = pi.getMetadatas();
		if(cms != null && !cms.isEmpty()) {
			for(CfgMetadata cm : cms) {
				if(!resourceMetadatas.containsKey(cm.getResName())) {
					resourceMetadatas.put(cm.getResName(), new HashSet<>());
				}
				resourceMetadatas.get(cm.getResName()).add(cm);
			}
		}
	}
	*/

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
			
			boolean con = false;
			long curTime = TimeUtils.getCurTime();
			for(ResourceMonitorConfig cfg : r.configs.values()) {
				if(curTime - cfg.getLastNotifyTime() > cfg.getT()) {
					con = true;
					break;
				}
			}
			
			if(!con) {
				continue;
			}
			
			Set<String> resNames = new HashSet<>();
			Map<String,String> expMap = new HashMap<>();
			
			for(ResourceMonitorConfig cfg : r.configs.values()) {
				resNames.add(cfg.getResName());
				if(StringUtils.isNotEmpty(cfg.getExpStr())) {
					expMap.put(cfg.getResName(), cfg.getExpStr());
				}
			}
			
			r.running = true;
			try {
				r.lastCheckTime = TimeUtils.getCurTime();
				r.resSrv.getResourceJMAsync(resNames,new HashMap<>(),expMap,r)
				.success((result,reg)->{
					Reg rr = (Reg)reg;
					notifyResourceMonitorResult(rr,(Set<ResourceData>)result);
					r.lastCheckTime = TimeUtils.getCurTime();
					rr.running = false;
				}).fail((code,msg,reg)->{
					Reg rr = (Reg)reg;
					logger.error("code: " + code + ", msg: " + msg +",srv: "+ r.resSrv.getItem().getKey().toKey(true, true, true));
					rr.running = false;
					rr.lastCheckTime = TimeUtils.getCurTime();
				});
			}catch(Throwable e) {
				logger.error("",e);
				//r.running = false;
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
			//每一个Config只对一种资源感兴趣
			ResourceMonitorConfig cfg = rr.configs.get(cid);
			if(cfg == null) continue;
			
			ResourceData rd = rdsMap.get(cfg.getResName());
			if(rd == null) continue;
			
			if(TimeUtils.getCurTime() - cfg.getLastNotifyTime() < cfg.getT()) {
				//没达到一个通知周期
				continue;
			}
			
			if(cfg.getExp() != null) {
				rd = filterByExp(cfg.getExp(),rd);
			}
			
			if(rd == null) {
				continue;
			}
			
			rd.setCid(cfg.getId());
			
			cfg.setLastNotifyTime(TimeUtils.getCurTime());
			switch(cfg.getToType()) {
			case StatisConfig.TO_TYPE_SERVICE_METHOD:
				if(cfg.getToSrv() == null) {
					this.createToRemoteService(cfg);
					if(cfg.getToSrv() == null) {
						logger.error("Notify result fail for service proxy is null!");
						return;
					}
				}
				PromiseUtils.callService(cfg.getToSrv(), cfg.getToMt(), null, rd)
				.fail((code,msg,cxt)->{
					logger.error("Notify fail: " + cfg.getToSn() +"##"+cfg.getToNs() +"##" + cfg.getToVer()+"##"+ cfg.getToMt());
				});
				break;
			case StatisConfig.TO_TYPE_CONSOLE:
				logger.info(JsonUtils.getIns().toJson(rd));
				break;
			case StatisConfig.TO_TYPE_MESSAGE:
				PSData pd = new PSData();
				pd.setData(rd);
				pd.setTopic(cfg.getToParams());
				pd.setPersist(false);
				pd.setId(idGenerator.getIntId(PSData.class));
				this.pubsubMng.publish(pd);
				break;
			case StatisConfig.TO_TYPE_DB:
				rd.setTag(cfg.getExtParams());
				MongoCollection<Document> coll = mongoDb.getCollection(cfg.getToParams());
				coll.insertOne(Document.parse(JsonUtils.getIns().toJson(rd)));
				break;
			case StatisConfig.TO_TYPE_MONITOR_LOG:
				rd.setTag(cfg.getToParams());
				LG.log(MC.LOG_INFO, cfg.getToParams(), JsonUtils.getIns().toJson(rd),null);
				break;
			case StatisConfig.TO_TYPE_EMAIL:
				if(mailSender != null) {
					mailSender.sendJMAsync(cfg.getToParams(), cfg.getExtParams(),  
							JsonUtils.getIns().toJson(rd), cfg)
					.fail((code,msg,cxt)->{
						logger.error("code:" + code+", msg: " + msg);
					});
				} else {
					logger.error("Email sender not found!");
				}
				break;
			case StatisConfig.TO_TYPE_FILE:
				try {
					cfg.getBw().write(JsonUtils.getIns().toJson(rd)+"\n");
				} catch (IOException e) {
					logger.error("",JsonUtils.getIns().toJson(rd));
				}
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private ResourceData filterByExp(Exp exp, ResourceData rd) {
		if(rd.getMetaData() == null || rd.getMetaData().isEmpty()) {
			return null;
		}
		
		if(rd.getMetaData().size() == 1) {
			Map.Entry<String, Object> vo = rd.getMetaData().entrySet().iterator().next();
			if(vo.getValue() instanceof Collection) {
				List<Map<String,Object>> rst = new ArrayList<>();
				
				Collection<Map<String,Object>> col = (Collection<Map<String,Object>>)vo.getValue();
				for(Map<String,Object> omap: col) {
					if(ExpUtils.compute(exp, omap, Boolean.class)) {
						rst.add(omap);
					}
				}
				
				if(!rst.isEmpty()) {
					ResourceData rd0 = new ResourceData();
					rd0.setBelongInsId(rd.getBelongInsId());
					rd0.setBelongInsName(rd0.getBelongInsName());
					rd0.setResName(rd.getResName());
					rd0.putData(vo.getKey(), rst);
					return rd0;
				}
				return null;
			}
		}
		return ExpUtils.compute(exp, rd.getMetaData(), Boolean.class) ? rd : null;
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
					if(cfg.isEnable()) {
						this.addConfig2Reg(cfg, r);
					}
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
	
	private void notifyConfigEvent(int type,int cid, ResourceMonitorConfig cfg) {
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
				if(cfg.isEnable()) {
					this.addConfig2Reg(cfg, r);
				}
			}
		}else if(type == IListener.REMOVE) {
			configRemove(cid);
		}
	}
	
	private void configAdd(ResourceMonitorConfig cfg) {
		Set<String> srvKeys = new HashSet<>();
		srvKeys.addAll(this.srv2Regs.keySet());
		for(String insName : srvKeys) {
			Reg r = this.srv2Regs.get(insName);
			if(r != null) {
				this.addConfig2Reg(cfg, r);
			}
		}
	}
	
	private void configRemove(Integer cid) {
		Set<String> srvKeys = new HashSet<>();
		srvKeys.addAll(this.srv2Regs.keySet());
		for(String insName : srvKeys) {
			Reg r = this.srv2Regs.get(insName);
			if(r != null && r.configs.containsKey(cid)) {
				ResourceMonitorConfig cfg = r.configs.remove(cid);
				if(cfg != null && cfg.getBw() != null) {
					try {
						cfg.getBw().close();
					} catch (IOException e) {
						logger.error("",e);
					}
				}
			}
		}
	}
	
	private void addConfig2Reg(ResourceMonitorConfig cfg, Reg r) {
		
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
		Set<CfgMetadata> metadata = pi.getMetadatas(resName);
		if(metadata != null) {
			for(CfgMetadata cm : metadata) {
				if(cm.getResName().equals(resName)) {
					f = true;
				}
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
			
			if(StatisConfig.TO_TYPE_FILE == cfg.getToType()) {
				File logFile = new File(this.logDir + cfg.getId() + "_" + cfg.getToParams());
				if (!logFile.exists()) {
					try {
						logFile.createNewFile();
					} catch (IOException e) {
						String msg ="Create log file fail";
						logger.error(msg, e);
						LG.logWithNonRpcContext(MC.LOG_ERROR, this.getClass(), msg, e);
					}
				}

				try {
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
					cfg.setBw(bw);
				} catch (FileNotFoundException e) {
					String msg ="Create writer fail";
					logger.error(msg, e);
					LG.logWithNonRpcContext(MC.LOG_ERROR, this.getClass(), msg, e);
				}
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
		
		if(!StringUtils.isEmpty(cfg.getExpStr())) {
			List<String> suffix = ExpUtils.toSuffix(cfg.getExpStr());
			if(ExpUtils.isValid(suffix)) {
				Exp ex = new Exp();
				ex.setOriEx(cfg.getExpStr());
				ex.setSuffix(suffix);
				cfg.setExp(ex);
			} else {
				logger.error("Invalid exp:" + cfg.getExpStr()+" for cfg: " + cfg.getId());
			}
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
	
	public Set<CfgMetadata> getResMetadata(String resName) {
		 Set<CfgMetadata> ms = new HashSet<>();
		this.insMng.forEach((pi)->{
			if(pi.getMetadatas(resName) != null) {
				ms.addAll(pi.getMetadatas(resName)) ;
			}
		});
		return ms;
	}
	
	public Map<String,Map<String,Set<CfgMetadata>>> getInstanceResourceList() {
		Map<String,Map<String,Set<CfgMetadata>>> ms = new HashMap<>();
		this.insMng.forEach((pi)->{
			Map<String,Set<CfgMetadata>> maps = new HashMap<String,Set<CfgMetadata>>();
			ms.put(pi.getInstanceName(), maps);
			maps.putAll(pi.getMetadatas());
		});
		return ms;
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

	public Resp<Map<String, Set<ResourceData>>> getInstanceResourceData(String insName) {
		
		Resp<Map<String,Set<ResourceData>>> r = new Resp<>();
		//Map<String,Set<ResourceData>> resDatas = new HashMap<>();
		
		Reg reg = srv2Regs.get(insName);
		if(reg == null) {
			r.setMsg("Instance [" + insName + "] offline!");
			r.setCode(Resp.CODE_FAIL);
			return r;
		}
		
		if(reg.configs == null || reg.configs.isEmpty()) {
			r.setMsg("Instance [" + insName + "] support on resources!");
			r.setCode(Resp.CODE_FAIL);
			return r;
		}
		
		IServiceAsyncResponse cb = JMicroContext.get().getParam(Constants.CONTEXT_SERVICE_RESPONSE, null);
		
		ProcessInfo pi = this.insMng.getProcessByName(insName);
		if(pi == null) {
			r.setMsg("Instance [" + insName + "] offline!");
			r.setCode(Resp.CODE_FAIL);
			return r;
		}
		
		Set<String> resNames = new HashSet<>();
		resNames.addAll(pi.getMetadatas().keySet());
		
		Map<String,String> expMap = new HashMap<>();
		
		try {
			reg.resSrv.getResourceJMAsync(resNames,new HashMap<>(),expMap,reg)
			.success((result,reg0)->{
				//Reg rr = (Reg)reg0;
				r.setData((Map<String,Set<ResourceData>>)result);
				if(cb != null) {
					cb.result(r);
				} else {
					r.notify();
				}
			}).fail((code,msg,reg0)->{
				Reg rr = (Reg)reg0;
				String desc = "code: " + code + ", msg: " + msg +",srv: "+ rr.resSrv.getItem().getKey().toKey(true, true, true);
				logger.error(desc);
				LG.log(MC.LOG_ERROR, this.getClass(), desc);
				if(cb != null) {
					r.setMsg("Instance [" + insName + "] offline!");
					r.setCode(Resp.CODE_FAIL);
					cb.result(r);
				} else {
					r.notify();
				}
			});
		}catch(Throwable e) {
			logger.error("",e);
		}
		
		if(cb == null) {
			try {
				r.wait(10000*30);
			} catch (InterruptedException e) {
				String msg = "Resource service timeout " + insName + "] "+reg.resSrv.getItem().getKey().toKey(true, true, true);
				r.setCode(Resp.CODE_FAIL);
				r.setMsg(msg);
				logger.error("",e);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
			}
			return r;
		}else {
			//多重RPC异步返回结果
			return null;
		}
	}

	public Resp<Map<String, Set<ResourceData>>> getDirectResourceData(String insName, String resName,
			Map<String, String> params) {
		
		Resp<Map<String,Set<ResourceData>>> r = new Resp<>();
		
		Set<String> resNames = new HashSet<>();
		if(StringUtils.isNotEmpty(resName)) {
			resNames.add(resName);
		}
		
		if(StringUtils.isNotEmpty(insName)) {
			final Reg reg = this.srv2Regs.get(insName);
			if(reg != null) {
				try {
					reg.resSrv.getResourceJMAsync(resNames,new HashMap<>(),null)
					.success((result,rrr)->{
						
					}).fail((code,msg,rrr)->{
						logger.error("code: " + code + ", msg: " + msg +",srv: "+ reg.resSrv.getItem().getKey().toKey(true, true, true));
					});
				}catch(Throwable e) {
					logger.error("",e);
				}
			}
		}
		
		
		return r;
	}
	
}
